package com.game.monopoly.controller;

import com.game.monopoly.dto.BoardLayoutCellResponse;
import com.game.monopoly.service.BoardClassicMapBootstrapService;
import com.game.monopoly.service.BoardLayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/board/maps")
@RequiredArgsConstructor
public class BoardLayoutController {

    private final BoardLayoutService boardLayoutService;
    private final BoardClassicMapBootstrapService boardClassicMapBootstrapService;

    @GetMapping("/{mapId}/layout")
    public List<BoardLayoutCellResponse> getMapLayout(@PathVariable Integer mapId) {
        if (Integer.valueOf(BoardClassicMapBootstrapService.CLASSIC_MAP_ID).equals(mapId)) {
            boardClassicMapBootstrapService.ensureClassicBoardIfMissing();
        }
        return boardLayoutService.getLayoutForMap(mapId);
    }
}
