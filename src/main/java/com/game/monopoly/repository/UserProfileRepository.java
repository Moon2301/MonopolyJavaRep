package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    boolean existsByUsername(String username);
    Optional<UserProfile> findByUsername(String username);
    Optional<UserProfile> findByAccount_AccountId(Long accountId);
    List<UserProfile> findByAccount_AccountIdIn(Collection<Long> accountIds);
}
