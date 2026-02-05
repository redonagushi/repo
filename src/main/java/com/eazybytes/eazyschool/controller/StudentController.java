package com.eazybytes.eazyschool.controller;

import com.eazybytes.eazyschool.model.Courses;
import com.eazybytes.eazyschool.model.CourseMaterial;
import com.eazybytes.eazyschool.model.Person;
import com.eazybytes.eazyschool.repository.CoursesRepository;
import com.eazybytes.eazyschool.repository.CourseMaterialRepository;
import com.eazybytes.eazyschool.service.CourseRequestService;
import com.eazybytes.eazyschool.service.CourseRatingService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("student")
public class StudentController {

    private final CoursesRepository coursesRepository;
    private final CourseMaterialRepository materialRepository;
    private final CourseRequestService courseRequestService;
    private final CourseRatingService courseRatingService;

    public StudentController(CoursesRepository coursesRepository,
                             CourseMaterialRepository materialRepository,
                             CourseRequestService courseRequestService,
                             CourseRatingService courseRatingService) {
        this.coursesRepository = coursesRepository;
        this.materialRepository = materialRepository;
        this.courseRequestService = courseRequestService;
        this.courseRatingService = courseRatingService;
    }

    @GetMapping("/displayCourses")
    public ModelAndView displayCourses(Model model, HttpSession session) {
        Person person = (Person) session.getAttribute("loggedInPerson");
        if (person == null) {
            return new ModelAndView("redirect:/login?logout=true");
        }
        ModelAndView mv = new ModelAndView("courses_enrolled.html");
        mv.addObject("person", person);
        return mv;
    }

    // ✅ STUDENT: View Materials për një kurs ku është i regjistruar
    @GetMapping("/course/{id}/materials")
    public String viewCourseMaterials(@PathVariable("id") int courseId,
                                      Model model,
                                      HttpSession session) {

        Person student = (Person) session.getAttribute("loggedInPerson");
        if (student == null) {
            return "redirect:/login?logout=true";
        }

        Courses course = coursesRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // 🔐 lejo vetëm nëse studenti është i regjistruar në këtë kurs
        boolean enrolled = student.getCourses().stream()
                .anyMatch(c -> c.getCourseId() == courseId);

        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled in this course");
        }

        List<CourseMaterial> materials = materialRepository.findByCourse(course);

        model.addAttribute("course", course);
        model.addAttribute("materials", materials);

        return "student_course_materials.html";
    }

    @PostMapping("/courses/{courseId}/enroll")
    public String requestEnroll(@PathVariable int courseId, HttpSession session, RedirectAttributes ra) {
        Person student = (Person) session.getAttribute("loggedInPerson");
        if (student == null) {
            return "redirect:/login";
        }
        Courses course = coursesRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            courseRequestService.createEnrollRequest(student, course);
            ra.addFlashAttribute("successMessage", "Enrollment request submitted");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/courses/{courseId}/unenroll")
    public String requestUnenroll(@PathVariable int courseId, HttpSession session, RedirectAttributes ra) {
        Person student = (Person) session.getAttribute("loggedInPerson");
        if (student == null) {
            return "redirect:/login";
        }
        Courses course = coursesRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            courseRequestService.createUnenrollRequest(student, course);
            ra.addFlashAttribute("successMessage", "Unenrollment request submitted");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/courses/{courseId}/rate")
    public String rateCourse(@PathVariable int courseId, @RequestParam int rating,
                             HttpSession session, RedirectAttributes ra) {
        Person student = (Person) session.getAttribute("loggedInPerson");
        if (student == null) {
            return "redirect:/login";
        }
        Courses course = coursesRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            courseRatingService.submitRating(student, course, rating);
            ra.addFlashAttribute("successMessage", "Rating submitted");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/courses/" + courseId;
    }
}
