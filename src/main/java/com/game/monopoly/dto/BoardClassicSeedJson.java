package com.game.monopoly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoardClassicSeedJson {
    private String name;
    private String type;
    private Integer price;
    private Integer baseRent;
    private Integer maxHouseLevel;
    private String colorHex;
}
