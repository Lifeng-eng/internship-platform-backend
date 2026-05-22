package com.example.internship.common;

import lombok.Data;
import java.util.List;

/**
 * 分页响应格式
 */
@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public PageResult(List<T> records, long total, int page, int size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = (int) Math.ceil((double) total / size);
    }
}
