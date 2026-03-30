package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "UserProfile")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_profile_id")
    private Long userProfileId;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private Long gold = 0L;

    @Column(nullable = false)
    private Long diamonds = 0L;

    @Column(name = "rank_points", nullable = false)
    private Integer rankPoints = 1000;

    @Column(name = "rank_tier", nullable = false, length = 50)
    private String rankTier = "BRONZE";

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * Hero hiện tại / mặc định — dùng cho hồ sơ, phòng chờ và tự gán khi chưa chọn (FK Hero.character_id).
     * Cột DB: {@code default_character_id} (giữ tương thích).
     */
    @Column(name = "default_character_id")
    private Integer currentHeroId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}