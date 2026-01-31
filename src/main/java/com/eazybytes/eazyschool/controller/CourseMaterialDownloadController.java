package com.eazybytes.eazyschool.controller;

import com.eazybytes.eazyschool.model.CourseMaterial;
import com.eazybytes.eazyschool.repository.CourseMaterialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class CourseMaterialDownloadController {

    private final CourseMaterialRepository courseMaterialRepository;

    @Value("${eazyschool.upload.dir:uploads}")
    private String uploadDir;

    public CourseMaterialDownloadController(CourseMaterialRepository courseMaterialRepository) {
        this.courseMaterialRepository = courseMaterialRepository;
    }

    @GetMapping("/course-materials/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {

        // 1️⃣ gjej materialin në DB
        CourseMaterial m = courseMaterialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found"));

        // 2️⃣ path real i file-it
        Path filePath = Paths.get(uploadDir, "course-materials")
                .toAbsolutePath()
                .normalize()
                .resolve(m.getStoredName())
                .normalize();

        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());

        // 3️⃣ force download
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + m.getOriginalName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE,
                        m.getContentType() != null ? m.getContentType() : "application/pdf")
                .body(resource);
    }
}
