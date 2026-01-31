package com.eazybytes.eazyschool.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class CourseMaterial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long materialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Courses course;

    private String originalName;
    private String storedName;
    private String contentType;
    private long fileSize;

    private LocalDateTime uploadedAt = LocalDateTime.now();
}

