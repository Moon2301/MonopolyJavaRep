package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.UserOwnedHero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserOwnedHeroRepository extends JpaRepository<UserOwnedHero, Long> {
    List<UserOwnedHero> findByUserProfile_UserProfileId(Long userProfileId);
    boolean existsByUserProfile_UserProfileIdAndHero_CharacterId(Long userProfileId, Integer characterId);
}
