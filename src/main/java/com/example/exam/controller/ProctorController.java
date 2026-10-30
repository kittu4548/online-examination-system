package com.example.exam.controller;

import com.example.exam.model.*;
import com.example.exam.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import com.example.exam.service.CourseAccessService;
import java.util.Map;

@Controller
public class ProctorController {
 private final ProctorEventRepository events; private final UserRepository users; private final ExamRepository exams;
 private final CourseAccessService courseAccessService;
 public ProctorController(ProctorEventRepository events, UserRepository users, ExamRepository exams,
                          CourseAccessService courseAccessService){
  this.events=events; this.users=users; this.exams=exams; this.courseAccessService=courseAccessService;
 }
 @PostMapping("/exam/proctor/event") @ResponseBody
 public ResponseEntity<?> record(@RequestBody Map<String,Object> body, Authentication auth,
                                 HttpSession session){
  if(auth==null || !auth.isAuthenticated()) return ResponseEntity.status(401).body(Map.of("error","Unauthorized"));
  try {
   Long examId=Long.valueOf(String.valueOf(body.get("examId")));
   String type=String.valueOf(body.getOrDefault("eventType","TAB_HIDDEN"));
   if(!type.matches("TAB_HIDDEN|WINDOW_BLUR|FULLSCREEN_EXIT|TAB_VISIBLE")) return ResponseEntity.badRequest().body(Map.of("error","Invalid event"));
   User student=users.findByUsername(auth.getName()).orElseThrow();
   Exam exam=exams.findById(examId).orElseThrow();
   if (!courseAccessService.canAccessExam(student, exam)
       || session.getAttribute("examStart:" + examId) == null) {
    return ResponseEntity.status(403).body(Map.of("error","Exam session is not active"));
   }
   ProctorEvent event=new ProctorEvent(); event.setStudent(student); event.setExam(exam); event.setEventType(type);
   event.setDetails("Browser visibility/focus event reported by exam page"); events.save(event);
   return ResponseEntity.ok(Map.of("recorded",true));
  } catch(Exception e){return ResponseEntity.badRequest().body(Map.of("error","Unable to record event"));}
 }
 @GetMapping("/admin/proctor")
 public String dashboard(Model model){model.addAttribute("events",events.findTop300ByOrderByOccurredAtDesc()); model.addAttribute("totalEvents",events.count()); model.addAttribute("tabSwitches",events.countByEventType("TAB_HIDDEN")); return "admin/proctor";}
}
