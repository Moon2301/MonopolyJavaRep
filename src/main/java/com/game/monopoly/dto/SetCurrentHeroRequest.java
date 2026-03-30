package com.game.monopoly.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SetCurrentHeroRequest {
    /** {@link com.game.monopoly.model.metaData.Hero#getCharacterId()} */
    private Integer heroId;
}
