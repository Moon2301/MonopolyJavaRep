package com.game.monopoly.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardCellRequest {

    private String name;
    private String type;
    private Integer price;
    private Integer baseRent;
    private Integer maxHouseLevel;

    private String colorHex;
}