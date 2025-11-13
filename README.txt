-Proyecto de plataforma de entrega de comida por delivery BACKEND basado en microservicios con Docker ( como Glovo o similares ).
-Stack Tecnologico usado:
  -VSCode como Ide.
  -Java como lenguaje con el framework de Spring Boot.
  -PostgreSQL y MongoDB como bases de datos.
  -Stripe API como pasarela de pago.
  -Spring Security y JWT como sistema de gestion de usuarios.
  -Eureka y Feign para el descubrimiento y comunicacion entre los distintos microservicios.
  -kafka para el manejo de eventos asyncronos entre los servicios de payment-service y order-service.
  -Mockito para los test unitarios.
  -Docker para las bases de datos y Kafka.


-Como desplegarlo en Local?
  -Primero ejecutar docker-compose up -d en la raiz del proyecto para crear y levantar los contenedores.
  -Luego añadir las claves de prueba de Stripe en el archivo application.yml para que se puedan procesar los pagos en el payment-service (sino se añaden, el proyecto funcionara igual, pero kafka no funcionara debido a que recivira un evento null {} debido a que no se puede procesar el pago en Stripe).
  -Para levantar el backend hay que levantar los microservicios en el siguiente orden: eureka-server -> catalog-server -> order-server -> payment-server -> user-service -> api-gateway.
  -Por ultimo, hay que poner el cli de stripe en escucha con el comando:  stripe listen --forward-to localhost:8086/api/v1/payments/webhooks/events.

-Como probarlo?
  -Una vez configuradas las API_SECRET_KEY y levantado todo el backend, crear una cuenta de usuario e ir al apartado de Admin en la foto del perfil del navbar.
  -Una vez ahí, crear productos de prueba.
  -Una vez creados los productos, se pueden añadir al carrito y proceder a crear la orden de pedido.
  -Datos de prueba de la tarjeta para procesar el pago: Numero Tarjeta: 4242 4242 4242 4242; Fecha expiracion: 12/34; CVV: 123
