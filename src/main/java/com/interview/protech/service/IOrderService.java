package com.interview.protech.service;

import com.interview.protech.entity.Order;

public interface IOrderService {
    Order placeOrder(Long userId, String productCode, Integer quantity);

    void sendOrderCancelMessage(String orderNumber);

    Order getOrderDetails(String orderNumber);
}
