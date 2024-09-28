package com.interview.protech.service.impl;

import com.interview.protech.entity.Order;
import com.interview.protech.repository.OrderRepository;
import com.interview.protech.repository.ProductRepository;
import com.interview.protech.service.IPaymentService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PaymentServiceImpl implements IPaymentService {

    private final OrderRepository orderRepository;

    private final RocketMQTemplate rocketMQTemplate;

    private final ProductRepository productRepository;

    @Autowired
    public PaymentServiceImpl(OrderRepository orderRepository, RocketMQTemplate rocketMQTemplate, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.rocketMQTemplate = rocketMQTemplate;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber);
        if (order == null || !"CREATED".equals(order.getStatus())) {
            throw new RuntimeException("订单不存在或已支付");
        }

        int updated = productRepository.decreaseStock(order.getProductCode(), order.getQuantity());
        if (updated == 0) {
            throw new RuntimeException("扣减库存失败");
        }

        order.setStatus("PAID");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.save(order);

        rocketMQTemplate.convertAndSend("order-topic", "订单支付成功：" + order.getOrderNumber());
    }
}
