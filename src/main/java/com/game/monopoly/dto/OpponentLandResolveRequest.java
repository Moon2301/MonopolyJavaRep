package com.game.monopoly.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpponentLandResolveRequest {
    /** true = mua lại với giá {@code buybackPrice} (130% giá niêm yết), false = trả tiền thuê */
    private boolean buyback;
}
