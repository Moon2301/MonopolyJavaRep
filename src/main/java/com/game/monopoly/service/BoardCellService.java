package com.game.monopoly.service;

import com.game.monopoly.dto.BoardCellRequest;
import com.game.monopoly.model.metaData.BoardCell;
import com.game.monopoly.repository.BoardCellRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardCellService {

    private final BoardCellRepository boardCellRepository;

    public List<BoardCell> getAllCells() {
        return boardCellRepository.findAll();
    }

    public BoardCell getCellById(Integer id) {
        return boardCellRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("BoardCell not found"));
    }

    public BoardCell createCell(BoardCellRequest request) {

        BoardCell cell = new BoardCell();
        cell.setName(request.getName());
        cell.setType(request.getType());
        cell.setPrice(request.getPrice());
        cell.setBaseRent(request.getBaseRent());
        cell.setMaxHouseLevel(request.getMaxHouseLevel());
        cell.setColorHex(request.getColorHex());

        return boardCellRepository.save(cell);
    }

    public BoardCell updateCell(Integer id, BoardCellRequest request) {

        BoardCell cell = getCellById(id);

        cell.setName(request.getName());
        cell.setType(request.getType());
        cell.setPrice(request.getPrice());
        cell.setBaseRent(request.getBaseRent());
        cell.setMaxHouseLevel(request.getMaxHouseLevel());
        cell.setColorHex(request.getColorHex());

        return boardCellRepository.save(cell);
    }

    public void deleteCell(Integer id) {

        BoardCell cell = getCellById(id);
        boardCellRepository.delete(cell);
    }
}