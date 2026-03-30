package com.game.monopoly.controller;

import com.game.monopoly.dto.BoardCellRequest;
import com.game.monopoly.model.metaData.BoardCell;
import com.game.monopoly.service.BoardCellService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/board-cells")
@RequiredArgsConstructor
public class BoardCellController {

    private final BoardCellService boardCellService;

    // lấy tất cả ô bàn cờ
    @GetMapping
    public List<BoardCell> getAll() {
        return boardCellService.getAllCells();
    }

    // lấy 1 ô
    @GetMapping("/{id}")
    public BoardCell getById(@PathVariable Integer id) {
        return boardCellService.getCellById(id);
    }

    // tạo ô mới
    @PostMapping
    public BoardCell create(@RequestBody BoardCellRequest request) {
        return boardCellService.createCell(request);
    }

    // cập nhật ô
    @PutMapping("/{id}")
    public BoardCell update(
            @PathVariable Integer id,
            @RequestBody BoardCellRequest request
    ) {
        return boardCellService.updateCell(id, request);
    }

    // xóa ô
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        boardCellService.deleteCell(id);
    }
}