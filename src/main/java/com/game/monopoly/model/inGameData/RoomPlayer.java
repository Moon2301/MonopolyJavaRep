package com.game.monopoly.model.inGameData;

import com.game.monopoly.model.metaData.Account;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "RoomPlayer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_player_account", columnNames = {"room_id", "account_id"}),
                @UniqueConstraint(name = "uk_room_player_slot", columnNames = {"room_id", "slot_index"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_player_id")
    private Long roomPlayerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    @Column(name = "selected_hero_id")
    private Integer selectedHeroId;

    @Column(name = "is_host", nullable = false)
    private Boolean isHost;

    @Column(name = "is_ready", nullable = false)
    private Boolean isReady;

    /** Đang trong ván của phòng (sau khi host bắt đầu game). */
    @Column(name = "is_playing", nullable = false)
    @Builder.Default
    private Boolean isPlaying = false;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    public void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}
