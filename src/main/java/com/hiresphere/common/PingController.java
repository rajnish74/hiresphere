package com.hiresphere.common;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.exception.DuplicateResourceException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ping")
public class PingController {

    @GetMapping
    public ApiResponse<String> ping() {
        return ApiResponse.ok("pong");
    }

    @GetMapping("/error")
    public ApiResponse<String> error() {
        throw new ResourceNotFoundException("Demo resource not found");
    }

    @GetMapping("/duplicate")
    public ApiResponse<String> duplicate() {
        throw new DuplicateResourceException("Demo duplicate resource");
    }
}