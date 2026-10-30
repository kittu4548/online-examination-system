
package com.example.exam.controller;

import com.example.exam.model.User;
import com.example.exam.service.FileStorageService;
import com.example.exam.service.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AuthController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    public AuthController(
            UserService userService,
            FileStorageService fileStorageService) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerStudent(
            @ModelAttribute("user") User user,
            BindingResult bindingResult,
            @RequestParam(
                value = "profilePicFile",
                required = false
            ) MultipartFile profilePicFile,
            Model model) {

        // The registration email is also the login username.
        String username = user.getUsername();

        if (username == null || username.isBlank()) {
            bindingResult.rejectValue(
                "username",
                "error.user",
                "Email is required."
            );
        } else {
            username = username.trim().toLowerCase();

            if (userService.findByUsername(username) != null) {
                bindingResult.rejectValue(
                    "username",
                    "error.user",
                    "An account already exists with this email."
                );
            }
        }

        if (user.getPassword() == null
                || user.getPassword().isBlank()) {
            bindingResult.rejectValue(
                "password",
                "error.user",
                "Password is required."
            );
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        user.setUsername(username);

        // The database requires an email value.
        user.setEmail(username);

        if (profilePicFile != null && !profilePicFile.isEmpty()) {
            try {
                String filePath =
                    fileStorageService.saveFile(profilePicFile);
                user.setProfilePicUrl(filePath);
            } catch (Exception e) {
                model.addAttribute(
                    "fileError",
                    "Could not save the profile picture. "
                    + "Please try another image."
                );
                return "register";
            }
        }

        try {
            userService.saveStudent(user);
            return "redirect:/register?success";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute(
                "registrationError",
                "Registration could not be saved. "
                + "This username or email may already exist."
            );
            return "register";
        }
    }
}
