package com.game.monopoly.controller;

import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.UserProfileRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/userProfiles")
public class UserProfileController {

    private final UserProfileRepository userProfileRepository;

    public UserProfileController(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    // Lấy tất cả user profile
    @GetMapping
    public List<UserProfile> getAll() {
        return userProfileRepository.findAll();
    }

    // Lấy theo ID
    @GetMapping("/{id}")
    public UserProfile getById(@PathVariable Long id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));
    }

    // Tạo mới
    @PostMapping
    public UserProfile create(@RequestBody UserProfile userProfile) {
        return userProfileRepository.save(userProfile);
    }

    // Cập nhật
    @PutMapping("/{id}")
    public UserProfile update(@PathVariable Long id, @RequestBody UserProfile updated) {

        UserProfile user = userProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));

        user.setUsername(updated.getUsername());
        user.setAvatarUrl(updated.getAvatarUrl());
        user.setGold(updated.getGold());
        user.setDiamonds(updated.getDiamonds());
        user.setRankPoints(updated.getRankPoints());
        user.setRankTier(updated.getRankTier());

        return userProfileRepository.save(user);
    }

    // Xóa
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userProfileRepository.deleteById(id);
    }
}