package com.eazybytes.eazyschool.controller;

import com.eazybytes.eazyschool.model.*;
import com.eazybytes.eazyschool.repository.CoursesRepository;
import com.eazybytes.eazyschool.repository.EazyClassRepository;
import com.eazybytes.eazyschool.repository.PersonRepository;
import com.eazybytes.eazyschool.service.CourseRequestService;
import com.eazybytes.eazyschool.service.PersonService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import com.eazybytes.eazyschool.model.LecturerForm;


import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("admin")
public class AdminController {
    @Value("${eazyschool.upload.dir:uploads}")
    private String uploadDir;
    @Autowired
    EazyClassRepository eazyClassRepository;

    @Autowired
    PersonRepository personRepository;

    @Autowired
    CoursesRepository coursesRepository;

    @Autowired
    private PersonService personService;

    @Autowired
    private CourseRequestService courseRequestService;

    @RequestMapping("/displayClasses")
    public ModelAndView displayClasses(Model model) {
        List<EazyClass> eazyClasses = eazyClassRepository.findAll();
        ModelAndView modelAndView = new ModelAndView("classes.html");
        modelAndView.addObject("eazyClasses",eazyClasses);
        modelAndView.addObject("eazyClass", new EazyClass());
        return modelAndView;
    }

    @PostMapping("/addNewClass")
    public ModelAndView addNewClass(Model model, @ModelAttribute("eazyClass") EazyClass eazyClass) {
        eazyClassRepository.save(eazyClass);
        ModelAndView modelAndView = new ModelAndView("redirect:/admin/displayClasses");
        return modelAndView;
    }

    @RequestMapping("/deleteClass")
    public ModelAndView deleteClass(Model model, @RequestParam int id) {
        Optional<EazyClass> eazyClass = eazyClassRepository.findById(id);
        for(Person person : eazyClass.get().getPersons()){
            person.setEazyClass(null);
            personRepository.save(person);
        }
        eazyClassRepository.deleteById(id);
        ModelAndView modelAndView = new ModelAndView("redirect:/admin/displayClasses");
        return modelAndView;
    }

    @GetMapping("/displayStudents")
    public ModelAndView displayStudents(Model model, @RequestParam int classId, HttpSession session,
                                        @RequestParam(value = "error", required = false) String error) {
        String errorMessage = null;
        ModelAndView modelAndView = new ModelAndView("students.html");
        Optional<EazyClass> eazyClass = eazyClassRepository.findById(classId);
        modelAndView.addObject("eazyClass",eazyClass.get());
        modelAndView.addObject("person",new Person());
        session.setAttribute("eazyClass",eazyClass.get());
        if(error != null) {
            errorMessage = "Invalid Email entered!!";
            modelAndView.addObject("errorMessage", errorMessage);
        }
        return modelAndView;
    }

    @PostMapping("/addStudent")
    public ModelAndView addStudent(Model model, @ModelAttribute("person") Person person, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView();
        EazyClass eazyClass = (EazyClass) session.getAttribute("eazyClass");
        Person personEntity = personRepository.readByEmail(person.getEmail());
        if(personEntity==null || !(personEntity.getPersonId()>0)){
            modelAndView.setViewName("redirect:/admin/displayStudents?classId="+eazyClass.getClassId()
                    +"&error=true");
            return modelAndView;
        }
        personEntity.setEazyClass(eazyClass);
        personRepository.save(personEntity);
        eazyClass.getPersons().add(personEntity);
        eazyClassRepository.save(eazyClass);
        modelAndView.setViewName("redirect:/admin/displayStudents?classId="+eazyClass.getClassId());
        return modelAndView;
    }

    @GetMapping("/deleteStudent")
    public ModelAndView deleteStudent(Model model, @RequestParam int personId, HttpSession session) {
        EazyClass eazyClass = (EazyClass) session.getAttribute("eazyClass");
        Optional<Person> person = personRepository.findById(personId);
        person.get().setEazyClass(null);
        eazyClass.getPersons().remove(person.get());
        EazyClass eazyClassSaved = eazyClassRepository.save(eazyClass);
        session.setAttribute("eazyClass",eazyClassSaved);
        ModelAndView modelAndView = new ModelAndView("redirect:/admin/displayStudents?classId="+eazyClass.getClassId());
        return modelAndView;
    }

    @GetMapping("/displayCourses")
    public String displayCourses(Model model) {
        model.addAttribute("courses", coursesRepository.findAll());
        model.addAttribute("course", new Courses());
        model.addAttribute("lecturers", personRepository.findByRoles_RoleName("LECTURER"));
        return "courses_secure.html";
    }

    @PostMapping("/addNewCourse")
    public String addNewCourse(@Valid @ModelAttribute("course") Courses course,
                               BindingResult br,
                               @RequestParam(value = "image", required = false) MultipartFile image,
                               RedirectAttributes ra) {

        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMessage", "Validation error!");
            return "redirect:/admin/displayCourses";
        }

