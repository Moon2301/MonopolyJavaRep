package com.game.monopoly.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamUpInviteRequest {
    /** true = gửi lời mời team-up, false = bỏ qua. */
    private boolean invite;
}

