package com.game.monopoly.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamUpRespondRequest {
    /** true = đồng ý vào team-up, false = từ chối. */
    private boolean accept;
}

