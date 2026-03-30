package com.unicornt.store.controller;

import com.unicornt.store.dao.CategoryDAO;
import com.unicornt.store.dao.ProductTypeDAO;
import com.unicornt.store.model.Product;
import com.unicornt.store.service.ProductService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador Spring MVC para la gestión de productos del admin.
 *
 * Rutas:
 *   GET  /admin/products         → listado + búsqueda/filtro
 *   GET  /admin/products/new     → formulario de creación
 *   POST /admin/products         → crear producto
 *   GET  /admin/products/edit    → formulario de edición (?id=X)
 *   POST /admin/products/update  → guardar edición
 *   POST /admin/products/delete  → eliminar
 */
@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private static final int PAGE_SIZE = 10;

    private final ProductService  productService;
    private final CategoryDAO     categoryDAO;
    private final ProductTypeDAO  productTypeDAO;

    public AdminProductController(ProductService productService,
                                  CategoryDAO categoryDAO,
                                  ProductTypeDAO productTypeDAO) {
        this.productService  = productService;
        this.categoryDAO     = categoryDAO;
        this.productTypeDAO  = productTypeDAO;
    }

    // ----------------------------------------------------------------
    // GET — listado
    // ----------------------------------------------------------------

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) Integer categoryId,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {

        Integer categoryFilter = (categoryId != null && categoryId > 0) ? categoryId : null;

        int totalItems = productService.countAll(search, categoryFilter);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));
        page = Math.max(1, Math.min(page, totalPages));
        int offset = (page - 1) * PAGE_SIZE;

        int windowStart = Math.max(1, page - 2);
        int windowEnd   = Math.min(totalPages, page + 2);

        model.addAttribute("products",       productService.findAll(search, categoryFilter, PAGE_SIZE, offset));
        model.addAttribute("categories",     categoryDAO.findAll());
        model.addAttribute("searchParam",    search != null ? search : "");
        model.addAttribute("categoryFilter", categoryFilter);
        model.addAttribute("currentPage",    page);
        model.addAttribute("totalPages",     totalPages);
        model.addAttribute("totalItems",     totalItems);
        model.addAttribute("windowStart",    windowStart);
        model.addAttribute("windowEnd",      windowEnd);

        return "admin/product-list";
    }

    // ----------------------------------------------------------------
    // GET — formulario nuevo
    // ----------------------------------------------------------------

    @GetMapping("/new")
    public String showNewForm(Model model) {
        model.addAttribute("product",      new Product());
        model.addAttribute("categories",   categoryDAO.findAll());
        model.addAttribute("productTypes", productTypeDAO.findAll());
        return "admin/product-form";
    }

    // ----------------------------------------------------------------
    // GET — formulario edición
    // ----------------------------------------------------------------

    @GetMapping("/edit")
    public String showEditForm(@RequestParam int id,
                               RedirectAttributes ra,
                               Model model) {
        if (id <= 0) {
            ra.addAttribute("error", "ID de producto no válido.");
            return "redirect:/admin/products";
        }
        Product product = productService.findById(id);
        if (product == null) {
            ra.addAttribute("error", "Producto no encontrado (id=" + id + ").");
            return "redirect:/admin/products";
        }
        model.addAttribute("product",      product);
        model.addAttribute("categories",   categoryDAO.findAll());
        model.addAttribute("productTypes", productTypeDAO.findAll());
        return "admin/product-form";
    }

    // ----------------------------------------------------------------
    // POST — crear
    // ----------------------------------------------------------------

    @PostMapping
    public String create(@ModelAttribute Product product,
                         @RequestParam(required = false) String isActive,
                         @RequestParam(required = false) String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String imageBase,
                         @RequestParam(required = false) String price,
                         @RequestParam(required = false) Integer categoryId,
                         @RequestParam(required = false) Integer productTypeId,
                         RedirectAttributes ra,
                         Model model) {

        Product input = extractProduct(name, description, imageBase, price,
                                       categoryId, productTypeId, isActive);
        List<String> errors = validate(input);

        if (!errors.isEmpty()) {
            model.addAttribute("product",      input);
            model.addAttribute("errors",       errors);
            model.addAttribute("categories",   categoryDAO.findAll());
            model.addAttribute("productTypes", productTypeDAO.findAll());
            return "admin/product-form";
        }

        try {
            productService.insert(input);
            ra.addAttribute("success", "Producto creado exitosamente.");
        } catch (RuntimeException e) {
            model.addAttribute("product",      input);
            model.addAttribute("errors",       List.of("Error al guardar el producto. Intenta nuevamente."));
            model.addAttribute("categories",   categoryDAO.findAll());
            model.addAttribute("productTypes", productTypeDAO.findAll());
            return "admin/product-form";
        }
        return "redirect:/admin/products";
    }

    // ----------------------------------------------------------------
    // POST — actualizar
    // ----------------------------------------------------------------

    @PostMapping("/update")
    public String update(@RequestParam int id,
                         @RequestParam(required = false) String isActive,
                         @RequestParam(required = false) String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String imageBase,
                         @RequestParam(required = false) String price,
                         @RequestParam(required = false) Integer categoryId,
                         @RequestParam(required = false) Integer productTypeId,
                         RedirectAttributes ra,
                         Model model) {

        if (id <= 0 || productService.findById(id) == null) {
            ra.addAttribute("error", "Producto no encontrado (id=" + id + ").");
            return "redirect:/admin/products";
        }

        Product input = extractProduct(name, description, imageBase, price,
                                       categoryId, productTypeId, isActive);
        input.setId(id);
        List<String> errors = validate(input);

        if (!errors.isEmpty()) {
            model.addAttribute("product",      input);
            model.addAttribute("errors",       errors);
            model.addAttribute("categories",   categoryDAO.findAll());
            model.addAttribute("productTypes", productTypeDAO.findAll());
            return "admin/product-form";
        }

        try {
            productService.update(input);
            ra.addAttribute("success", "Producto actualizado exitosamente.");
        } catch (RuntimeException e) {
            model.addAttribute("product",      input);
            model.addAttribute("errors",       List.of("Error al actualizar el producto. Intenta nuevamente."));
            model.addAttribute("categories",   categoryDAO.findAll());
            model.addAttribute("productTypes", productTypeDAO.findAll());
            return "admin/product-form";
        }
        return "redirect:/admin/products";
    }

    // ----------------------------------------------------------------
    // POST — eliminar
    // ----------------------------------------------------------------

    @PostMapping("/delete")
    public String delete(@RequestParam int id, RedirectAttributes ra) {
        if (id <= 0 || productService.findById(id) == null) {
            ra.addAttribute("error", "Producto no encontrado (id=" + id + ").");
            return "redirect:/admin/products";
        }
        try {
            productService.delete(id);
            ra.addAttribute("success", "Producto eliminado.");
        } catch (RuntimeException e) {
            ra.addAttribute("error", "Error al eliminar el producto.");
        }
        return "redirect:/admin/products";
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private Product extractProduct(String name, String description, String imageBase,
                                   String price, Integer categoryId, Integer productTypeId,
                                   String isActive) {
        Product p = new Product();
        p.setName(trim(name));
        p.setDescription(trim(description));
        p.setImageBase(trim(imageBase));
        p.setActive("1".equals(isActive));
        p.setCategoryId(categoryId != null ? categoryId : 0);
        p.setProductTypeId(productTypeId != null ? productTypeId : 0);

        String priceStr = trim(price);
        if (!priceStr.isEmpty()) {
            try {
                p.setPrice(Integer.parseInt(priceStr));
            } catch (NumberFormatException e) {
                p.setPrice(-1);
            }
        }
        return p;
    }

    private List<String> validate(Product p) {
        List<String> errors = new ArrayList<>();
        if (p.getName() == null || p.getName().isEmpty()) {
            errors.add("El nombre del producto es obligatorio.");
        } else if (p.getName().length() > 200) {
            errors.add("El nombre no puede superar los 200 caracteres.");
        }
        if (p.getCategoryId() <= 0) {
            errors.add("Debes seleccionar una categoría.");
        }
        if (p.getProductTypeId() <= 0) {
            errors.add("Debes seleccionar un tipo de producto.");
        }
        if (p.getPrice() <= 0) {
            errors.add("El precio debe ser un número entero mayor a 0.");
        }
        return errors;
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }
}
