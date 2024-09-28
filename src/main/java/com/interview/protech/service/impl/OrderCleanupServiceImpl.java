package com.interview.protech.service.impl;

import com.interview.protech.entity.Order;
import com.interview.protech.repository.OrderRepository;
import com.interview.protech.service.IProductService;
import com.interview.protech.service.IStockSynchronizationServiceImpl;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderCleanupServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;

    private final RocketMQTemplate rocketMQTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    private final IProductService productService;

    private final IStockSynchronizationServiceImpl stockSynchronizationService;

    @Autowired
    public OrderCleanupServiceImpl(OrderRepository orderRepository,
                                   RocketMQTemplate rocketMQTemplate,
                                   StringRedisTemplate stringRedisTemplate,
                                   IProductService productService,
                                   @Autowired(required = false) IStockSynchronizationServiceImpl stockSynchronizationService) {
        this.orderRepository = orderRepository;
        this.rocketMQTemplate = rocketMQTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.productService = productService;
        this.stockSynchronizationService = stockSynchronizationService;
    }

    @Transactional(rollbackFor = Exception.class)
    @Scheduled(fixedRate = 60000)
    public void closeUnpaidOrders() {
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        List<Order> unpaidOrders = orderRepository.findByStatusAndCreateTimeBefore("CREATED", twoMinutesAgo);
        for (Order order : unpaidOrders) {
            try {
                String stockKey = "stock:" + order.getProductCode();
                Boolean stockUpdated = stringRedisTemplate.execute(new SessionCallback<Boolean>() {
                    @Override
                    public Boolean execute(RedisOperations operations) throws DataAccessException {
                        operations.watch(stockKey);
                        String currentStock = (String) operations.opsForValue().get(stockKey);
                        if (currentStock != null) {
                            int updatedStock = Integer.parseInt(currentStock) + order.getQuantity();
                            operations.multi();
                            operations.opsForValue().set(stockKey, String.valueOf(updatedStock));
                            List<Object> results = operations.exec();
                            if (!results.isEmpty()) {
                                productService.updateProductInRedis(order.getProductCode(), updatedStock);
                                return true;
                            }
                        }
                        return false;
                    }
                });

                if (Boolean.TRUE.equals(stockUpdated)) {
                    order.setStatus("CLOSED");
                    orderRepository.save(order);
                    rocketMQTemplate.convertAndSend("order-topic", "Order auto-closed: " + order.getOrderNumber());
                    log.info("Closed unpaid order: {} and updated stock for product: {}",
                            order.getOrderNumber(), order.getProductCode());
                } else {
                    log.error("Failed to update stock for closed order: {}", order.getOrderNumber());
                    stockSynchronizationService.addToRetryQueue(order.getProductCode());
                }
            } catch (Exception e) {
                log.error("Error while closing unpaid order: {}", order.getOrderNumber(), e);
            }
        }
    }
}
