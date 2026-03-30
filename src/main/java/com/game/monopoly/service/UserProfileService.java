package com.game.monopoly.service;

import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    public List<UserProfile> getAll() {
        return repository.findAll();
    }

    public UserProfile getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));
    }

    public UserProfile create(UserProfile userProfile) {
        return repository.save(userProfile);
    }

    public UserProfile update(Long id, UserProfile updated) {
        UserProfile existing = getById(id);

        existing.setUsername(updated.getUsername());
        existing.setAvatarUrl(updated.getAvatarUrl());
        existing.setGold(updated.getGold());
        existing.setDiamonds(updated.getDiamonds());
        existing.setRankPoints(updated.getRankPoints());
        existing.setRankTier(updated.getRankTier());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}