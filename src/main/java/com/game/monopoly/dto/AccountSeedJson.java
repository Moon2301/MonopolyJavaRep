package com.game.monopoly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Bản ghi trong {@code seed/accounts-seed.json} — tài khoản demo (đăng nhập bằng email + mật khẩu).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountSeedJson {
    private String email;
    private String username;
    /** Mật khẩu gốc — chỉ dùng lúc seed, sẽ được mã hóa BCrypt. */
    private String password;
    /** USER hoặc ADMIN */
    private String role;
    private Long gold;
    private Long diamonds;
}
