package com.eazybytes.eazyschool.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "courses")
public class Courses extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(name = "course_id")
    private int courseId;

    @Column(name = "name")
    private String name;

    @Column(name = "fees")
    private String fees;

    @Size(max = 600, message = "Description max 600 characters")
    @Column(name = "description", length = 600)
    private String description;

    @Column(name = "image_name")
    private String imageName;

    @ManyToMany(mappedBy = "courses", fetch = FetchType.EAGER)
    private Set<Person> persons = new HashSet<>();

    // ✅ FIX: mos përdor referencedColumnName="personId"
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lecturer_id", nullable = true)
    private Person lecturer;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseMaterial> materials = new ArrayList<>();
}
