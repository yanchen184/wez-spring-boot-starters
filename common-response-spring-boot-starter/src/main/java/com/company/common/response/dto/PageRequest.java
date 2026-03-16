package com.company.common.response.dto;

/**
 * 通用分頁查詢參數
 *
 * <p>使用方式：Controller 直接作為方法參數，Spring 自動綁定 query string。
 * <pre>
 * &#64;GetMapping
 * public ResponseEntity&lt;ApiResponse&lt;PageResponse&lt;UserResponse&gt;&gt;&gt; search(
 *         &#64;RequestParam(required = false) String keyword,
 *         PageRequest pageRequest) { ... }
 *
 * // GET /api/users?keyword=王&amp;page=0&amp;size=20&amp;sortBy=cname&amp;sortDir=desc
 * </pre>
 *
 * <p>Service 層搭配 Spring Data 使用：
 * <pre>
 * int safePage = Math.max(pageRequest.getPage(), 0);
 * int safeSize = Math.min(Math.max(pageRequest.getSize(), 1), MAX_PAGE_SIZE);
 * Pageable pageable = PageRequest.of(safePage, safeSize, sort);
 * </pre>
 */
public class PageRequest {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private int page = DEFAULT_PAGE;
    private int size = DEFAULT_SIZE;
    private String sortBy;
    private String sortDir = "asc";

    public PageRequest() {
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

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }

    /**
     * 取得安全的頁碼（不小於 0）
     */
    public int safePage() {
        return Math.max(page, 0);
    }

    /**
     * 取得安全的每頁筆數（1 ~ MAX_SIZE）
     */
    public int safeSize() {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    /**
     * 判斷排序方向是否為降序
     */
    public boolean isDescending() {
        return "desc".equalsIgnoreCase(sortDir);
    }
}
