package com.company.common.response.dto;

import java.util.List;

/**
 * 分頁回應封裝
 *
 * 統一的分頁格式，所有分頁查詢都使用此格式
 *
 * @param <T> 資料類型
 * @author Platform Team
 * @version 1.0.0
 */
public class PageResponse<T> {

    /**
     * 資料列表
     */
    private List<T> content;

    /**
     * 當前頁碼（從 0 開始）
     */
    private int page;

    /**
     * 每頁大小
     */
    private int size;

    /**
     * 總筆數
     */
    private long totalElements;

    /**
     * 總頁數
     */
    private int totalPages;

    /**
     * 是否為第一頁
     */
    private boolean first;

    /**
     * 是否為最後一頁
     */
    private boolean last;

    /**
     * 是否有下一頁
     */
    private boolean hasNext;

    /**
     * 是否有上一頁
     */
    private boolean hasPrevious;

    public PageResponse() {
    }

    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        this.first = page == 0;
        this.last = page >= totalPages - 1;
        this.hasNext = page < totalPages - 1;
        this.hasPrevious = page > 0;
    }

    /**
     * 從 Spring Data Page 轉換
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        return new PageResponse<>(content, page, size, totalElements);
    }

    /**
     * 包裝成 ApiResponse
     */
    public ApiResponse<PageResponse<T>> toApiResponse() {
        return ApiResponse.ok(this);
    }

    // Getters and Setters

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
