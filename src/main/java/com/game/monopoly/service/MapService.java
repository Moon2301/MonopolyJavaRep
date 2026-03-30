package com.game.monopoly.service;

import com.game.monopoly.model.metaData.Map;
import com.game.monopoly.repository.MapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapService {

    private final MapRepository mapRepository;

    public List<Map> getAllMaps() {
        return mapRepository.findAll();
    }

    public Map getMapById(Integer id) {
        return mapRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Map not found"));
    }

    public Map saveMap(Map map) {
        return mapRepository.save(map);
    }

    public void deleteMap(Integer id) {
        mapRepository.deleteById(id);
    }
}
