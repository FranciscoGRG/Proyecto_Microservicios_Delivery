package com.plataforma_deliveri.order_service.errors;

import feign.Response;
import feign.codec.ErrorDecoder;

public class CustomErrorDecoder implements ErrorDecoder{
private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            // 💡 Lanza una excepción normal (RuntimeException) con un mensaje clave
            // para que el OrderService la pueda identificar y capturar.
            return new RuntimeException("PRODUCT_NOT_FOUND_IN_CATALOG"); 
        }

        // Si es cualquier otro error (500, etc.), usa el comportamiento por defecto de Feign
        return defaultDecoder.decode(methodKey, response);
    }
}
