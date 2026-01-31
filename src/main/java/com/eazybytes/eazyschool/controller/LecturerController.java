package com.eazybytes.eazyschool.controller;

import com.eazybytes.eazyschool.model.CourseMaterial;
import com.eazybytes.eazyschool.model.Courses;
import com.eazybytes.eazyschool.model.Person;
import com.eazybytes.eazyschool.repository.CourseMaterialRepository;
import com.eazybytes.eazyschool.repository.CoursesRepository;
import com.eazybytes.eazyschool.repository.PersonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/lecturer")
public class LecturerController {

    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10MB
    private static final int MAX_DOCS = 8;

    private final PersonRepository personRepository;
    private final CoursesRepository coursesRepository;
    private final CourseMaterialRepository materialRepository;

    @Value("${eazyschool.upload.dir:uploads}")
    private String uploadDir;

    public LecturerController(PersonRepository personRepository,
                              CoursesRepository coursesRepository,
                              CourseMaterialRepository materialRepository) {
        this.personRepository = personRepository;
        this.coursesRepository = coursesRepository;
        this.materialRepository = materialRepository;
    }

    // 1) Kursët e pedagogut
    @GetMapping("/courses")
    public String myCourses(Model model, Authentication auth) {
        Person me = personRepository.readByEmail(auth.getName());
        List<Courses> myCourses = coursesRepository.findByLecturer(me);
        model.addAttribute("courses", myCourses);
        return "lecturer_courses.html";
    }

    // 2) Materials page (përdor lecturer_materials.html)
    @GetMapping("/course/{id}/materials")
    public String courseMaterials(@PathVariable int id, Model model, Authentication auth) {
        Person me = personRepository.readByEmail(auth.getName());
        Courses course = coursesRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (course.getLecturer() == null || course.getLecturer().getPersonId() != me.getPersonId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your course");
        }

        model.addAttribute("course", course);
        model.addAttribute("materials", materialRepository.findByCourse(course));
        model.addAttribute("materialsCount", materialRepository.countByCourse(course));
        return "lecturer_materials.html";
    }

    // 3) Students page (përdor lecturer_course_students.html)
    @GetMapping("/course/{id}/students")
    public String courseStudents(@PathVariable int id, Model model, Authentication auth) {
        Person me = personRepository.readByEmail(auth.getName());
        Courses course = coursesRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (course.getLecturer() == null || course.getLecturer().getPersonId() != me.getPersonId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your course");
        }

        model.addAttribute("course", course);
        model.addAttribute("students", course.getPersons());
        return "lecturer_course_students.html";
    }

    // 4) Upload PDF (<=10MB) + max 8 docs
    @PostMapping(value = "/course/{id}/upload", consumes = "multipart/form-data")
    public String upload(@PathVariable int id,
                         @RequestParam("file") MultipartFile file,
                         RedirectAttributes ra,
                         Authentication auth) throws IOException {

        Person me = personRepository.readByEmail(auth.getName());
        Courses course = coursesRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (course.getLecturer() == null || course.getLecturer().getPersonId() != me.getPersonId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your course");
        }

        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "File is empty.");
            return "redirect:/lecturer/course/" + id + "/materials";
        }

        if (file.getSize() > MAX_BYTES) {
            ra.addFlashAttribute("errorMessage", "Max size is 10MB.");
            return "redirect:/lecturer/course/" + id + "/materials";
        }

        String ct = file.getContentType();
        if (ct == null || !ct.equalsIgnoreCase("application/pdf")) {
            ra.addFlashAttribute("errorMessage", "Only PDF files are allowed.");
            return "redirect:/lecturer/course/" + id + "/materials";
        }

        if (materialRepository.countByCourse(course) >= MAX_DOCS) {
            ra.addFlashAttribute("errorMessage", "Max " + MAX_DOCS + " materials allowed for this course.");
            return "redirect:/lecturer/course/" + id + "/materials";
        }

        // save file: uploads/materials/<courseId>/
        String safeName = UUID.randomUUID() + ".pdf";
        Path courseDir = Paths.get(uploadDir, "materials", String.valueOf(course.getCourseId()))
                .toAbsolutePath().normalize();
        Files.createDirectories(courseDir);

        Path target = courseDir.resolve(safeName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // save metadata in DB (emrat e tu)
        CourseMaterial m = new CourseMaterial();
        m.setCourse(course);
        m.setOriginalName(file.getOriginalFilename());
        m.setStoredName(safeName);
        m.setContentType(ct);
        m.setFileSize(file.getSize());
        materialRepository.save(m);

        ra.addFlashAttribute("successMessage", "Material uploaded successfully.");
        return "redirect:/lecturer/course/" + id + "/materials";
    }

    // 5) Delete
    @PostMapping("/material/{materialId}/delete")
    public String deleteMaterial(@PathVariable Long materialId,
                                 RedirectAttributes ra,
                                 Authentication auth) throws IOException {

        Person me = personRepository.readByEmail(auth.getName());

        CourseMaterial m = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found"));

        Courses course = m.getCourse();

        if (course.getLecturer() == null || course.getLecturer().getPersonId() != me.getPersonId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your course");
        }

        Path filePath = Paths.get(uploadDir, "materials", String.valueOf(course.getCourseId()), m.getStoredName())
                .toAbsolutePath().normalize();

        Files.deleteIfExists(filePath);
        materialRepository.delete(m);

        ra.addFlashAttribute("successMessage", "Material deleted.");
        return "redirect:/lecturer/course/" + course.getCourseId() + "/materials";
    }
}

