package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findTop50ByRecipient_UserProfileIdOrderByCreatedAtDesc(Long recipientUserProfileId);

    long countByRecipient_UserProfileIdAndReadFalse(Long recipientUserProfileId);
}
