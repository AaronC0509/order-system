package com.interview.protech.service.impl;

import com.interview.protech.entity.Order;
import com.interview.protech.entity.Product;
import com.interview.protech.repository.OrderRepository;
import com.interview.protech.service.IOrderService;
import com.interview.protech.service.IProductService;
import com.interview.protech.service.IStockSynchronizationServiceImpl;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl implements IOrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final IProductService productService;
    private final OrderRepository orderRepository;
    private final RocketMQTemplate rocketMQTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final IStockSynchronizationServiceImpl stockSynchronizationService;

    @Autowired
    public OrderServiceImpl(IProductService productService,
                            OrderRepository orderRepository,
                            StringRedisTemplate stringRedisTemplate,
                            RocketMQTemplate rocketMQTemplate,
                            RedissonClient redissonClient,
                            @Autowired(required = false) IStockSynchronizationServiceImpl stockSynchronizationService) {
        this.productService = productService;
        this.orderRepository = orderRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.rocketMQTemplate = rocketMQTemplate;
        this.redissonClient = redissonClient;
        this.stockSynchronizationService = stockSynchronizationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order placeOrder(Long userId, String productCode, Integer quantity) {
        RLock lock = redissonClient.getLock("order:lock:" + productCode);
        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                String errorMessage = "Unable to acquire lock for order placement: " + productCode;
                log.error(errorMessage);
                rocketMQTemplate.convertAndSend("order-failure-topic", errorMessage);
                throw new RuntimeException(errorMessage);
            }

            Product product = productService.getProductByCode(productCode);
            if (product == null) {
                String errorMessage = "Product not found: " + productCode;
                log.error(errorMessage);
                rocketMQTemplate.convertAndSend("order-failure-topic", errorMessage);
                throw new RuntimeException(errorMessage);
            }

            String stockKey = "stock:" + productCode;

            Long remainingStock = stringRedisTemplate.opsForValue().decrement(stockKey, quantity);
            if (remainingStock == null || remainingStock < 0) {
                stringRedisTemplate.opsForValue().increment(stockKey, quantity);
                String errorMessage = String.format("Insufficient stock for product: %s. Requested: %d, Available: %d", productCode, quantity, remainingStock + quantity);
                log.error(errorMessage);
                rocketMQTemplate.convertAndSend("order-failure-topic", errorMessage);
                throw new RuntimeException(errorMessage);
            }

            productService.updateProductInRedis(productCode, remainingStock.intValue());

            Order order = new Order();
            order.setOrderNumber(UUID.randomUUID().toString());
            order.setUserId(userId);
            order.setProductCode(productCode);
            order.setQuantity(quantity);
            order.setAmount(product.getPrice());
            order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            order.setStatus("CREATED");
            order.setCreateTime(LocalDateTime.now());
            orderRepository.save(order);

            log.info("Order created successfully: {}", order.getOrderNumber());
            rocketMQTemplate.convertAndSend("order-topic", "Order created successfully: " + order.getOrderNumber());

            sendOrderCancelMessage(order.getOrderNumber());

            return order;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMessage = "Order placement was interrupted for product: " + productCode;
            log.error(errorMessage, e);
            rocketMQTemplate.convertAndSend("order-failure-topic", errorMessage);
            throw new RuntimeException(e.getMessage(), e);
        } catch (Exception e) {
            String errorMessage = "Error placing order for product: " + productCode;
            log.error(errorMessage, e);
            rocketMQTemplate.convertAndSend("order-failure-topic", errorMessage + ": " + e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    @Override
    public void sendOrderCancelMessage(String orderNumber) {
        try {
            Message<String> message = MessageBuilder.withPayload(orderNumber).build();
            rocketMQTemplate.syncSend("order-cancel-topic", message, 1000, 16);
        } catch (Exception e) {
            System.err.println("Error sending cancel message for order: " + orderNumber);
            e.printStackTrace();
        }
    }

    @Override
    public Order getOrderDetails(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }
}
