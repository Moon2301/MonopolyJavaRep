package com.game.monopoly.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChangeDisplayNameRequest {
    private String newUsername;
}
