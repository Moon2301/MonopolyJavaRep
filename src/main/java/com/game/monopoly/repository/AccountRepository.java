package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String email); // Dùng để đăng nhập
    boolean existsByEmail(String email);       // Dùng để kiểm tra khi đăng ký
}