package com.gdn.project.waroenk.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Controller for serving the monitoring dashboard UI.
 */
@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Monitoring dashboard UI")
public class DashboardController {

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Dashboard UI", description = "Monitoring dashboard for all microservices")
    public ResponseEntity<String> getDashboard() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/dashboard.html");
        try (InputStream is = resource.getInputStream()) {
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(html);
        }
    }
}




