package com.game.monopoly.service;

import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.repository.HeroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HeroService {

    private final HeroRepository heroRepository;

    public List<Hero> getAllHeroes() {
        return heroRepository.findAll();
    }

    public Hero getHeroById(Integer id) {
        return heroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hero not found"));
    }

    public Hero saveHero(Hero hero) {
        return heroRepository.save(hero);
    }

    public void deleteHero(Integer id) {
        heroRepository.deleteById(id);
    }
}
