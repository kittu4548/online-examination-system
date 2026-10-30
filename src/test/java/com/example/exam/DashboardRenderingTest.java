package com.example.exam;

import com.example.exam.model.Exam;
import com.example.exam.model.ExamResult;
import com.example.exam.model.Subject;
import com.example.exam.model.User;
import com.example.exam.repository.CourseEnrollmentRepository;
import com.example.exam.repository.ExamRepository;
import com.example.exam.repository.ExamResultRepository;
import com.example.exam.repository.QuestionRepository;
import com.example.exam.repository.SubjectRepository;
import com.example.exam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private ExamRepository examRepository;
    @MockBean
    private ExamResultRepository examResultRepository;
    @MockBean
    private SubjectRepository subjectRepository;
    @MockBean
    private QuestionRepository questionRepository;
    @MockBean
    private CourseEnrollmentRepository courseEnrollmentRepository;

    private User student;
    private Exam availableExam;
    private Subject availableSubject;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(7L);
        student.setUsername("student");
        student.setEmail("student@example.com");
        student.setRole("STUDENT");

        when(userRepository.findByUsername("student"))
                .thenReturn(Optional.of(student));
        availableSubject = new Subject();
        availableSubject.setId(11L);
        availableSubject.setName("Java");
        availableSubject.setAccessType("FREE");
        availableSubject.setPublished(true);

        availableExam = new Exam();
        availableExam.setId(2L);
        availableExam.setTitle("Java Basics");
        availableExam.setDurationInMinutes(30);
        availableExam.setSubject(availableSubject);

        when(examRepository.findAll()).thenReturn(List.of(availableExam));
        when(subjectRepository.findByPublishedTrueOrderByNameAsc())
                .thenReturn(List.of(availableSubject));
        when(examResultRepository.findByStudentOrderBySubmissionTimeDesc(
                student)).thenReturn(List.of(result()));
        when(examResultRepository.findByStudent(student))
                .thenReturn(List.of(result()));
        when(questionRepository.findAll()).thenReturn(List.of());
        when(subjectRepository.findAll()).thenReturn(List.of());
        when(examResultRepository.findAll()).thenReturn(List.of());
        when(courseEnrollmentRepository.findAllByOrderByPurchasedAtDesc())
                .thenReturn(List.of());
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void rendersStudentDashboardWithExistingResult() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/dashboard"))
                .andExpect(content().string(containsString("Student Dashboard")))
                .andExpect(content().string(containsString("8 / 10")))
                .andExpect(content().string(containsString("80.0%")))
                .andExpect(content().string(containsString("30 minutes")))
                .andExpect(content().string(containsString("/student/subject/11")))
                .andExpect(content().string(containsString("/exam/2")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void rendersAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(content().string(containsString("Admin Dashboard")));
    }

    private ExamResult result() {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setTitle("Java Basics");

        ExamResult result = new ExamResult();
        result.setId(1L);
        result.setStudent(student);
        result.setExam(exam);
        result.setScoreAchieved(8);
        result.setTotalMarks(10);
        return result;
    }
}
