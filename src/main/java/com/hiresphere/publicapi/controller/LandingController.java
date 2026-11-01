package com.hiresphere.publicapi.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.job.dto.SkillCount;
import com.hiresphere.job.dto.TopCompany;
import com.hiresphere.publicapi.dto.PublicStats;
import com.hiresphere.publicapi.service.LandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class LandingController {

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic();

    private final LandingService landingService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PublicStats>> stats() {
        return ResponseEntity.ok().cacheControl(CACHE).body(ApiResponse.ok(landingService.stats()));
    }

    @GetMapping("/top-companies")
    public ResponseEntity<ApiResponse<List<TopCompany>>> topCompanies(@RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok().cacheControl(CACHE).body(ApiResponse.ok(landingService.topCompanies(limit)));
    }

    @GetMapping("/popular-skills")
    public ResponseEntity<ApiResponse<List<SkillCount>>> popularSkills(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok().cacheControl(CACHE).body(ApiResponse.ok(landingService.popularSkills(limit)));
    }
}