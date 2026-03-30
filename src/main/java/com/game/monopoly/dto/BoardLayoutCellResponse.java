package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một ô trên bàn theo thứ tự đi trên map (0 = xuất phát).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardLayoutCellResponse {
    private int position;
    private Integer cellId;
    private String name;
    /** property | chance | tax | jail | free | start — khớp CSS phía client */
    private String displayType;
    private Integer price;
    /** Nhãn hiển thị (vd $60 hoặc rỗng) */
    private String priceLabel;
    private String colorHex;
    private Integer baseRent;
    private Integer maxHouseLevel;
}
