package com.game.monopoly.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StartBotGameRequest {
    /** Tất cả bot cùng độ khó (khi không dùng {@link #botSlots}). */
    private String difficulty;
    /** Hero người chơi. */
    private Integer heroId;
    /** Số bot (1–3). Mặc định 1 — chỉ khi không gửi {@link #botSlots}. */
    private Integer botCount;
    /**
     * Từng bot: độ khó + nhân vật. Tối đa 3 phần tử. Nếu có danh sách này thì ưu tiên hơn difficulty/botCount.
     */
    private List<BotSlotRequest> botSlots;
}
