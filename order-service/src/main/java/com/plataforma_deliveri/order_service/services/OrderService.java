package com.plataforma_deliveri.order_service.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.plataforma_deliveri.order_service.models.Order;
import com.plataforma_deliveri.order_service.repositories.IOrderRepository;

import clients.ICatalogServiceFeignClient;

@Service
public class OrderService {

    @Autowired
    private IOrderRepository repository;

    @Autowired
    private ICatalogServiceFeignClient catalogClient;

    public List<Order> findAll() {
        return repository.findAll();
    }

    
}
