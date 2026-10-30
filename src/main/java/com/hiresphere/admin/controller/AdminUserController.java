package com.hiresphere.admin.controller;

import com.hiresphere.admin.service.AdminActionService;
import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.recruiter.dto.ReasonRequest;
import com.hiresphere.user.dto.AdminUserResponse;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final AdminActionService actions;

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userService.search(q, role, enabled, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.getForAdmin(id));
    }

    @PostMapping("/{id}/block")
    public ApiResponse<AdminUserResponse> block(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.ok("User blocked",
                actions.blockUser(Long.valueOf(jwt.getSubject()), id, request.reason().trim()));
    }

    @PostMapping("/{id}/unblock")
    public ApiResponse<AdminUserResponse> unblock(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ApiResponse.ok("User unblocked", actions.unblockUser(Long.valueOf(jwt.getSubject()), id));
    }
}