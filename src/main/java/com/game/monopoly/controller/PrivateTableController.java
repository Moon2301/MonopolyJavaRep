package com.game.monopoly.controller;

import com.game.monopoly.dto.PrivateTableResponse;
import com.game.monopoly.service.PrivateTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/private-table")
@RequiredArgsConstructor
public class PrivateTableController {

    private final PrivateTableService privateTableService;

    @GetMapping("/{roomId}")
    public PrivateTableResponse getPrivateTable(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @PathVariable Long roomId
    ) {
        return privateTableService.getPrivateTable(roomId, accountId);
    }
}
