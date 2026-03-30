package com.game.monopoly.controller;

import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.service.HeroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/heroes")
@RequiredArgsConstructor
public class HeroController {

    private final HeroService heroService;

    @GetMapping
    public ResponseEntity<List<Hero>> getAllHeroes() {
        return ResponseEntity.ok(heroService.getAllHeroes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hero> getHeroById(@PathVariable Integer id) {
        return ResponseEntity.ok(heroService.getHeroById(id));
    }

    @PostMapping
    public ResponseEntity<Hero> createHero(@RequestBody Hero hero) {
        return ResponseEntity.ok(heroService.saveHero(hero));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Hero> updateHero(@PathVariable Integer id, @RequestBody Hero hero) {
        hero.setCharacterId(id);
        return ResponseEntity.ok(heroService.saveHero(hero));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHero(@PathVariable Integer id) {
        heroService.deleteHero(id);
        return ResponseEntity.ok().build();
    }
}
