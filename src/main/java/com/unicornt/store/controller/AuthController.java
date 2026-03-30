package com.unicornt.store.controller;

import com.unicornt.store.dto.RegisterRequest;
import com.unicornt.store.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterRequest registerRequest, Model model) {
        // Validación básica de campos vacíos
        if (registerRequest.getFirstName() == null || registerRequest.getFirstName().isBlank()
                || registerRequest.getLastName() == null || registerRequest.getLastName().isBlank()
                || registerRequest.getEmail() == null || registerRequest.getEmail().isBlank()
                || registerRequest.getPassword() == null || registerRequest.getPassword().isBlank()) {
            model.addAttribute("error", "Todos los campos son obligatorios.");
            return "register";
        }

        if (userService.emailExists(registerRequest.getEmail())) {
            model.addAttribute("error", "Ya existe una cuenta con ese correo electrónico.");
            return "register";
        }

        userService.register(registerRequest);
        return "redirect:/login?registered";
    }
}
