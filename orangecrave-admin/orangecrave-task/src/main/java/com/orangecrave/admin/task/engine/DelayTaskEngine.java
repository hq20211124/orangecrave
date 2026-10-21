package com.orangecrave.admin.task.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orangecrave.admin.common.lock.DistributedLock;
import com.orangecrave.admin.task.entity.TaskDelayLog;
import com.orangecrave.admin.task.handler.DelayTaskHandler;
import com.orangecrave.admin.task.mapper.TaskDelayLogMapper;
import com.orangecrave.admin.task.model.DelayTaskRecord;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 延迟任务引擎（见 00 篇 §5.3 三层机制）：
 * ① DelayQueue 精准计时；② 启动重建：tsk_delay_log 重排队；③ 每 5 分钟兜底扫描。
 * 并发与幂等由业务侧条件 UPDATE 保证（见 00 篇 §5.4）。
 * <p>注：handler 通过 {@link ObjectProvider} 惰态解析——业务/handler 依赖 OrderService，
 * 老 OrderService 依赖本引擎，若构造期解析会造成循环依赖。 */
@Slf4j
@Component
public class DelayTaskEngine {

    private static final String SCAN_LOCK_KEY = "delay-task-scan";

    private final DelayQueue<DelayTaskRecord> queue = new DelayQueue<>();
    private final TaskDelayLogMapper logMapper;
    private final ObjectProvider<DelayTaskHandler> handlerProvider;
    private final DistributedLock distributedLock;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "delay-task-worker");
        t.setDaemon(true);
        return t;
    });

    private volatile Map<String, DelayTaskHandler> handlers;

    public DelayTaskEngine(TaskDelayLogMapper logMapper, ObjectProvider<DelayTaskHandler> handlerProvider,
                           DistributedLock distributedLock) {
        this.logMapper = logMapper;
        this.handlerProvider = handlerProvider;
        this.distributedLock = distributedLock;
    }

    private Map<String, DelayTaskHandler> handlers() {
        Map<String, DelayTaskHandler> local = handlers;
        if (local == null) {
            synchronized (this) {
                if (handlers == null) {
                    Map<String, DelayTaskHandler> map = new HashMap<>();
                    handlerProvider.forEach(h -> map.put(h.taskType(), h));
                    handlers = map;
                }
                local = handlers;
            }
        }
        return local;
    }

    @PostConstruct
    public void start() {
        worker.execute(this::loop);
        rebuild();
    }

    @PreDestroy
    public void stop() {
        worker.shutdownNow();
    }

    /** ① 提交任务：写 tsk_delay_log（幂等 upsert）并入队。*/
    public void schedule(String taskType, Long bizId, long delayMillis) {
        LocalDateTime fireTime = LocalDateTime.now().plusNanos(Math.max(delayMillis, 0) * 1_000_000L);
        TaskDelayLog existing = logMapper.selectOne(new LambdaQueryWrapper<TaskDelayLog>()
                .eq(TaskDelayLog::getTaskType, taskType)
                .eq(TaskDelayLog::getBizId, bizId));
        if (existing == null) {
            TaskDelayLog entity = new TaskDelayLog();
            entity.setTaskType(taskType);
            entity.setBizId(bizId);
            entity.setFireTime(fireTime);
            entity.setStatus(1);
            entity.setRetryCount(0);
            logMapper.insert(entity);
        } else {
            TaskDelayLog update = new TaskDelayLog();
            update.setId(existing.getId());
            update.setFireTime(fireTime);
            update.setStatus(1);
            logMapper.updateById(update);
        }
        queue.put(new DelayTaskRecord(taskType, bizId,
                fireTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
    }

    /** ② 启动重建：把 DB 里仍「待执行」的任务重新入队。*/
    public void rebuild() {
        List<TaskDelayLog> pending = logMapper.selectList(new LambdaQueryWrapper<TaskDelayLog>()
                .eq(TaskDelayLog::getStatus, 1));
        for (TaskDelayLog task : pending) {
            long fireMillis = task.getFireTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            queue.put(new DelayTaskRecord(task.getTaskType(), task.getBizId(), fireMillis));
        }
        log.info("延迟任务启动重建：{} 个待执行任务重新入队", pending.size());
    }

    /** ② 兜底扫描：每 5 分钟补静默丢失的任务。多实例下用锁保证同一时刻只有一个节点扫，避免重复执行。*/
    @Scheduled(fixedDelayString = "${orangecrave.task.scan-interval-ms:300000}", initialDelay = 60000)
    public void scan() {
        if (!distributedLock.tryLock(SCAN_LOCK_KEY, Duration.ofMinutes(5))) {
            return;
        }
        try {
            for (DelayTaskHandler handler : handlers().values()) {
                try {
                    for (Long bizId : handler.scanOverdue()) {
                        execute(handler.taskType(), bizId);
                    }
                } catch (Exception e) {
                    log.warn("兜底扫描失败 taskType={}: {}", handler.taskType(), e.getMessage());
                }
            }
        } finally {
            distributedLock.unlock(SCAN_LOCK_KEY);
        }
    }

    /** 执行某任务（供兜底扫描与手动重试调用）。**/
    public void execute(String taskType, Long bizId) {
        DelayTaskHandler handler = handlers().get(taskType);
        if (handler == null) {
            return;
        }
        TaskDelayLog record = logMapper.selectOne(new LambdaQueryWrapper<TaskDelayLog>()
                .eq(TaskDelayLog::getTaskType, taskType)
                .eq(TaskDelayLog::getBizId, bizId));
        try {
            handler.execute(bizId);
            mark(record, 2, null);
        } catch (Exception e) {
            log.warn("延迟任务执行失败 {}#{}: {}", taskType, bizId, e.getMessage());
            mark(record, 3, e.getMessage());
        }
    }

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                DelayTaskRecord record = queue.take();
                execute(record.getTaskType(), record.getBizId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("延迟任务循环异常: {}", e.getMessage());
            }
        }
    }

    private void mark(TaskDelayLog record, int status, String errorMsg) {
        if (record == null) {
            return;
        }
        TaskDelayLog update = new TaskDelayLog();
        update.setId(record.getId());
        update.setStatus(status);
        update.setExecTime(LocalDateTime.now());
        update.setErrorMsg(errorMsg);
        if (status == 3) {
            update.setRetryCount((record.getRetryCount() == null ? 0 : record.getRetryCount()) + 1);
        }
        logMapper.updateById(update);
    }
}
