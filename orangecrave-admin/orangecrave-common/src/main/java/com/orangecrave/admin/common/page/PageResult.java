package com.orangecrave.admin.common.page;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应体（见 02 篇 §1.3）。pageNum 从 1 开始，pageSize 默认 20、上限 200。  */
@Data
public class PageResult<T> implements Serializable {

    private long total;
    private long pageNum;
    private long pageSize;
    private List<T> list;

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> list) {
        PageResult<T> p = new PageResult<>();
        p.total = total;
        p.pageNum = pageNum;
        p.pageSize = pageSize;
        p.list = list;
        return p;
    }
}
