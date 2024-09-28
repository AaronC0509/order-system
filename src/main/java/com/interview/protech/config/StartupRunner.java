package com.interview.protech.config;

import com.interview.protech.entity.Product;
import com.interview.protech.service.IProductService;
import com.interview.protech.service.impl.OrderServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartupRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final IProductService productService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public StartupRunner(IProductService productService, RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.productService = productService;
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void run(String... args) {
        List<Product> products = productService.findAll();

        for (Product product : products) {
            String stockKey = "stock:" + product.getProductCode();
            String productKey = "product:" + product.getProductCode();
            try {
                String existingStockStr = stringRedisTemplate.opsForValue().get(stockKey);
                Integer existingStock = null;
                if (existingStockStr != null) {
                    existingStockStr = existingStockStr.replaceAll("\"", "");
                    existingStock = Integer.parseInt(existingStockStr);
                }

                if (existingStock == null) {
                    stringRedisTemplate.opsForValue().set(stockKey, product.getStockQuantity().toString());
                }

                redisTemplate.opsForValue().set(productKey, product);

                String storedStockStr = stringRedisTemplate.opsForValue().get(stockKey);
                Integer storedStock = null;
                if (storedStockStr != null) {
                    storedStockStr = storedStockStr.replaceAll("\"", "");
                    storedStock = Integer.parseInt(storedStockStr);
                }
                Object storedProduct = redisTemplate.opsForValue().get(productKey);

                log.info("Raw stock data: " + stringRedisTemplate.opsForValue().get(stockKey));
                log.info("Raw product data: " + redisTemplate.opsForValue().get(productKey));
            } catch (Exception e) {
                log.error("Error handling data for product: " + product.getProductCode() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}