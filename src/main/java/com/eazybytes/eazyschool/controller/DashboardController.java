package com.eazybytes.eazyschool.controller;

import com.eazybytes.eazyschool.repository.CoursesRepository;
import com.eazybytes.eazyschool.model.Courses;
import com.eazybytes.eazyschool.model.Person;
import com.eazybytes.eazyschool.repository.PersonRepository;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;


@Slf4j
@Controller
public class DashboardController {

    @Autowired
    PersonRepository personRepository;

    @Autowired
    private CoursesRepository coursesRepository;

    @Value("${eazyschool.pageSize}")
    private int defaultPageSize;

    @Value("${eazyschool.contact.successMsg}")
    private String message;

    @Autowired
    Environment environment;



    @RequestMapping("/dashboard") // ose @GetMapping("/dashboard")
    public String displayDashboard(Model model, Authentication authentication, HttpSession session) {

        Person person = personRepository.readByEmail(authentication.getName());

        boolean isLecturer = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LECTURER"));

        if (isLecturer) {
            model.addAttribute("lecturerCourses",
                    coursesRepository.findByLecturer(person));
        }

        model.addAttribute("username", person.getName());
        model.addAttribute("roles", authentication.getAuthorities().toString());

        if (person.getEazyClass() != null && person.getEazyClass().getName() != null) {
            model.addAttribute("enrolledClass", person.getEazyClass().getName());
        }

        session.setAttribute("loggedInPerson", person);
        return "dashboard.html";
    }

    private void logMessages() {
        log.error("Error message from the Dashboard page");
        log.warn("Warning message from the Dashboard page");
        log.info("Info message from the Dashboard page");
        log.debug("Debug message from the Dashboard page");
        log.trace("Trace message from the Dashboard page");

        log.error("defaultPageSize value with @Value annotation is : "+defaultPageSize);
        log.error("successMsg value with @Value annotation is : "+message);

        log.error("defaultPageSize value with Environment is : "+environment.getProperty("eazyschool.pageSize"));
        log.error("successMsg value with Environment is : "+environment.getProperty("eazyschool.contact.successMsg"));
        log.error("Java Home environment variable using Environment is : "+environment.getProperty("JAVA_HOME"));
    }

}
