package com.plataforma_deliveri.order_service.repositories;

import org.springframework.stereotype.Repository;

import com.plataforma_deliveri.order_service.models.Order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface IOrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserEmail(String email);
}
