
package com.example.demo.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Customer;
import com.example.demo.model.CustomerOrder;
import com.example.demo.model.Sweet;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.SweetRepository;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:5173"
})
public class OrderController {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final SweetRepository sweetRepository;

    private static final Set<String> VALID_STATUSES = Set.of(
        "Pending",
        "Confirmed",
        "Completed",
        "Cancelled"
    );

    public OrderController(
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            SweetRepository sweetRepository) {

        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.sweetRepository = sweetRepository;
    }

    // 1. PLACE NEW ORDER
    @PostMapping
    @Transactional
    public ResponseEntity<?> placeOrder(
            @RequestBody CustomerOrder order) {

        if (order == null) {
            return ResponseEntity.badRequest()
                    .body("Order details are required");
        }

        if (order.getQuantity() <= 0) {
            return ResponseEntity.badRequest()
                    .body("Quantity must be greater than zero");
        }

        Customer customer = customerRepository
                .findById(order.getCustomerId())
                .orElse(null);

        if (customer == null) {
            return ResponseEntity.badRequest()
                    .body("Customer not found");
        }

        Sweet sweet = sweetRepository
                .findById(order.getSweetId())
                .orElse(null);

        if (sweet == null) {
            return ResponseEntity.badRequest()
                    .body("Sweet not found");
        }

        if (sweet.getQuantity() < order.getQuantity()) {
            return ResponseEntity.badRequest()
                    .body("Insufficient sweet stock");
        }

        // Calculate total price
        order.setId(0);
        order.setTotalprice(
                sweet.getPrice() * order.getQuantity()
        );

        // Default status
        order.setStatus("Pending");

        // Reduce stock
        sweet.setQuantity(
                sweet.getQuantity() - order.getQuantity()
        );

        sweetRepository.save(sweet);

        // Save order
        CustomerOrder savedOrder =
                orderRepository.save(order);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedOrder);
    }

    // 2. GET ALL ORDERS
    @GetMapping
    public ResponseEntity<List<CustomerOrder>> getAllOrders() {

        List<CustomerOrder> orders = orderRepository.findAll();

        return ResponseEntity.ok(orders);
    }

    // 3. GET ORDER BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(
            @PathVariable int id) {

        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body("Invalid order ID");
        }

        return orderRepository.findById(id)
                .map(order -> ResponseEntity.ok(order))
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    // 4. GET ORDERS BY CUSTOMER ID
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getCustomerOrders(
            @PathVariable int customerId) {

        if (customerId <= 0) {
            return ResponseEntity.badRequest()
                    .body("Invalid customer ID");
        }

        List<CustomerOrder> orders =
                orderRepository.findByCustomerId(customerId);

        return ResponseEntity.ok(orders);
    }

    // 5. UPDATE ORDER STATUS
    @PutMapping("/{id}/status")
    @Transactional
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable int id,
            @RequestParam String status) {

        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body("Invalid order ID");
        }

        if (status == null ||
                !VALID_STATUSES.contains(status)) {

            return ResponseEntity.badRequest()
                    .body("Invalid status. Allowed: Pending, Confirmed, Completed, Cancelled");
        }

        CustomerOrder order = orderRepository
                .findById(id)
                .orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        String currentStatus = order.getStatus();

        if ("Cancelled".equals(currentStatus)) {
            return ResponseEntity.badRequest()
                    .body("Cancelled order cannot be updated");
        }

        if ("Completed".equals(currentStatus)) {
            return ResponseEntity.badRequest()
                    .body("Completed order cannot be updated");
        }

        // Restore stock when order is cancelled
        if ("Cancelled".equals(status)) {

            Sweet sweet = sweetRepository
                    .findById(order.getSweetId())
                    .orElse(null);

            if (sweet != null) {

                sweet.setQuantity(
                        sweet.getQuantity() + order.getQuantity()
                );

                sweetRepository.save(sweet);
            }
        }

        order.setStatus(status);

        CustomerOrder updatedOrder =
                orderRepository.save(order);

        return ResponseEntity.ok(updatedOrder);
    }

    // 6. CANCEL ORDER
    @PutMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelOrder(
            @PathVariable int id) {

        CustomerOrder order = orderRepository
                .findById(id)
                .orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if ("Cancelled".equals(order.getStatus())) {
            return ResponseEntity.badRequest()
                    .body("Order is already cancelled");
        }

        if ("Completed".equals(order.getStatus())) {
            return ResponseEntity.badRequest()
                    .body("Completed order cannot be cancelled");
        }

        Sweet sweet = sweetRepository
                .findById(order.getSweetId())
                .orElse(null);

        if (sweet != null) {

            sweet.setQuantity(
                    sweet.getQuantity() + order.getQuantity()
            );

            sweetRepository.save(sweet);
        }

        order.setStatus("Cancelled");

        CustomerOrder updatedOrder =
                orderRepository.save(order);

        return ResponseEntity.ok(updatedOrder);
    }

    // 7. DELETE ORDER
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteOrder(
            @PathVariable int id) {

        CustomerOrder order = orderRepository
                .findById(id)
                .orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Restore stock only if order was not cancelled
        if (!"Cancelled".equals(order.getStatus())) {

            Sweet sweet = sweetRepository
                    .findById(order.getSweetId())
                    .orElse(null);

            if (sweet != null) {

                sweet.setQuantity(
                        sweet.getQuantity() + order.getQuantity()
                );

                sweetRepository.save(sweet);
            }
        }

        orderRepository.delete(order);

        return ResponseEntity.ok(
                "Order deleted successfully"
        );
    }
}
