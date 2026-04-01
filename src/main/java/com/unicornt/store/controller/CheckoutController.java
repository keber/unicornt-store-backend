package com.unicornt.store.controller;

import com.unicornt.store.model.Address;
import com.unicornt.store.model.CartItem;
import com.unicornt.store.model.User;
import com.unicornt.store.repository.AddressRepository;
import com.unicornt.store.repository.UserRepository;
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
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public CheckoutController(CartService cartService,
                              AddressRepository addressRepository,
                              UserRepository userRepository) {
        this.cartService = cartService;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String checkout(Principal principal, Model model) {
        String email = principal.getName();
        Long userId = getUserId(email);

        List<CartItem> items = cartService.getCartItems(email);
        if (items.isEmpty()) {
            return "redirect:/cart";
        }

        int total = items.stream().mapToInt(CartItem::getSubtotal).sum();
        NumberFormat nf = NumberFormat.getInstance(Locale.of("es", "CL"));

        List<Address> addresses = addressRepository.findByUserId(userId);

        model.addAttribute("cartItems", items);
        model.addAttribute("cartTotal", total);
        model.addAttribute("formattedTotal", "$" + nf.format(total));
        model.addAttribute("addresses", addresses);
        model.addAttribute("newAddress", new Address());

        return "checkout/checkout";
    }

    @PostMapping("/add-address")
    public String addAddress(@RequestParam String street,
                             @RequestParam String city,
                             @RequestParam String region,
                             @RequestParam(required = false) String zipCode,
                             Principal principal) {
        Long userId = getUserId(principal.getName());

        List<Address> existing = addressRepository.findByUserId(userId);

        Address address = new Address();
        address.setUserId(userId);
        address.setStreet(street);
        address.setCity(city);
        address.setRegion(region);
        address.setZipCode(zipCode);
        address.setDefault(existing.isEmpty());
        addressRepository.save(address);

        return "redirect:/checkout";
    }

    @PostMapping("/remove-address")
    public String removeAddress(@RequestParam Long addressId, Principal principal) {
        Long userId = getUserId(principal.getName());
        addressRepository.findById(addressId)
                .filter(a -> a.getUserId().equals(userId))
                .ifPresent(addressRepository::delete);
        return "redirect:/checkout";
    }

    @PostMapping("/confirm")
    public String confirm(@RequestParam Long addressId,
                          Principal principal,
                          RedirectAttributes flash) {
        String email = principal.getName();
        Long userId = getUserId(email);

        List<CartItem> items = cartService.getCartItems(email);
        if (items.isEmpty()) {
            return "redirect:/cart";
        }

        Address address = addressRepository.findById(addressId)
                .filter(a -> a.getUserId().equals(userId))
                .orElse(null);
        if (address == null) {
            flash.addFlashAttribute("checkoutError", "Selecciona una dirección de envío válida.");
            return "redirect:/checkout";
        }

        int total = items.stream().mapToInt(CartItem::getSubtotal).sum();
        NumberFormat nf = NumberFormat.getInstance(Locale.of("es", "CL"));

        cartService.clearCart(email);

        flash.addFlashAttribute("orderAddress", address.getFullAddress());
        flash.addFlashAttribute("orderTotal", "$" + nf.format(total));
        flash.addFlashAttribute("orderItemCount", items.size());

        return "redirect:/checkout/success";
    }

    @GetMapping("/success")
    public String success(Model model) {
        if (!model.containsAttribute("orderTotal")) {
            return "redirect:/catalog";
        }
        return "checkout/success";
    }

    private Long getUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
        return user.getId();
    }
}
