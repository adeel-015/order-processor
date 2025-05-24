package com.adeelmp.order_service.service;

import brave.Span;
import brave.Tracer;
import com.adeelmp.order_service.dto.InventoryResponse;
import com.adeelmp.order_service.dto.OrderLineItemsDto;
import com.adeelmp.order_service.dto.OrderRequest;
import com.adeelmp.order_service.event.OrderPlacedEvent;
import com.adeelmp.order_service.model.Order;
import com.adeelmp.order_service.model.OrderLineItems;
import com.adeelmp.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        // Call Inventory Service, and place order if product is in
        // stock

//        Observation inventoryServiceObservation = Observation.createNotStarted("inventory-observation-lookup", this.observationRegistry);
//        inventoryServiceObservation.lowCardinalityKeyValue("call", "inventory-service");
//        return inventoryServiceObservation.observe(() -> {
//            InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
//                    .uri("http://inventory-service/api/inventory",
//                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
//                    .retrieve()
//                    .bodyToMono(InventoryResponse[].class)
//                    .block();

        Span inventoryServiceLookup = tracer.nextSpan().name("inventoryServiceLookup").start();

        InventoryResponse[] inventoryResponseArray = null; // Declare outside try
        try (Tracer.SpanInScope isLookup = tracer.withSpanInScope(inventoryServiceLookup.start())){
            // Call Inventory Service and place order if products are in
            // stock

            // ADD TRY-CATCH BLOCK AROUND WEBCLIENT CALL
            try {
                inventoryResponseArray = webClientBuilder.build().get()
                        .uri("http://inventory-service:8080/api/inventory",
                                uriBuilder -> {
                                    skuCodes.forEach(sku -> uriBuilder.queryParam("skuCode", sku));
                                    return uriBuilder.build();
                                })
                        .retrieve()
                        .bodyToMono(InventoryResponse[].class)
                        .block(); // Potential point of failure

            } catch (Exception e) {
                // Log the exception explicitly
                // You can log e.getMessage() or e.printStackTrace() for more details
                System.err.println("Error calling Inventory Service: " + e.getMessage());
                e.printStackTrace(); // Print stack trace for full details
                // Depending on how you want to handle this failure, you might re-throw or return an error string
                // For now, let's re-throw to see it propagate, or return a specific error indicating inventory call failed
                // throw new RuntimeException("Failed to call Inventory Service", e); // Example: Re-throw wrapped exception
                return "Inventory Service call failed: " + e.getMessage(); // Example: Return specific error message
            }

            // Check if inventoryResponseArray is still null if you returned above
            if (inventoryResponseArray == null) {
                // This part might not be reached if you return in the catch block
                // Handle the case where the catch block returned a message
                return "Order placement failed due to Inventory Service call.";
            }

            boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                    .allMatch(InventoryResponse::isInStock);

            if (allProductsInStock) {
                orderRepository.save(order);
                // publish Order Placed Event
                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
//                applicationEventPublisher.publishEvent(new OrderPlacedEvent(this, order.getOrderNumber()));
                return "Order Placed Successfully";
            } else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }
        } finally {
            inventoryServiceLookup.flush();
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
