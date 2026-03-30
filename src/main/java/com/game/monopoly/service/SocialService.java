package com.game.monopoly.service;

import com.game.monopoly.dto.FriendListItemResponse;
import com.game.monopoly.dto.MessageItemResponse;
import com.game.monopoly.dto.RoomInviteRequest;
import com.game.monopoly.dto.MessageResponse;
import com.game.monopoly.dto.NotificationItemResponse;
import com.game.monopoly.dto.NotificationUnreadResponse;
import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.model.enums.RoomStatus;
import com.game.monopoly.model.inGameData.Room;
import com.game.monopoly.model.metaData.*;
import com.game.monopoly.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialService {

    private static final String DEFAULT_AVATAR = "/images/avatar-default.png";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final FriendRepository friendRepository;
    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final GameRepository gameRepository;
    private final PresenceRegistry presenceRegistry;

    @Transactional(readOnly = true)
    public String presenceStatusForProfile(Long userProfileId) {
        if (userProfileId == null) {
            return "OFFLINE";
        }
        boolean playing =
                !gameRepository
                        .findPlayingGamesForHumanProfile(userProfileId, GameStatus.PLAYING)
                        .isEmpty();
        if (playing) {
            return "PLAYING";
        }
        UserProfile up = userProfileRepository.findById(userProfileId).orElse(null);
        Long accId =
                up != null && up.getAccount() != null ? up.getAccount().getAccountId() : null;
        if (presenceRegistry.isOnline(accId)) {
            return "ONLINE";
        }
        return "OFFLINE";
    }

    @Transactional(readOnly = true)
    public List<FriendListItemResponse> getFriends(Long accountId) {
        UserProfile me = getCurrentProfile(accountId);
        List<Friend> relations = friendRepository.findAllByUserId(me.getUserProfileId());
        List<FriendListItemResponse> result = new ArrayList<>();

        for (Friend relation : relations) {
            UserProfile other = relation.getRequester().getUserProfileId().equals(me.getUserProfileId())
                    ? relation.getAddressee()
                    : relation.getRequester();

            String avatar = other.getAvatarUrl();
            if (avatar == null || avatar.isBlank()) {
                avatar = DEFAULT_AVATAR;
            }

            Long unread = messageRepository.countUnreadBySender(me.getUserProfileId(), other.getUserProfileId());
            boolean pendingOutgoing =
                    "PENDING".equals(relation.getStatus())
                            && relation.getRequester().getUserProfileId().equals(me.getUserProfileId());
            boolean canAccept =
                    "PENDING".equals(relation.getStatus())
                            && relation.getAddressee().getUserProfileId().equals(me.getUserProfileId());
            result.add(FriendListItemResponse.builder()
                    .friendId(relation.getFriendId())
                    .userProfileId(other.getUserProfileId())
                    .username(other.getUsername())
                    .avatarUrl(avatar)
                    .status(relation.getStatus())
                    .unreadMessages(unread)
                    .canAccept(canAccept)
                    .pendingOutgoing(pendingOutgoing)
                    .presenceStatus(presenceStatusForProfile(other.getUserProfileId()))
                    .build());
        }

        return result;
    }

    @Transactional
    public FriendListItemResponse sendFriendRequest(Long accountId, String friendUsername) {
        UserProfile me = getCurrentProfile(accountId);
        if (friendUsername == null || friendUsername.isBlank()) {
            throw new RuntimeException("username is required");
        }

        UserProfile target = userProfileRepository.findByUsername(friendUsername.trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (me.getUserProfileId().equals(target.getUserProfileId())) {
            throw new RuntimeException("Không thể kết bạn với chính mình");
        }

        Friend existing = friendRepository.findRelationBetween(me.getUserProfileId(), target.getUserProfileId())
                .orElse(null);
        if (existing != null) {
            throw new RuntimeException("Yêu cầu kết bạn đã tồn tại");
        }

        Friend friend = new Friend();
        friend.setRequester(me);
        friend.setAddressee(target);
        friend.setStatus("PENDING");
        friend = friendRepository.save(friend);

        userNotificationRepository.save(
                UserNotification.builder()
                        .recipient(target)
                        .sender(me)
                        .type("FRIEND_REQUEST")
                        .title("Lời mời kết bạn")
                        .body(me.getUsername() + " muốn kết bạn với bạn.")
                        .read(false)
                        .build());

        String avatar = target.getAvatarUrl();
        if (avatar == null || avatar.isBlank()) {
            avatar = DEFAULT_AVATAR;
        }

        return FriendListItemResponse.builder()
                .friendId(friend.getFriendId())
                .userProfileId(target.getUserProfileId())
                .username(target.getUsername())
                .avatarUrl(avatar)
                .status(friend.getStatus())
                .unreadMessages(0L)
                .canAccept(false)
                .pendingOutgoing(true)
                .presenceStatus(presenceStatusForProfile(target.getUserProfileId()))
                .build();
    }

    @Transactional
    public FriendListItemResponse acceptFriendRequest(Long accountId, Long friendId) {
        UserProfile me = getCurrentProfile(accountId);
        Friend relation = friendRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!relation.getAddressee().getUserProfileId().equals(me.getUserProfileId())) {
            throw new RuntimeException("Bạn không có quyền chấp nhận yêu cầu này");
        }

        relation.setStatus("ACCEPTED");
        relation = friendRepository.save(relation);

        UserProfile other = relation.getRequester();
        String avatar = other.getAvatarUrl();
        if (avatar == null || avatar.isBlank()) {
            avatar = DEFAULT_AVATAR;
        }

        return FriendListItemResponse.builder()
                .friendId(relation.getFriendId())
                .userProfileId(other.getUserProfileId())
                .username(other.getUsername())
                .avatarUrl(avatar)
                .status(relation.getStatus())
                .unreadMessages(messageRepository.countUnreadBySender(me.getUserProfileId(), other.getUserProfileId()))
                .canAccept(false)
                .pendingOutgoing(false)
                .presenceStatus(presenceStatusForProfile(other.getUserProfileId()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<MessageItemResponse> getConversation(Long accountId, Long friendUserProfileId) {
        UserProfile me = getCurrentProfile(accountId);
        ensureAcceptedFriend(me.getUserProfileId(), friendUserProfileId);

        return messageRepository.findConversation(me.getUserProfileId(), friendUserProfileId).stream()
                .limit(100)
                .map(message -> MessageItemResponse.builder()
                        .messageId(message.getMessageId())
                        .senderId(message.getSender().getUserProfileId())
                        .receiverId(message.getReceiver().getUserProfileId())
                        .content(message.getContent())
                        .createdAt(message.getCreatedAt().format(DATE_TIME_FORMATTER))
                        .isRead(message.getIsRead())
                        .build())
                .toList();
    }

    @Transactional
    public MessageItemResponse sendMessage(Long accountId, Long toUserProfileId, String content) {
        UserProfile me = getCurrentProfile(accountId);
        if (toUserProfileId == null) {
            throw new RuntimeException("toUserProfileId is required");
        }
        if (content == null || content.isBlank()) {
            throw new RuntimeException("content is required");
        }

        UserProfile receiver = userProfileRepository.findById(toUserProfileId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
        ensureAcceptedFriend(me.getUserProfileId(), receiver.getUserProfileId());

        Message message = new Message();
        message.setSender(me);
        message.setReceiver(receiver);
        message.setContent(content.trim());
        message.setIsRead(false);
        message = messageRepository.save(message);

        return MessageItemResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSender().getUserProfileId())
                .receiverId(message.getReceiver().getUserProfileId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt().format(DATE_TIME_FORMATTER))
                .isRead(message.getIsRead())
                .build();
    }

    @Transactional(readOnly = true)
    public List<MessageItemResponse> getChannelMessages(Long accountId) {
        // Validate current user session
        getCurrentProfile(accountId);

        List<Message> messages = messageRepository.findTop50ByReceiverIsNullOrderByCreatedAtDesc();
        Collections.reverse(messages);
        return messages.stream()
                .map(message -> MessageItemResponse.builder()
                        .messageId(message.getMessageId())
                        .senderId(message.getSender().getUserProfileId())
                        .receiverId(null)
                        .content(message.getContent())
                        .createdAt(message.getCreatedAt().format(DATE_TIME_FORMATTER))
                        .isRead(message.getIsRead())
                        .build())
                .toList();
    }

    @Transactional
    public MessageItemResponse sendChannelMessage(Long accountId, String content) {
        UserProfile me = getCurrentProfile(accountId);
        if (content == null || content.isBlank()) {
            throw new RuntimeException("content is required");
        }

        Message message = new Message();
        message.setSender(me);
        message.setReceiver(null);
        message.setContent(content.trim());
        message.setIsRead(false);
        message = messageRepository.save(message);

        return MessageItemResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSender().getUserProfileId())
                .receiverId(null)
                .content(message.getContent())
                .createdAt(message.getCreatedAt().format(DATE_TIME_FORMATTER))
                .isRead(message.getIsRead())
                .build();
    }

    @Transactional
    public MessageItemResponse sendRoomInviteFromRequest(Long accountId, RoomInviteRequest request) {
        if (request == null || request.getRoomId() == null) {
            throw new RuntimeException("roomId is required");
        }
        Long roomId = request.getRoomId();
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            UserProfile target =
                    userProfileRepository
                            .findByUsername(request.getUsername().trim())
                            .orElseThrow(
                                    () -> new RuntimeException("Không tìm thấy người chơi với username này"));
            return sendRoomInvite(accountId, target.getUserProfileId(), roomId);
        }
        if (request.getToUserProfileId() == null) {
            throw new RuntimeException("Nhập username hoặc chọn bạn trong danh sách");
        }
        return sendRoomInvite(accountId, request.getToUserProfileId(), roomId);
    }

    /**
     * Gửi tin nhắn mời bạn vào phòng (chỉ khi đã kết bạn và bạn đang ở trong phòng chờ).
     */
    @Transactional
    public MessageItemResponse sendRoomInvite(Long accountId, Long toUserProfileId, Long roomId) {
        if (toUserProfileId == null) {
            throw new RuntimeException("toUserProfileId is required");
        }
        if (roomId == null) {
            throw new RuntimeException("roomId is required");
        }
        UserProfile me = getCurrentProfile(accountId);
        ensureAcceptedFriend(me.getUserProfileId(), toUserProfileId);

        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Phòng không tồn tại"));
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new RuntimeException("Chỉ mời được khi phòng đang chờ người chơi");
        }
        roomPlayerRepository
                .findByRoom_RoomIdAndAccount_AccountId(roomId, accountId)
                .orElseThrow(() -> new RuntimeException("Bạn không ở trong phòng này"));

        UserProfile receiver =
                userProfileRepository.findById(toUserProfileId).orElseThrow(() -> new RuntimeException("Người nhận không tồn tại"));
        Long receiverAccountId = receiver.getAccount().getAccountId();
        if (roomPlayerRepository.findByRoom_RoomIdAndAccount_AccountId(roomId, receiverAccountId).isPresent()) {
            throw new RuntimeException("Người này đã ở trong phòng");
        }

        String code = room.getRoomCode() != null ? room.getRoomCode() : "";
        String content =
                "🎮 "
                        + me.getUsername()
                        + " mời bạn vào bàn riêng Monopoly.\nMã phòng: "
                        + code
                        + "\nVào phòng chờ: /private-table?roomId="
                        + roomId
                        + "\n(Hoặc nhập mã phòng ở menu chính.)";
        MessageItemResponse out = sendMessage(accountId, toUserProfileId, content);
        userNotificationRepository.save(
                UserNotification.builder()
                        .recipient(receiver)
                        .sender(me)
                        .type("ROOM_INVITE")
                        .title("Lời mời vào phòng")
                        .body(
                                me.getUsername()
                                        + " mời bạn vào bàn riêng. Mã phòng: "
                                        + code)
                        .roomId(roomId)
                        .roomCode(code)
                        .read(false)
                        .build());
        return out;
    }

    @Transactional(readOnly = true)
    public List<NotificationItemResponse> getNotifications(Long accountId) {
        UserProfile me = getCurrentProfile(accountId);
        return userNotificationRepository
                .findTop50ByRecipient_UserProfileIdOrderByCreatedAtDesc(me.getUserProfileId())
                .stream()
                .map(this::toNotificationDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationUnreadResponse getUnreadNotificationCount(Long accountId) {
        UserProfile me = getCurrentProfile(accountId);
        long n =
                userNotificationRepository.countByRecipient_UserProfileIdAndReadFalse(
                        me.getUserProfileId());
        return NotificationUnreadResponse.builder().unreadCount(n).build();
    }

    @Transactional
    public MessageResponse markNotificationRead(Long accountId, Long notificationId) {
        UserProfile me = getCurrentProfile(accountId);
        UserNotification n =
                userNotificationRepository
                        .findById(notificationId)
                        .orElseThrow(() -> new RuntimeException("Thông báo không tồn tại"));
        if (!n.getRecipient().getUserProfileId().equals(me.getUserProfileId())) {
            throw new RuntimeException("Không có quyền");
        }
        n.setRead(true);
        userNotificationRepository.save(n);
        return MessageResponse.builder().message("OK").build();
    }

    @Transactional
    public MessageResponse markAllNotificationsRead(Long accountId) {
        UserProfile me = getCurrentProfile(accountId);
        List<UserNotification> list =
                userNotificationRepository.findTop50ByRecipient_UserProfileIdOrderByCreatedAtDesc(
                        me.getUserProfileId());
        for (UserNotification n : list) {
            if (!Boolean.TRUE.equals(n.getRead())) {
                n.setRead(true);
                userNotificationRepository.save(n);
            }
        }
        return MessageResponse.builder().message("OK").build();
    }

    private NotificationItemResponse toNotificationDto(UserNotification n) {
        UserProfile s = n.getSender();
        return NotificationItemResponse.builder()
                .notificationId(n.getNotificationId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .read(n.getRead())
                .createdAt(n.getCreatedAt().format(DATE_TIME_FORMATTER))
                .roomId(n.getRoomId())
                .roomCode(n.getRoomCode())
                .senderUserProfileId(s != null ? s.getUserProfileId() : null)
                .senderUsername(s != null ? s.getUsername() : null)
                .build();
    }

    private UserProfile getCurrentProfile(Long accountId) {
        if (accountId == null) {
            throw new RuntimeException("Missing X-Account-Id header");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));
    }

    private void ensureAcceptedFriend(Long meUserProfileId, Long otherUserProfileId) {
        Friend relation = friendRepository.findRelationBetween(meUserProfileId, otherUserProfileId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa kết bạn với người dùng này"));
        if (!"ACCEPTED".equalsIgnoreCase(relation.getStatus())) {
            throw new RuntimeException("Mối quan hệ bạn bè chưa được chấp nhận");
        }
    }
}
