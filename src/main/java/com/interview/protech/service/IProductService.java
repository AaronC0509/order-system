package com.interview.protech.service;

import com.interview.protech.entity.Product;

import java.util.List;

public interface IProductService {
    Product getProductByCode(String productCode);

    void updateProductInRedis(String productCode, int newStockQuantity);

    List<Product> findAll();
}
