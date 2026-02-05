package com.eazybytes.eazyschool.repository;

import com.eazybytes.eazyschool.model.CourseRequest;
import com.eazybytes.eazyschool.model.Courses;
import com.eazybytes.eazyschool.model.Person;
import com.eazybytes.eazyschool.model.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRequestRepository extends JpaRepository<CourseRequest, Long> {

    Optional<CourseRequest> findByStudentAndCourseAndType(Person student, Courses course, RequestType type);

    List<CourseRequest> findAllByOrderByCreatedAtDesc();

    boolean existsByStudentAndCourseAndType(Person student, Courses course, RequestType type);
}
