package com.unicornt.store.controller;

import com.unicornt.store.dao.CategoryDAO;
import com.unicornt.store.model.CartItem;
import com.unicornt.store.service.CartService;
import com.unicornt.store.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/catalog")
public class CatalogController {

    private static final int PAGE_SIZE = 12;

    private final ProductService productService;
    private final CategoryDAO categoryDAO;
    private final CartService cartService;

    public CatalogController(ProductService productService, CategoryDAO categoryDAO,
                             CartService cartService) {
        this.productService = productService;
        this.categoryDAO = categoryDAO;
        this.cartService = cartService;
    }

    @GetMapping
    public String catalog(@RequestParam(required = false) String search,
                          @RequestParam(required = false) Integer categoryId,
                          @RequestParam(defaultValue = "1") int page,
                          Principal principal,
                          Model model) {

        Integer categoryFilter = (categoryId != null && categoryId > 0) ? categoryId : null;

        int totalItems = productService.countAll(search, categoryFilter);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));
        page = Math.max(1, Math.min(page, totalPages));
        int offset = (page - 1) * PAGE_SIZE;

        int windowStart = Math.max(1, page - 2);
        int windowEnd = Math.min(totalPages, page + 2);

        // Map productId -> quantity in cart
        Map<Integer, Integer> cartMap = new HashMap<>();
        if (principal != null) {
            for (CartItem item : cartService.getCartItems(principal.getName())) {
                cartMap.put(item.getProductId(), item.getQuantity());
            }
        }

        model.addAttribute("products", productService.findAll(search, categoryFilter, PAGE_SIZE, offset));
        model.addAttribute("categories", categoryDAO.findAll());
        model.addAttribute("searchParam", search != null ? search : "");
        model.addAttribute("categoryFilter", categoryFilter);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("windowStart", windowStart);
        model.addAttribute("windowEnd", windowEnd);
        model.addAttribute("cartMap", cartMap);

        return "catalog/product-list";
    }
}
