package com.game.monopoly.repository;

import com.game.monopoly.dto.AdminTopupLedgerItem;
import com.game.monopoly.model.metaData.CurrencyLedger;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CurrencyLedgerRepository extends JpaRepository<CurrencyLedger, Long> {
    List<CurrencyLedger> findByUserProfile_UserProfileId(Long userProfileId);
    boolean existsByReasonTypeAndReferenceId(String reasonType, Long referenceId);

    long countByReasonType(String reasonType);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM CurrencyLedger l WHERE l.reasonType = :reason")
    long sumGoldByReasonType(@Param("reason") String reason);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM CurrencyLedger l WHERE l.reasonType = :reason AND l.createdAt >= :from")
    long sumGoldByReasonTypeSince(@Param("reason") String reason, @Param("from") LocalDateTime from);

    @Query(
            """
            SELECT new com.game.monopoly.dto.AdminTopupLedgerItem(
                l.ledgerId,
                l.amount,
                (l.amount * 100),
                l.createdAt,
                l.userProfile.userProfileId,
                l.referenceId)
            FROM CurrencyLedger l
            WHERE l.reasonType = :reason
            ORDER BY l.createdAt DESC
            """)
    List<AdminTopupLedgerItem> findRecentTopups(@Param("reason") String reason, Pageable pageable);
}
