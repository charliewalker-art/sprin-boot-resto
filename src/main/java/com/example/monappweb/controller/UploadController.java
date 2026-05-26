package com.example.monappweb.controller;

import com.example.monappweb.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        try {
            String url = uploadService.uploadImage(file);
            return ResponseEntity.ok(Map.of("imageUrl", url));
        } catch (IOException e) {
            throw new RuntimeException("Erreur upload : " + e.getMessage());
        }
    }
}