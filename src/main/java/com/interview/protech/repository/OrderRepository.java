package com.interview.protech.repository;

import com.interview.protech.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Order findByOrderNumber(String orderNumber);

    List<Order> findByStatusAndCreateTimeBefore(String status, LocalDateTime createTimeBefore);
}
