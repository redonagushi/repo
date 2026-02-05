package com.eazybytes.eazyschool.repository;

import com.eazybytes.eazyschool.model.CourseRating;
import com.eazybytes.eazyschool.model.Courses;
import com.eazybytes.eazyschool.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRatingRepository extends JpaRepository<CourseRating, Long> {

    Optional<CourseRating> findByStudentAndCourse(Person student, Courses course);

    @Query("SELECT AVG(r.rating) FROM CourseRating r WHERE r.course = :course")
    Double findAverageRatingByCourse(@Param("course") Courses course);

    boolean existsByStudentAndCourse(Person student, Courses course);
}