        try {
            if (image != null && !image.isEmpty()) {

                if (image.getContentType() == null || !image.getContentType().startsWith("image/")) {
                    ra.addFlashAttribute("errorMessage", "Only image files allowed!");
                    return "redirect:/admin/displayCourses";
                }

                String original = image.getOriginalFilename() != null ? image.getOriginalFilename() : "";
                String ext = "";
                int dot = original.lastIndexOf('.');
                if (dot >= 0) {
                    ext = original.substring(dot);
                }

                String storedName = java.util.UUID.randomUUID() + ext;

                Path dir = Paths.get(uploadDir, "course-images").toAbsolutePath().normalize();
                Files.createDirectories(dir);

                Path target = dir.resolve(storedName).normalize();
                Files.copy(image.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                course.setImageName(storedName);
            }

            coursesRepository.save(course);

            ra.addFlashAttribute("successMessage", "Course added successfully!");
            return "redirect:/admin/displayCourses";

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error saving course: " + e.getMessage());
            return "redirect:/admin/displayCourses";
        }
    }

    @GetMapping("/viewStudents")
    public ModelAndView viewStudents(Model model, @RequestParam int id
                 ,HttpSession session,@RequestParam(required = false) String error) {
        String errorMessage = null;
        ModelAndView modelAndView = new ModelAndView("course_students.html");
        Optional<Courses> courses = coursesRepository.findById(id);
        modelAndView.addObject("courses",courses.get());
        modelAndView.addObject("person",new Person());
        session.setAttribute("courses",courses.get());
        if(error != null) {
            errorMessage = "Invalid Email entered!!";
            modelAndView.addObject("errorMessage", errorMessage);
        }
        return modelAndView;
    }

    @PostMapping("/addStudentToCourse")
    public ModelAndView addStudentToCourse(Model model, @ModelAttribute("person") Person person,
                                           HttpSession session) {
        ModelAndView modelAndView = new ModelAndView();
        Courses courses = (Courses) session.getAttribute("courses");
        Person personEntity = personRepository.readByEmail(person.getEmail());
        if(personEntity==null || !(personEntity.getPersonId()>0)){
            modelAndView.setViewName("redirect:/admin/viewStudents?id="+courses.getCourseId()
                    +"&error=true");
            return modelAndView;
        }
        personEntity.getCourses().add(courses);
        courses.getPersons().add(personEntity);
        personRepository.save(personEntity);
        session.setAttribute("courses",courses);
        modelAndView.setViewName("redirect:/admin/viewStudents?id="+courses.getCourseId());
        return modelAndView;
    }

    @GetMapping("/deleteStudentFromCourse")
    public ModelAndView deleteStudentFromCourse(Model model, @RequestParam int personId,
                                                HttpSession session) {
        Courses courses = (Courses) session.getAttribute("courses");
        Optional<Person> person = personRepository.findById(personId);
        person.get().getCourses().remove(courses);
        courses.getPersons().remove(person);
        personRepository.save(person.get());
        session.setAttribute("courses",courses);
        ModelAndView modelAndView = new
                ModelAndView("redirect:/admin/viewStudents?id="+courses.getCourseId());
        return modelAndView;
    }

    @PostMapping("/assignLecturerToCourse")
    public String assignLecturerToCourse(@RequestParam int courseId,
                                         @RequestParam int lecturerId) {

        Courses course = coursesRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Person lecturer = personRepository.findById(lecturerId)
                .orElseThrow(() -> new RuntimeException("Lecturer not found"));

        if (!"LECTURER".equalsIgnoreCase(lecturer.getRoles().getRoleName())) {
            throw new RuntimeException("This user is not a LECTURER");
        }

        course.setLecturer(lecturer);
        coursesRepository.save(course);

        return "redirect:/admin/displayCourses";
    }

    @GetMapping("/displayLecturers")
    public ModelAndView displayLecturers() {
        ModelAndView mv = new ModelAndView("lecturers.html");
        mv.addObject("lecturerForm", new LecturerForm());
        return mv;
    }

    @PostMapping("/createLecturer")
    public ModelAndView createLecturer(
            @Valid @ModelAttribute("lecturerForm") LecturerForm form,
            Errors errors) {

        if (errors.hasErrors()) {
            ModelAndView mv = new ModelAndView("lecturers.html");
            mv.addObject("lecturerForm", form);
            return mv;
        }

        Person p = new Person();
        p.setName(form.getName());
        p.setMobileNumber(form.getMobileNumber());
        p.setEmail(form.getEmail());
        p.setPwd(form.getPwd());

        personService.createLecturer(p);

        return new ModelAndView("redirect:/admin/displayLecturers");
    }

    @GetMapping("/requests")
    public String displayRequests(Model model) {
        model.addAttribute("requests", courseRequestService.getAllRequests());
        return "admin_requests.html";
    }

    @PostMapping("/requests/{id}/approve")
    public String approveRequest(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Person admin = (Person) session.getAttribute("loggedInPerson");
        try {
            courseRequestService.approveRequest(id, admin);
            ra.addFlashAttribute("successMessage", "Request approved");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/requests";
    }

    @PostMapping("/requests/{id}/reject")
    public String rejectRequest(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Person admin = (Person) session.getAttribute("loggedInPerson");
        try {
            courseRequestService.rejectRequest(id, admin);
            ra.addFlashAttribute("successMessage", "Request rejected");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/requests";
    }
}
