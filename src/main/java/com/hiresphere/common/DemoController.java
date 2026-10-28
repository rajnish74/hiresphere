package com.hiresphere.common;

import com.hiresphere.common.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> adminOnly() {
        return ApiResponse.ok("Hello Admin");
    }

    @GetMapping("/recruiter")
    @PreAuthorize("hasRole('RECRUITER')")
    public ApiResponse<String> recruiterOnly() {
        return ApiResponse.ok("Hello Recruiter");
    }
}