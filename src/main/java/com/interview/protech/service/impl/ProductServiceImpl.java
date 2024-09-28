package com.interview.protech.service.impl;

import com.interview.protech.entity.Product;
import com.interview.protech.repository.ProductRepository;
import com.interview.protech.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements IProductService {
    private final ProductRepository productRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository, RedisTemplate<String, Object> redisTemplate) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Product getProductByCode(String productCode) {
        String key = "product:" + productCode;
        Product product = (Product) redisTemplate.opsForValue().get(key);
        if (product == null) {
            product = productRepository.findByProductCode(productCode);
            if (product != null) {
                redisTemplate.opsForValue().set(key, product);
                redisTemplate.opsForValue().set("stock:" + productCode, product.getStockQuantity());
            }
        }
        return product;
    }

    @Override
    public void updateProductInRedis(String productCode, int newStockQuantity) {
        String productKey = "product:" + productCode;
        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.watch(productKey);
                Product product = (Product) operations.opsForValue().get(productKey);
                if (product != null) {
                    product.setStockQuantity(newStockQuantity);
                    operations.multi();
                    operations.opsForValue().set(productKey, product);
                    return operations.exec();
                }
                return null;
            }
        });
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }
}
