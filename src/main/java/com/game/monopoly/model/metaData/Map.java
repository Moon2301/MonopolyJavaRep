package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Map")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Map {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "map_id")
    private Integer mapId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "max_laps")
    private Integer maxLaps = 20;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
