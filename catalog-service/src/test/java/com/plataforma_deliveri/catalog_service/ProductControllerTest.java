package com.plataforma_deliveri.catalog_service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.plataforma_deliveri.catalog_service.controllers.ProductController;
import com.plataforma_deliveri.catalog_service.dtos.ProductRequestDto;
import com.plataforma_deliveri.catalog_service.dtos.ProductResponseDto;
import com.plataforma_deliveri.catalog_service.services.ProductService;

import com.plataforma_deliveri.catalog_service.errors.ProductNotFoundException;

import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private ProductService productService;

        private final String API_URL = "/api/v1/products";
        private final String PRODUCT_ID = "test-id-1";
        private final ProductRequestDto requestDto = new ProductRequestDto(
                        "Test Product", "Desc", 10.50, 5, "CatA");
        private final ProductResponseDto responseDto = new ProductResponseDto(
                        PRODUCT_ID, "Test Product", "Desc", 10.50, 5, "CatA", LocalDate.now());

        @TestConfiguration
        static class JacksonTestConfig {
                @Bean
                public ObjectMapper objectMapper() {
                        ObjectMapper mapper = new ObjectMapper();

                        mapper.registerModule(new JavaTimeModule());

                        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                        return mapper;
                }
        }

        @Test
        void createProduct_ShouldReturnCreatedProduct_AndStatus201() throws Exception {

                when(productService.createProduct(any(ProductRequestDto.class))).thenReturn(responseDto);

                mockMvc.perform(post(API_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                                .andExpect(status().isCreated()) // Espera status 201 CREATED
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").value(PRODUCT_ID))
                                .andExpect(jsonPath("$.name").value("Test Product"));

                verify(productService, times(1)).createProduct(any(ProductRequestDto.class));
        }

        @Test
        void findAll_ShouldReturnListOfProducts_AndStatus200() throws Exception {

                ProductResponseDto product2 = new ProductResponseDto(
                                "id-2", "Product 2", "D2", 20.00, 10, "CatB", LocalDate.now());
                List<ProductResponseDto> expectedList = Arrays.asList(responseDto, product2);

                when(productService.getAllProducts()).thenReturn(expectedList);

                mockMvc.perform(get(API_URL).contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].name").value("Test Product"));

                verify(productService, times(1)).getAllProducts();
        }

        @Test
        void findById_ShouldReturnProduct_AndStatus200_WhenFound() throws Exception {

                when(productService.findById(PRODUCT_ID)).thenReturn(responseDto);

                mockMvc.perform(get(API_URL + "/{id}", PRODUCT_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(PRODUCT_ID));

                verify(productService, times(1)).findById(PRODUCT_ID);
        }

        @Test
        void findById_ShouldReturnError_WhenNotFound() throws Exception {

                when(productService.findById(PRODUCT_ID))
                                .thenThrow(new ProductNotFoundException("Producto no encontrado"));

                mockMvc.perform(get(API_URL + "/{id}", PRODUCT_ID))
                                .andExpect(status().isNotFound());

                verify(productService, times(1)).findById(PRODUCT_ID);
        }

        @Test
        void updateProduct_ShouldReturnUpdatedProduct_AndStatus200() throws Exception {

                ProductResponseDto updatedResponse = new ProductResponseDto(
                                PRODUCT_ID, "New Name", "New Desc", 30.00, 20, "CatC", LocalDate.now());

                when(productService.updateProduct(eq(PRODUCT_ID), any(ProductRequestDto.class)))
                                .thenReturn(updatedResponse);

                mockMvc.perform(put(API_URL + "/{id}", PRODUCT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("New Name"));

                verify(productService, times(1)).updateProduct(eq(PRODUCT_ID), any(ProductRequestDto.class));
        }

        @Test
        void updateProduct_ShouldReturnError_WhenProductNotFound() throws Exception {

                when(productService.updateProduct(eq(PRODUCT_ID), any(ProductRequestDto.class)))
                                .thenThrow(new ProductNotFoundException(
                                                "Producto con id: " + PRODUCT_ID + " no encontrado"));

                mockMvc.perform(put(API_URL + "/{id}", PRODUCT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                                .andExpect(status().isNotFound());

                verify(productService, times(1)).updateProduct(eq(PRODUCT_ID), any(ProductRequestDto.class));
        }

        @Test
        void deleteProduct_ShouldReturnStatus204() throws Exception {

                doNothing().when(productService).deleteProduct(PRODUCT_ID);

                mockMvc.perform(delete(API_URL + "/{id}", PRODUCT_ID))
                                .andExpect(status().isNoContent());

                verify(productService, times(1)).deleteProduct(PRODUCT_ID);
        }

        @Test
        void deleteProduct_ShouldReturnError_WhenProductNotFound() throws Exception {

                doThrow(new ProductNotFoundException("Producto no encontrado")).when(productService)
                                .deleteProduct(PRODUCT_ID);

                mockMvc.perform(delete(API_URL + "/{id}", PRODUCT_ID))
                                .andExpect(status().isNotFound());

                verify(productService, times(1)).deleteProduct(PRODUCT_ID);
        }
}
