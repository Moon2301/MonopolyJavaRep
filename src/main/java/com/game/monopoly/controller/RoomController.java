package com.game.monopoly.controller;

import com.game.monopoly.dto.*;
import com.game.monopoly.model.enums.RoomStatus;
import com.game.monopoly.model.enums.RoomVisibility;
import com.game.monopoly.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public RoomListResponse getRooms(
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) RoomVisibility visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return roomService.getRooms(status, visibility, page, size);
    }

    @PostMapping
    public RoomCreateResponse createRoom(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody RoomCreateRequest request
    ) {
        return roomService.createRoom(request, accountId);
    }

    @PostMapping("/join")
    public RoomJoinResponse joinRoom(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody RoomJoinRequest request
    ) {
        return roomService.joinRoom(request, accountId);
    }

    /**
     * Tham gia phòng theo id (dùng khi mở link /private-table?roomId=… — trước đây GET chi tiết báo lỗi vì chưa có
     * RoomPlayer).
     */
    @PostMapping("/{roomId}/join")
    public RoomJoinResponse joinRoomByRoomId(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @PathVariable Long roomId,
            @RequestBody(required = false) RoomJoinPasswordBody body
    ) {
        String password = body != null ? body.getPassword() : null;
        return roomService.joinRoomByRoomId(roomId, accountId, password);
    }

    @GetMapping("/{roomId}")
    public RoomDetailResponse getRoomDetail(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @PathVariable Long roomId
    ) {
        return roomService.getRoomDetail(roomId, accountId);
    }

    @PatchMapping("/{roomId}/settings")
    public MessageResponse updateRoomSettings(
            @PathVariable Long roomId,
            @RequestBody RoomSettingsUpdateRequest request,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return roomService.updateRoomSettings(roomId, request, accountId);
    }

    @PostMapping("/{roomId}/hero")
    public RoomHeroSelectResponse selectHero(
            @PathVariable Long roomId,
            @RequestBody RoomHeroSelectRequest request,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return roomService.selectHero(roomId, request, accountId);
    }

    /** Gán hero mặc định từ hồ sơ nếu chưa chọn (phòng chờ). */
    @PostMapping("/{roomId}/hero/apply-default")
    public RoomHeroSelectResponse applyDefaultHero(
            @PathVariable Long roomId,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return roomService.applyDefaultHeroFromProfile(roomId, accountId);
    }

    @PostMapping("/{roomId}/ready")
    public RoomReadyResponse setReady(
            @PathVariable Long roomId,
            @RequestBody RoomReadyRequest request,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return roomService.setReady(roomId, request, accountId);
    }

    @PostMapping("/{roomId}/start")
    public RoomStartResponse startGame(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @PathVariable Long roomId
    ) {
        return roomService.startGame(roomId, accountId);
    }

    @PostMapping("/{roomId}/leave")
    public MessageResponse leaveRoom(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @PathVariable Long roomId
    ) {
        return roomService.leaveRoom(roomId, accountId);
    }
}
