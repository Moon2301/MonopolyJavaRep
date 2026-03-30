package com.game.monopoly.service;

import com.game.monopoly.dto.BoardLayoutCellResponse;
import com.game.monopoly.model.metaData.BoardCell;
import com.game.monopoly.model.metaData.MapCell;
import com.game.monopoly.repository.MapCellRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BoardLayoutService {

    private final MapCellRepository mapCellRepository;

    @Transactional(readOnly = true)
    public List<BoardLayoutCellResponse> getLayoutForMap(Integer mapId) {
        if (mapId == null) {
            return List.of();
        }
        List<MapCell> rows = mapCellRepository.findAllByMapIdOrderByPositionWithCell(mapId);
        List<BoardLayoutCellResponse> out = new ArrayList<>(rows.size());
        int index = 0;
        for (MapCell mc : rows) {
            BoardCell c = mc.getBoardCell();
            if (c == null) {
                continue;
            }
            Integer embeddedPos = mc.getId() != null ? mc.getId().getPosition() : null;
            int position = embeddedPos != null ? embeddedPos : index;
            out.add(BoardLayoutCellResponse.builder()
                    .position(position)
                    .cellId(c.getCellId())
                    .name(c.getName())
                    .displayType(toDisplayType(c.getType()))
                    .price(c.getPrice())
                    .priceLabel(formatPriceLabel(c.getPrice()))
                    .colorHex(c.getColorHex())
                    .baseRent(c.getBaseRent())
                    .maxHouseLevel(c.getMaxHouseLevel())
                    .build());
            index++;
        }
        return out;
    }

    private static String formatPriceLabel(Integer price) {
        if (price == null || price <= 0) {
            return "";
        }
        return "$" + price;
    }

    private static String toDisplayType(String dbType) {
        if (dbType == null) {
            return "property";
        }
        String u = dbType.toUpperCase(Locale.ROOT);
        if (u.contains("START") || u.equals("GO")) {
            return "start";
        }
        if (u.contains("CHANCE") || u.contains("COMMUNITY")) {
            return "chance";
        }
        if (u.contains("TAX")) {
            return "tax";
        }
        if (u.contains("JAIL")) {
            return "jail";
        }
        if (u.contains("FREE")) {
            return "free";
        }
        if (u.contains("PROPERTY") || u.contains("RAIL") || u.contains("RR") || u.contains("UTILITY")) {
            return "property";
        }
        return "property";
    }
}
