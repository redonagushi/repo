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
public class Courses extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private int courseId;

    private String name;
    private String fees;

    // ✅ NEW: përshkrimi (max 600)
    @Size(max = 600, message = "Description max 600 characters")
    @Column(length = 600)
    private String description;

    // ✅ NEW: emri i file-it të imazhit të ruajtur
    @Column(name = "image_name")
    private String imageName;

    // STUDENTËT e regjistruar
    @ManyToMany(mappedBy = "courses", fetch = FetchType.EAGER)
    private Set<Person> persons = new HashSet<>();

    // ✅ Pedagogu përgjegjës për kursin
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lecturer_id", referencedColumnName = "personId")
    private Person lecturer;

    // ✅ Materialet e kursit
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseMaterial> materials = new ArrayList<>();
}
