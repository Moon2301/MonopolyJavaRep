package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    @Query("""
           SELECT f FROM Friend f
           WHERE (f.requester.userProfileId = :userA AND f.addressee.userProfileId = :userB)
              OR (f.requester.userProfileId = :userB AND f.addressee.userProfileId = :userA)
           """)
    Optional<Friend> findRelationBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("""
           SELECT f FROM Friend f
           WHERE f.requester.userProfileId = :userId OR f.addressee.userProfileId = :userId
           ORDER BY f.createdAt DESC
           """)
    List<Friend> findAllByUserId(@Param("userId") Long userId);
}
