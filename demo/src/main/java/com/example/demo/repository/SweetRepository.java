
package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Sweet;

public interface SweetRepository extends JpaRepository<Sweet, Integer> {

    List<Sweet> findByNameContainingIgnoreCase(String name);

    List<Sweet> findByCategoryIgnoreCase(String category);
}