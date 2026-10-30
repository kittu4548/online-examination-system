package com.example.exam.controller;

import com.example.exam.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class EmailController {

    @Autowired
    private EmailService emailService;

    // ============================================================
    // SEND EMAIL
    // ============================================================

    @PostMapping("/email-center/send")
    public String sendEmail(
            @RequestParam("recipient") String recipient,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message,
            RedirectAttributes redirectAttributes) {

        try {

            emailService.sendTestEmail(
                    recipient,
                    subject,
                    message
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Email sent successfully to " + recipient
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Failed to send email: " + e.getMessage()
            );
        }

        return "redirect:/admin/email-center";
    }
}