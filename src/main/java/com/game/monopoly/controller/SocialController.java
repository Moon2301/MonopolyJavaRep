package com.game.monopoly.controller;

import com.game.monopoly.dto.*;
import com.game.monopoly.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;

    @GetMapping("/friends")
    public List<FriendListItemResponse> getFriends(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return socialService.getFriends(accountId);
    }

    @PostMapping("/friends/request")
    public FriendListItemResponse sendFriendRequest(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody FriendRequestDto request
    ) {
        return socialService.sendFriendRequest(accountId, request.getUsername());
    }

    @PostMapping("/room-invite")
    public MessageItemResponse sendRoomInvite(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody RoomInviteRequest request
    ) {
        return socialService.sendRoomInviteFromRequest(accountId, request);
    }

    @GetMapping("/notifications")
    public List<NotificationItemResponse> getNotifications(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return socialService.getNotifications(accountId);
    }

    @GetMapping("/notifications/unread-count")
    public NotificationUnreadResponse getUnreadNotificationCount(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return socialService.getUnreadNotificationCount(accountId);
    }

    @PostMapping("/notifications/{notificationId}/read")
    public MessageResponse markNotificationRead(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @PathVariable Long notificationId
    ) {
        return socialService.markNotificationRead(accountId, notificationId);
    }

    @PostMapping("/notifications/read-all")
    public MessageResponse markAllNotificationsRead(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return socialService.markAllNotificationsRead(accountId);
    }

    @PostMapping("/friends/{friendId}/accept")
    public FriendListItemResponse acceptFriendRequest(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @PathVariable Long friendId
    ) {
        return socialService.acceptFriendRequest(accountId, friendId);
    }

    @GetMapping("/messages")
    public List<MessageItemResponse> getConversation(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestParam("friendUserProfileId") Long friendUserProfileId
    ) {
        return socialService.getConversation(accountId, friendUserProfileId);
    }

    @PostMapping("/messages")
    public MessageItemResponse sendMessage(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody SendMessageRequest request
    ) {
        return socialService.sendMessage(accountId, request.getToUserProfileId(), request.getContent());
    }

    @GetMapping("/channel/messages")
    public List<MessageItemResponse> getChannelMessages(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return socialService.getChannelMessages(accountId);
    }

    @PostMapping("/channel/messages")
    public MessageItemResponse sendChannelMessage(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody SendMessageRequest request
    ) {
        return socialService.sendChannelMessage(accountId, request.getContent());
    }
}
