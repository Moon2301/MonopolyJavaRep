package com.game.monopoly.model.inGameData;

import com.game.monopoly.model.enums.RoomMode;
import com.game.monopoly.model.enums.RoomStatus;
import com.game.monopoly.model.enums.RoomVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "room_code", nullable = false, unique = true, length = 20)
    private String roomCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "host_player_id", nullable = false)
    private Long hostPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomVisibility visibility;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomMode mode;

    @Column(name = "max_players", nullable = false)
    private Integer maxPlayers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    /** Ván đang chơi (sau khi chủ bấm Bắt đầu) — để client trong phòng poll và chuyển sang bàn. */
    @Column(name = "active_game_id")
    private Long activeGameId;

    /** Đã bắt đầu ván (bổ sung cho {@link #status} IN_GAME). */
    @Column(name = "is_started", nullable = false)
    @Builder.Default
    private Boolean isStarted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
