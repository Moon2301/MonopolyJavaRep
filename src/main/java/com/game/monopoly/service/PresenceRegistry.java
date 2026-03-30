package com.game.monopoly.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/** Theo dõi hoạt động gần đây theo accountId (API có header X-Account-Id). */
@Component
public class PresenceRegistry {

    private static final long ONLINE_WINDOW_MS = 120_000L;

    private final ConcurrentHashMap<Long, Long> lastSeenMs = new ConcurrentHashMap<>();

    public void touch(Long accountId) {
        if (accountId != null) {
            lastSeenMs.put(accountId, System.currentTimeMillis());
        }
    }

    public boolean isOnline(Long accountId) {
        if (accountId == null) {
            return false;
        }
        Long t = lastSeenMs.get(accountId);
        return t != null && System.currentTimeMillis() - t < ONLINE_WINDOW_MS;
    }

    /**
     * Đã từng có heartbeat và hiện không còn sự kiện trong khoảng {@code absentMs} — dùng để máy chơi thay.
     * Chưa từng {@link #touch} → false (không coi là thoát ngay khi vào ván).
     */
    public boolean isDisconnectedLongerThan(Long accountId, long absentMs) {
        if (accountId == null || absentMs <= 0) {
            return false;
        }
        Long t = lastSeenMs.get(accountId);
        if (t == null) {
            return false;
        }
        return System.currentTimeMillis() - t > absentMs;
    }
}
