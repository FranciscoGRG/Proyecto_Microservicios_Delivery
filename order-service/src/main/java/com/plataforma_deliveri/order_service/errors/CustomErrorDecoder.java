package com.plataforma_deliveri.order_service.errors;

import feign.Response;
import feign.codec.ErrorDecoder;

public class CustomErrorDecoder implements ErrorDecoder{
private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new RuntimeException("PRODUCT_NOT_FOUND_IN_CATALOG"); 
        }

        return defaultDecoder.decode(methodKey, response);
    }
}
