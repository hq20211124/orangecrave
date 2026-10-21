package com.orangecrave.admin.common.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 分页查询基类（见 02 篇 §1.3）。pageNum 从 1 开始，pageSize 默认 20、上限 200。  */
@Data
public class PageQuery {

    private long pageNum = 1;
    private long pageSize = 20;
    private String keyword;
    private String orderBy;
    private String orderDirection = "desc";

    public <T> Page<T> toPage() {
        long num = Math.max(pageNum, 1);
        long size = Math.min(Math.max(pageSize, 1), 200);
        return new Page<>(num, size);
    }
}
