package com.interview.protech.dto.orders;

import lombok.Data;

@Data
public class OrderRequest {
    private Long userId;
    private String productCode;
    private Integer quantity;
}