package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.CustomerOrder;

public interface OrderRepository extends JpaRepository<CustomerOrder, Integer> {

    List<CustomerOrder> findByCustomerId(int customerId);

}