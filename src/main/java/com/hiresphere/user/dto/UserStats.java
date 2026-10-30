package com.hiresphere.user.dto;

import com.hiresphere.user.entity.Role;

import java.util.Map;

public record UserStats(long total,
                        Map<Role, Long> byRole,
                        long blocked,
                        long unverified,
                        long newLast7Days) {
}