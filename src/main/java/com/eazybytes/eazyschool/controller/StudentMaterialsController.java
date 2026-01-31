package com.eazybytes.eazyschool.controller;

import com.eazybytes.eazyschool.model.Courses;
import com.eazybytes.eazyschool.model.CourseMaterial;
import com.eazybytes.eazyschool.model.Person;
import com.eazybytes.eazyschool.repository.CoursesRepository;
import com.eazybytes.eazyschool.repository.CourseMaterialRepository;
import com.eazybytes.eazyschool.repository.PersonRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentMaterialsController {

    private final PersonRepository personRepository;
    private final CoursesRepository coursesRepository;
    private final CourseMaterialRepository materialRepository;

    public StudentMaterialsController(PersonRepository personRepository,
                                      CoursesRepository coursesRepository,
                                      CourseMaterialRepository materialRepository) {
        this.personRepository = personRepository;
        this.coursesRepository = coursesRepository;
        this.materialRepository = materialRepository;
    }

    @RequestMapping("/student/materials")
    @GetMapping("/course/{id}")
    public String courseMaterials(@PathVariable int id, Model model, Authentication auth) {

        Person me = personRepository.readByEmail(auth.getName());
        Courses course = coursesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // ✅ lejo vetëm nëse studenti është i regjistruar në këtë kurs
        boolean enrolled = me.getCourses().stream().anyMatch(c -> c.getCourseId() == id);
        if (!enrolled) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not enrolled in this course");
        }

        List<CourseMaterial> materials = materialRepository.findByCourse(course);

        model.addAttribute("course", course);
        model.addAttribute("materials", materials);
        return "student_materials.html";
    }
}
