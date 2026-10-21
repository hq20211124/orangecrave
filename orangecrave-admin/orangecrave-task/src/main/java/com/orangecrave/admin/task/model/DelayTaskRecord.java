package com.orangecrave.admin.task.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
@AllArgsConstructor
public class DelayTaskRecord implements Delayed {

    private final String taskType;
    private final Long bizId;
    private final long fireTime;

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(fireTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(this.fireTime, ((DelayTaskRecord) other).fireTime);
    }
}
