package com.game.monopoly.service;

import com.game.monopoly.config.VNPayConfig;
import com.game.monopoly.model.enums.CurrencyType;
import com.game.monopoly.model.metaData.CurrencyLedger;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.CurrencyLedgerRepository;
import com.game.monopoly.repository.UserProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 💳 Xử lý thanh toán VNPay và cộng Xu cho Player
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final VNPayConfig vnPayConfig;
    private final UserProfileRepository userProfileRepository;
    private final CurrencyLedgerRepository currencyLedgerRepository;

    /**
     * 🚀 Tạo URL thanh toán VNPay
     */
    public String createPaymentUrl(Long amount, Long accountId, HttpServletRequest request) {
        long vnpAmount = amount * 100;
        String vnp_TxnRef = String.valueOf(System.currentTimeMillis());
        String vnp_IpAddr = VNPayConfig.getIpAddress(request);
        String vnp_TmnCode = vnPayConfig.getVnpTmnCode();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(vnpAmount));
        vnp_Params.put("vnp_CurrCode", "VND");

        // Ghi lại AccountId vào OrderInfo để cộng xu khi Return
        vnp_Params.put("vnp_OrderInfo", "NAP_TIEN_" + amount + "_" + accountId);

        vnp_Params.put("vnp_OrderType", "190000"); // Nạp tiền Game
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnpReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        return vnPayConfig.buildPaymentUrl(vnp_Params);
    }

    /**
     * 🔄 Xử lý kết quả trả về từ VNPay và cộng Xu
     */
    /**
     * 🔄 Xử lý kết quả trả về từ VNPay và cộng Xu
     */
    public boolean processVnPayReturn(HttpServletRequest request) {
        Map<String, String[]> requestParams = request.getParameterMap();
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length > 0) {
                fields.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        String vnp_SecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        // Verify Chữ ký
        String signValue = vnPayConfig.hashAllFields(fields);
        System.out.println("VNPay Return - Order: " + fields.get("vnp_TxnRef") + ", Hash Match: " + signValue.equalsIgnoreCase(vnp_SecureHash));

        if (signValue.equalsIgnoreCase(vnp_SecureHash)) {
            String responseCode = fields.get("vnp_ResponseCode");
            String orderInfo = fields.get("vnp_OrderInfo");
            String transactionNo = fields.get("vnp_TransactionNo");

            if ("00".equals(responseCode)) {
                return handleSuccessfulTopup(orderInfo, transactionNo);
            } else {
                System.out.println("VNPay Return - Failed Code: " + responseCode);
            }
        }
        return false;
    }

    @Transactional
    public boolean handleSuccessfulTopup(String orderInfo, String transactionNo) {
        try {
            System.out.println("Processing Topup: " + orderInfo + " (VNP: " + transactionNo + ")");

            // Định dạng: NAP_TIEN_{amount}_{accountId}
            String[] parts = orderInfo.split("_");
            if (parts.length < 4) {
                System.err.println("Invalid OrderInfo format: " + orderInfo);
                return false;
            }

            long amountVnd = Long.parseLong(parts[2]);
            long accountId = Long.parseLong(parts[3]);

            UserProfile profile = userProfileRepository.findByAccount_AccountId(accountId).orElse(null);
            if (profile == null) {
                System.err.println("UserProfile not found for AccountId: " + accountId);
                return false;
            }

            // Chống trùng lặp (Idempotency)
            boolean alreadyProcessed = currencyLedgerRepository.existsByReasonTypeAndReferenceId("VNPAY_TOPUP", Long.parseLong(transactionNo));
            if (alreadyProcessed) {
                System.out.println("Transaction " + transactionNo + " already processed.");
                return true;
            }

            // Quy đổi: 50,000đ = 500 Xu (Gold)
            long goldAmount = amountVnd / 100;
            long currentGold = (profile.getGold() != null) ? profile.getGold() : 0L;
            long newBalance = currentGold + goldAmount;
            profile.setGold(newBalance);
            userProfileRepository.save(profile);

            // Ghi nhận lịch sử giao dịch
            CurrencyLedger ledger = new CurrencyLedger();
            ledger.setUserProfile(profile);
            ledger.setCurrencyType(CurrencyType.GOLD);
            ledger.setAmount(goldAmount);
            ledger.setBalanceAfter(newBalance);
            ledger.setReasonType("VNPAY_TOPUP");
            ledger.setReferenceId(Long.parseLong(transactionNo));
            currencyLedgerRepository.save(ledger);

            System.out.println("Successfully credited " + goldAmount + " Xu to Account " + accountId);
            return true;
        } catch (Exception e) {
            System.err.println("Error processing topup: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}