package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RoomListResponse {
    private List<RoomListItemResponse> items;
    private int page;
    private int size;
    private long totalItems;
}
