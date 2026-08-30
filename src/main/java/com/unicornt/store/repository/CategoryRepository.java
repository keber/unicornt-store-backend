package com.unicornt.store.repository;

import com.unicornt.store.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    List<Category> findAllByOrderByNameAsc();

    Optional<Category> findBySlug(String slug);
}
