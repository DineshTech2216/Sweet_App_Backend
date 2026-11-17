package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import com.example.demo.model.Sweet;
import com.example.demo.repository.SweetRepository;

@RestController
@RequestMapping("/sweets")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class SweetController {

    private final SweetRepository repository;

    public SweetController(SweetRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Sweet> addSweet(@RequestBody Sweet sweet) {
        sweet.setId(0);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(repository.save(sweet));
    }

    @GetMapping
    public List<Sweet> getAllSweets() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sweet> getSweet(@PathVariable int id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Sweet> searchSweet(@RequestParam String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }

    @GetMapping("/category/{category}")
    public List<Sweet> getByCategory(@PathVariable String category) {
        return repository.findByCategoryIgnoreCase(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sweet> updateSweet(
            @PathVariable int id,
            @RequestBody Sweet sweet) {

        return repository.findById(id).map(existing -> {
            existing.setName(sweet.getName());
            existing.setPrice(sweet.getPrice());
            existing.setQuantity(sweet.getQuantity());
            existing.setCategory(sweet.getCategory());
            existing.setImage(sweet.getImage());

            return ResponseEntity.ok(repository.save(existing));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSweet(@PathVariable int id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        repository.deleteById(id);
        return ResponseEntity.ok("Sweet deleted successfully");
    }
}