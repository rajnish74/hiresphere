package com.hiresphere.notification.dto;

import com.hiresphere.notification.entity.NotificationChannel;

public record ChannelChoice(Boolean email, Boolean inApp) {

    public Boolean valueFor(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL ? email : inApp;
    }
}