package com.example.exam.config;

import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private UserRepository userRepository;
    /***
     Subscribe Lazycoder - https://www.youtube.com/c/LazyCoderOnline?sub_confirmation=1
     whatsapp - https://wa.me/919572181024
     email - wapka1503@gmail.com
     ***/

    @ModelAttribute("globalProfilePicUrl")
    public String getGlobalProfilePicUrl() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName(); // This is the user's email
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                return user.getProfilePicUrl();
            }
        }
        return null; // No user or no profile pic
    }
}