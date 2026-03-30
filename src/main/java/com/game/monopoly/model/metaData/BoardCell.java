package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "BoardCell")
@Getter
@Setter
public class BoardCell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cell_id")
    private Integer cellId;

    private String name;

    private String type;

    private Integer price;

    @Column(name = "base_rent")
    private Integer baseRent;

    @Column(name = "max_house_level")
    private Integer maxHouseLevel;

    /** Màu dải ô trên UI (hex), có thể null */
    @Column(name = "color_hex", length = 16)
    private String colorHex;
}