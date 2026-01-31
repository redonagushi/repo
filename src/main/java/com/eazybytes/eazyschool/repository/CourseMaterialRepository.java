package com.eazybytes.eazyschool.repository;

import com.eazybytes.eazyschool.model.CourseMaterial;
import com.eazybytes.eazyschool.model.Courses;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Long> {
    long countByCourse(Courses course);
    List<CourseMaterial> findByCourse(Courses course);
}
