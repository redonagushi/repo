package com.eazybytes.eazyschool.service;

import com.eazybytes.eazyschool.model.*;
import com.eazybytes.eazyschool.repository.CourseRequestRepository;
import com.eazybytes.eazyschool.repository.CoursesRepository;
import com.eazybytes.eazyschool.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseRequestService {

    private final CourseRequestRepository courseRequestRepository;
    private final PersonRepository personRepository;
    private final CoursesRepository coursesRepository;

    public CourseRequestService(CourseRequestRepository courseRequestRepository,
                                PersonRepository personRepository,
                                CoursesRepository coursesRepository) {
        this.courseRequestRepository = courseRequestRepository;
        this.personRepository = personRepository;
        this.coursesRepository = coursesRepository;
    }

    public void createEnrollRequest(Person student, Courses course) {
        if (courseRequestRepository.existsByStudentAndCourseAndType(student, course, RequestType.ENROLL)) {
            throw new IllegalStateException("Enrollment request already exists");
        }
        boolean enrolled = student.getCourses().stream()
                .anyMatch(c -> c.getCourseId() == course.getCourseId());
        if (enrolled) {
            throw new IllegalStateException("Already enrolled in this course");
        }
        CourseRequest request = new CourseRequest();
        request.setStudent(student);
        request.setCourse(course);
        request.setType(RequestType.ENROLL);
        request.setStatus(RequestStatus.PENDING);
        courseRequestRepository.save(request);
    }

    public void createUnenrollRequest(Person student, Courses course) {
        if (courseRequestRepository.existsByStudentAndCourseAndType(student, course, RequestType.UNENROLL)) {
            throw new IllegalStateException("Unenrollment request already exists");
        }
        boolean enrolled = student.getCourses().stream()
                .anyMatch(c -> c.getCourseId() == course.getCourseId());
        if (!enrolled) {
            throw new IllegalStateException("Not enrolled in this course");
        }
        CourseRequest request = new CourseRequest();
        request.setStudent(student);
        request.setCourse(course);
        request.setType(RequestType.UNENROLL);
        request.setStatus(RequestStatus.PENDING);
        courseRequestRepository.save(request);
    }

    @Transactional
    public void approveRequest(Long requestId, Person admin) {
        CourseRequest request = courseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }

        Person student = personRepository.findById(request.getStudent().getPersonId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        Courses course = coursesRepository.findById(request.getCourse().getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (request.getType() == RequestType.ENROLL) {
            student.getCourses().add(course);
            course.getPersons().add(student);
            personRepository.save(student);
        } else {
            student.getCourses().remove(course);
            course.getPersons().remove(student);
            personRepository.save(student);
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(admin);
        courseRequestRepository.save(request);
    }

    @Transactional
    public void rejectRequest(Long requestId, Person admin) {
        CourseRequest request = courseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }
        request.setStatus(RequestStatus.REJECTED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(admin);
        courseRequestRepository.save(request);
    }

    public List<CourseRequest> getAllRequests() {
        return courseRequestRepository.findAllByOrderByCreatedAtDesc();
    }
}
