package com.shopping.inventory_service.service;

import com.shopping.inventory_service.entity.Inventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LowStockNotificationService {

    private final RestTemplate restTemplate;

    @Value("${services.email-service}")
    private String emailServiceUrl;

    @Value("${admin.email}")
    private String adminEmail;

    /**
     * Sends a low-stock alert email to the admin.
     * Runs asynchronously so it never blocks the order flow.
     */
    @Async
    public void sendLowStockAlert(Inventory inventory, String productName) {
        try {
            String subject = "⚠️ Low Stock Alert — " + productName;
            String body    = buildAlertBody(inventory, productName);

            Map<String, Object> emailRequest = new HashMap<>();
            emailRequest.put("to",           adminEmail);
            emailRequest.put("subject",      subject);
            emailRequest.put("body",         body);
            emailRequest.put("type",         "LOW_STOCK_ALERT");
            emailRequest.put("customerName", "Admin");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(emailRequest, headers);

            restTemplate.postForEntity(
                    emailServiceUrl + "/api/email/send",
                    request,
                    String.class
            );

            log.info("Low-stock alert sent for product {} (qty: {})",
                    inventory.getProductId(), inventory.getQuantity());

        } catch (Exception e) {
            // Non-critical — log and continue, never fail the order
            log.warn("Failed to send low-stock alert for product {}: {}",
                    inventory.getProductId(), e.getMessage());
        }
    }

    private String buildAlertBody(Inventory inventory, String productName) {
        return String.format(
            "Dear Admin,\n\n" +
            "This is an automated low-stock alert.\n\n" +
            "Product:        %s (ID: %d)\n" +
            "Current Stock:  %d units\n" +
            "Alert Threshold: %d units\n\n" +
            "Please restock this product to avoid stockouts.\n\n" +
            "— Integrated Online Shopping System",
            productName,
            inventory.getProductId(),
            inventory.getQuantity(),
            inventory.getLowStockAlert()
        );
    }
}
