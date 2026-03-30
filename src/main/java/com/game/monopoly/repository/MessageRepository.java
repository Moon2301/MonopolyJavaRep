package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
           SELECT m FROM Message m
           WHERE (m.sender.userProfileId = :userA AND m.receiver.userProfileId = :userB)
              OR (m.sender.userProfileId = :userB AND m.receiver.userProfileId = :userA)
           ORDER BY m.createdAt DESC
           """)
    List<Message> findConversation(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("""
           SELECT COUNT(m) FROM Message m
           WHERE m.receiver.userProfileId = :receiverId
             AND m.sender.userProfileId = :senderId
             AND m.isRead = false
           """)
    Long countUnreadBySender(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    List<Message> findTop50ByReceiverIsNullOrderByCreatedAtDesc();
}
