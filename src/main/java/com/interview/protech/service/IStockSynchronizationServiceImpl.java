package com.interview.protech.service;

public interface IStockSynchronizationServiceImpl {
    void addToRetryQueue(String productCode);
}
