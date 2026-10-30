
package com.example.exam.controller;

import com.example.exam.repository.SubjectRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/subjects")
public class SubjectController {

    private final SubjectRepository subjectRepository;

    public SubjectController(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @GetMapping
    public String listSubjects(Model model) {
        model.addAttribute(
            "subjects",
            subjectRepository.findByPublishedTrueOrderByNameAsc()
        );

        return "subjects/list";
    }
}
