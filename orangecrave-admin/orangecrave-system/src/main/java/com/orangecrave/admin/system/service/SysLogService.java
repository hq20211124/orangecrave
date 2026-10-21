package com.orangecrave.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.system.entity.SysLoginLog;
import com.orangecrave.admin.system.entity.SysOperLog;
import com.orangecrave.admin.system.mapper.SysLoginLogMapper;
import com.orangecrave.admin.system.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志 / 登录日志查询（见 02 篇 §2.2）。  */
@Service
@RequiredArgsConstructor
public class SysLogService {

    private final SysOperLogMapper operLogMapper;
    private final SysLoginLogMapper loginLogMapper;

    public PageResult<SysOperLog> operPage(PageQuery query) {
        Page<SysOperLog> page = operLogMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysOperLog>()
                .like(StringUtils.hasText(query.getKeyword()), SysOperLog::getOperDesc, query.getKeyword())
                .orderByDesc(SysOperLog::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public SysOperLog operDetail(Long id) {
        return operLogMapper.selectById(id);
    }

    /** 操作日志导出（见 02 篇 §2.2）。v1 返回结构化 JSON，由前端生成文件。 */
    public java.util.List<SysOperLog> operList() {
        return operLogMapper.selectList(new LambdaQueryWrapper<SysOperLog>()
                .orderByDesc(SysOperLog::getId));
    }

    public PageResult<SysLoginLog> loginPage(PageQuery query) {
        Page<SysLoginLog> page = loginLogMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysLoginLog>()
                .like(StringUtils.hasText(query.getKeyword()), SysLoginLog::getUserName, query.getKeyword())
                .orderByDesc(SysLoginLog::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
