package com.eazybytes.eazyschool.controller;

import com.eazybytes.eazyschool.model.Courses;
import com.eazybytes.eazyschool.model.CourseMaterial;
import com.eazybytes.eazyschool.model.Person;
import com.eazybytes.eazyschool.repository.CoursesRepository;
import com.eazybytes.eazyschool.repository.CourseMaterialRepository;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("student")
public class StudentController {

    private final CoursesRepository coursesRepository;
    private final CourseMaterialRepository materialRepository;

    public StudentController(CoursesRepository coursesRepository,
                             CourseMaterialRepository materialRepository) {
        this.coursesRepository = coursesRepository;
        this.materialRepository = materialRepository;
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
}
