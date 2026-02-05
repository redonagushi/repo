package com.eazybytes.eazyschool.controller;

import com.eazybytes.eazyschool.model.*;
import com.eazybytes.eazyschool.repository.CourseRequestRepository;
import com.eazybytes.eazyschool.repository.CourseMaterialRepository;
import com.eazybytes.eazyschool.repository.CoursesRepository;
import com.eazybytes.eazyschool.service.CourseRatingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class CourseController {

    private final CoursesRepository coursesRepository;
    private final CourseRatingService courseRatingService;
    private final CourseRequestRepository courseRequestRepository;
    private final CourseMaterialRepository courseMaterialRepository;

    public CourseController(CoursesRepository coursesRepository,
                            CourseRatingService courseRatingService,
                            CourseRequestRepository courseRequestRepository,
                            CourseMaterialRepository courseMaterialRepository) {
        this.coursesRepository = coursesRepository;
        this.courseRatingService = courseRatingService;
        this.courseRequestRepository = courseRequestRepository;
        this.courseMaterialRepository = courseMaterialRepository;
    }

    @GetMapping("/courses")
    public String listCourses(Model model) {
        List<Courses> courses = coursesRepository.findByOrderByName();
        Map<Integer, Double> ratingsMap = new HashMap<>();
        Map<Integer, Long> materialsCountMap = new HashMap<>();
        for (Courses course : courses) {
            Double avg = courseRatingService.getAverageRating(course);
            ratingsMap.put(course.getCourseId(), avg);
            materialsCountMap.put(course.getCourseId(), courseMaterialRepository.countByCourse(course));
        }
        model.addAttribute("courses", courses);
        model.addAttribute("ratingsMap", ratingsMap);
        model.addAttribute("materialsCountMap", materialsCountMap);
        return "courses.html";
    }

    @GetMapping("/courses/{courseId}")
    public String courseDetail(@PathVariable int courseId, Model model, HttpSession session) {
        Courses course = coursesRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        model.addAttribute("course", course);
        model.addAttribute("avgRating", courseRatingService.getAverageRating(course));
        model.addAttribute("materialsCount", courseMaterialRepository.countByCourse(course));

        Person person = (Person) session.getAttribute("loggedInPerson");
        boolean loggedIn = person != null;
        boolean isStudent = loggedIn && "STUDENT".equalsIgnoreCase(person.getRoles().getRoleName());
        boolean enrolled = false;
        boolean hasEnrollRequest = false;
        boolean hasUnenrollRequest = false;
        boolean hasRated = false;
        CourseRating studentRating = null;

        if (isStudent) {
            enrolled = person.getCourses().stream()
                    .anyMatch(c -> c.getCourseId() == courseId);
            hasEnrollRequest = courseRequestRepository.existsByStudentAndCourseAndType(
                    person, course, RequestType.ENROLL);
            hasUnenrollRequest = courseRequestRepository.existsByStudentAndCourseAndType(
                    person, course, RequestType.UNENROLL);
            Optional<CourseRating> ratingOpt = courseRatingService.getStudentRating(person, course);
            hasRated = ratingOpt.isPresent();
            studentRating = ratingOpt.orElse(null);
        }

        if (enrolled) {
            model.addAttribute("materials", courseMaterialRepository.findByCourse(course));
        }

        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("isStudent", isStudent);
        model.addAttribute("enrolled", enrolled);
        model.addAttribute("hasEnrollRequest", hasEnrollRequest);
        model.addAttribute("hasUnenrollRequest", hasUnenrollRequest);
        model.addAttribute("hasRated", hasRated);
        model.addAttribute("studentRating", studentRating);

        return "course_detail.html";
    }
}
