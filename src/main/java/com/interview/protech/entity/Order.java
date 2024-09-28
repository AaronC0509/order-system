package com.interview.protech.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNumber;

    private Long userId;

    private String productCode;

    private Integer quantity;

    private BigDecimal amount;

    private BigDecimal totalAmount;

    private String status; // CREATED, PAID, CLOSED

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
