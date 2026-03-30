package com.game.monopoly.controller;

import com.game.monopoly.model.metaData.Map;
import com.game.monopoly.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/maps")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @GetMapping
    public ResponseEntity<List<Map>> getAllMaps() {
        return ResponseEntity.ok(mapService.getAllMaps());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map> getMapById(@PathVariable Integer id) {
        return ResponseEntity.ok(mapService.getMapById(id));
    }

    @PostMapping
    public ResponseEntity<Map> createMap(@RequestBody Map map) {
        return ResponseEntity.ok(mapService.saveMap(map));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map> updateMap(@PathVariable Integer id, @RequestBody Map map) {
        map.setMapId(id);
        return ResponseEntity.ok(mapService.saveMap(map));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMap(@PathVariable Integer id) {
        mapService.deleteMap(id);
        return ResponseEntity.ok().build();
    }
}
