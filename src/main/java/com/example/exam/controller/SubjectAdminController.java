
package com.example.exam.controller;

import com.example.exam.model.Subject;
import com.example.exam.repository.SubjectRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Locale;

@Controller
@RequestMapping("/admin/subjects")
public class SubjectAdminController {

    private final SubjectRepository subjectRepository;

    public SubjectAdminController(
            SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @GetMapping
    public String listSubjects(Model model) {
        model.addAttribute(
                "subjects", subjectRepository.findAll());
        return "admin/subjects";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Subject subject = new Subject();
        model.addAttribute("subject", subject);
        return "admin/subject-form";
    }

    @PostMapping("/save")
    public String saveSubject(
            @ModelAttribute("subject") Subject form,
            RedirectAttributes redirectAttributes) {

        String name = form.getName() == null
                ? "" : form.getName().trim();

        String accessType = form.getAccessType() == null
                ? "" : form.getAccessType()
                        .trim().toUpperCase(Locale.ROOT);

        String duration = form.getSubscriptionDuration() == null
                ? "" : form.getSubscriptionDuration()
                        .trim().toUpperCase(Locale.ROOT);

        if (name.isBlank()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Subject name is required.");
            return "redirect:/admin/subjects/new";
        }

        if (form.getId() == null
                && subjectRepository.existsByNameIgnoreCase(name)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "This subject already exists.");
            return "redirect:/admin/subjects/new";
        }

        if (!accessType.equals("FREE")
                && !accessType.equals("PAID")) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Select Free or Paid access.");
            return "redirect:/admin/subjects/new";
        }

        BigDecimal price = form.getPrice() == null
                ? BigDecimal.ZERO : form.getPrice();

        if (price.compareTo(BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Price cannot be negative.");
            return "redirect:/admin/subjects/new";
        }

        if (accessType.equals("FREE")) {
            price = BigDecimal.ZERO;
            duration = "NONE";
        } else {
            if (!duration.equals("MONTHLY")
                    && !duration.equals("YEARLY")
                    && !duration.equals("LIFETIME")) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Select a valid subscription duration.");
                return "redirect:/admin/subjects/new";
            }

            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Paid subjects must have a price above zero.");
                return "redirect:/admin/subjects/new";
            }
        }

        Subject subject;

        if (form.getId() != null) {
            subject = subjectRepository.findById(form.getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Subject not found."));
        } else {
            subject = new Subject();
        }

        subject.setName(name);
        subject.setDescription(form.getDescription());
        subject.setImageUrl(form.getImageUrl());
        subject.setAccessType(accessType);
        subject.setPrice(price);
        subject.setSubscriptionDuration(duration);
        subject.setPublished(form.isPublished());

        subjectRepository.save(subject);

        redirectAttributes.addFlashAttribute(
                "successMessage", "Subject saved successfully.");

        return "redirect:/admin/subjects";
    }

    @GetMapping("/edit/{id}")
    public String editSubject(
            @PathVariable Long id, Model model) {

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Subject not found."));

        model.addAttribute("subject", subject);
        return "admin/subject-form";
    }
}
