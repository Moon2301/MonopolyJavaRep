package com.game.monopoly.dto;

import java.time.LocalDateTime;

/**
 * Dòng giao dịch nạp tiền VNPay cho admin (amount trong ledger là Xu Vàng đã cộng; VND = amount × 100).
 */
public record AdminTopupLedgerItem(
        Long ledgerId,
        Long goldCredited,
        Long vndEquivalent,
        LocalDateTime createdAt,
        Long userProfileId,
        Long referenceId
) {
}
