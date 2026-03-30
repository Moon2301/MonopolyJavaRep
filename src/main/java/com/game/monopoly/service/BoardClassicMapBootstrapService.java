package com.game.monopoly.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.monopoly.dto.BoardClassicSeedJson;
import com.game.monopoly.model.metaData.BoardCell;
import com.game.monopoly.model.metaData.Map;
import com.game.monopoly.model.metaData.MapCell;
import com.game.monopoly.repository.BoardCellRepository;
import com.game.monopoly.repository.MapCellRepository;
import com.game.monopoly.repository.MapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Đảm bảo map classic (40 ô) tồn tại: đọc JSON trên classpath và ghi DB.
 * Gọi lúc khởi động và khi gameplay phát hiện bàn trống.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BoardClassicMapBootstrapService {

    public static final int CLASSIC_MAP_ID = 1;
    public static final int EXPECTED_CELLS = 40;

    private static final String[] SEED_CLASSPATH_LOCATIONS = {
            "static/seed/board-classic.json",
            "seed/board-classic.json"
    };

    private final ObjectMapper objectMapper;
    private final BoardCellRepository boardCellRepository;
    private final MapRepository mapRepository;
    private final MapCellRepository mapCellRepository;

    private final Object seedLock = new Object();

    @Transactional
    public void ensureClassicBoardIfMissing() {
        synchronized (seedLock) {
            if (mapCellRepository.findAllByMapIdOrderByPositionWithCell(CLASSIC_MAP_ID).size() >= EXPECTED_CELLS) {
                return;
            }
            ClassPathResource resource = resolveSeedResource();
            if (resource == null || !resource.exists()) {
                throw new IllegalStateException(
                        "Không tìm thấy file seed bàn cờ trên classpath. Cần: seed/board-classic.json hoặc static/seed/board-classic.json");
            }
            try (InputStream in = resource.getInputStream()) {
                BoardClassicSeedJson[] rows = objectMapper.readValue(in, BoardClassicSeedJson[].class);
                if (rows.length < EXPECTED_CELLS) {
                    throw new IllegalStateException("File seed không đủ " + EXPECTED_CELLS + " ô (có " + rows.length + ")");
                }

                Map gameMap = mapRepository.findById(CLASSIC_MAP_ID).orElseGet(() -> {
                    Map m = new Map();
                    m.setName("Classic Monopoly");
                    m.setMaxLaps(20);
                    m.setIsActive(true);
                    return mapRepository.save(m);
                });

                List<BoardCell> persistedCells = new ArrayList<>(rows.length);
                for (BoardClassicSeedJson row : rows) {
                    BoardCell bc = new BoardCell();
                    bc.setName(row.getName());
                    bc.setType(row.getType());
                    bc.setPrice(row.getPrice());
                    bc.setBaseRent(row.getBaseRent());
                    bc.setMaxHouseLevel(row.getMaxHouseLevel());
                    bc.setColorHex(row.getColorHex());
                    persistedCells.add(boardCellRepository.save(bc));
                }

                List<MapCell> links = new ArrayList<>(persistedCells.size());
                for (int pos = 0; pos < persistedCells.size(); pos++) {
                    links.add(new MapCell(gameMap, pos, persistedCells.get(pos)));
                }
                mapCellRepository.saveAll(links);
                log.info("Đã seed bàn cờ classic: {} BoardCell, {} MapCell (map_id={})",
                        persistedCells.size(), links.size(), gameMap.getMapId());
            } catch (Exception e) {
                log.error("Lỗi khi seed bàn cờ classic", e);
                throw new RuntimeException("Không seed được bàn cờ: " + e.getMessage(), e);
            }
        }
    }

    private ClassPathResource resolveSeedResource() {
        for (String path : SEED_CLASSPATH_LOCATIONS) {
            ClassPathResource r = new ClassPathResource(path);
            if (r.exists()) {
                return r;
            }
        }
        return null;
    }
}
