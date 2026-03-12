package com.company.showcase.repository;

import com.company.common.jpa.repository.SoftDeleteRepository;
import com.company.showcase.entity.Order;

/**
 * 訂單 Repository — SoftDeleteRepository（展示軟刪除功能）
 */
public interface OrderRepository extends SoftDeleteRepository<Order, Long> {
}
