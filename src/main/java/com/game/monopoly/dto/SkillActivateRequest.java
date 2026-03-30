package com.game.monopoly.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SkillActivateRequest {
    /** {@link com.game.monopoly.model.metaData.Skill#getSkillId()} */
    private Integer skillId;

    /** Ô mục tiêu (BoardCell.cellId) — skill cần chọn đất (vd Xóa Quyền Sở Hữu, Điều Khoản Vàng). */
    private Integer targetCellId;

    /** Thần xúc xắc (DUEL_DICE): mặt từng viên 1–6. */
    private Integer dice1;
    private Integer dice2;
}
