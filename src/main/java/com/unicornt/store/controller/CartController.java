package com.unicornt.store.controller;

import com.unicornt.store.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.text.NumberFormat;
import java.util.Locale;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String viewCart(Principal principal, Model model) {
        String email = principal.getName();
        var items = cartService.getCartItems(email);
        int total = items.stream().mapToInt(i -> i.getSubtotal()).sum();

        NumberFormat nf = NumberFormat.getInstance(Locale.of("es", "CL"));
        model.addAttribute("cartItems", items);
        model.addAttribute("cartTotal", total);
        model.addAttribute("formattedTotal", "$" + nf.format(total));
        return "cart/cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam int productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestParam(required = false) String redirectTo,
                            Principal principal,
                            RedirectAttributes flash) {
        cartService.addToCart(principal.getName(), productId, quantity);
        flash.addFlashAttribute("cartMessage", "Producto agregado al carrito");
        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }
        return "redirect:/catalog";
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam int productId,
                                 @RequestParam int quantity,
                                 Principal principal) {
        cartService.updateQuantity(principal.getName(), productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam int productId,
                                 Principal principal) {
        cartService.removeFromCart(principal.getName(), productId);
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(Principal principal) {
        cartService.clearCart(principal.getName());
        return "redirect:/cart";
    }
}
