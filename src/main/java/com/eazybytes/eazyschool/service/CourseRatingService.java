package com.eazybytes.eazyschool.service;

import com.eazybytes.eazyschool.model.CourseRating;
import com.eazybytes.eazyschool.model.Courses;
import com.eazybytes.eazyschool.model.Person;
import com.eazybytes.eazyschool.repository.CourseRatingRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CourseRatingService {

    private final CourseRatingRepository courseRatingRepository;

    public CourseRatingService(CourseRatingRepository courseRatingRepository) {
        this.courseRatingRepository = courseRatingRepository;
    }

    public void submitRating(Person student, Courses course, int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        if (courseRatingRepository.existsByStudentAndCourse(student, course)) {
            throw new IllegalStateException("You have already rated this course");
        }
        CourseRating cr = new CourseRating();
        cr.setStudent(student);
        cr.setCourse(course);
        cr.setRating(rating);
        courseRatingRepository.save(cr);
    }

    public Double getAverageRating(Courses course) {
        return courseRatingRepository.findAverageRatingByCourse(course);
    }

    public Optional<CourseRating> getStudentRating(Person student, Courses course) {
        return courseRatingRepository.findByStudentAndCourse(student, course);
    }
}
