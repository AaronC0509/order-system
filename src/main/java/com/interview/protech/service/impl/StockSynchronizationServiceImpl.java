package com.interview.protech.service.impl;

import com.interview.protech.entity.Product;
import com.interview.protech.repository.ProductRepository;
import com.interview.protech.service.IProductService;
import com.interview.protech.service.IStockSynchronizationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class StockSynchronizationServiceImpl implements IStockSynchronizationServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final ProductRepository productRepository;

    private final IProductService productService;

    private final StringRedisTemplate stringRedisTemplate;

    private final Queue<String> retryQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    public StockSynchronizationServiceImpl(ProductRepository productRepository, IProductService productService, StringRedisTemplate stringRedisTemplate) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Scheduled(fixedRate = 300000)
    public void synchronizeStockWithDatabase() {
        log.info("Starting stock synchronization");
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            String stockKey = "stock:" + product.getProductCode();
            String redisStock = stringRedisTemplate.opsForValue().get(stockKey);

            if (redisStock == null || !redisStock.equals(product.getStockQuantity().toString())) {
                log.info("Updating stock for product {} in Redis", product.getProductCode());
                stringRedisTemplate.opsForValue().set(stockKey, product.getStockQuantity().toString());
                productService.updateProductInRedis(product.getProductCode(), product.getStockQuantity());
            }
        }
        log.info("Stock synchronization completed");
    }

    @Scheduled(fixedRate = 60000)
    public void retryFailedOperations() {
        log.info("Starting retry of failed operations");
        String productCode;
        while ((productCode = retryQueue.poll()) != null) {
            try {
                Product product = productRepository.findByProductCode(productCode);
                if (product != null) {
                    String stockKey = "stock:" + productCode;
                    stringRedisTemplate.opsForValue().set(stockKey, product.getStockQuantity().toString());
                    productService.updateProductInRedis(productCode, product.getStockQuantity());
                    log.info("Successfully retried update for product: {}", productCode);
                } else {
                    log.warn("Product not found during retry: {}", productCode);
                }
            } catch (Exception e) {
                log.error("Error during retry for product: {}", productCode, e);
                retryQueue.offer(productCode);
            }
        }
        log.info("Retry of failed operations completed");
    }

    @Override
    public void addToRetryQueue(String productCode) {
        retryQueue.offer(productCode);
    }
}
