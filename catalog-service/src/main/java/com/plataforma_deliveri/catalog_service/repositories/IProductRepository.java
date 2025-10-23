package com.plataforma_deliveri.catalog_service.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.plataforma_deliveri.catalog_service.models.Product;

@Repository
public interface IProductRepository extends MongoRepository<Product, String> {

}
