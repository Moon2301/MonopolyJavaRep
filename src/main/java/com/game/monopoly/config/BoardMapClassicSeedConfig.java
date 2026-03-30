package com.game.monopoly.config;

import com.game.monopoly.service.BoardClassicMapBootstrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BoardMapClassicSeedConfig {

    private final BoardClassicMapBootstrapService boardClassicMapBootstrapService;

    @Bean
    @Order(50)
    CommandLineRunner seedClassicBoardMapLayout() {
        return args -> {
            try {
                boardClassicMapBootstrapService.ensureClassicBoardIfMissing();
            } catch (Exception e) {
                log.warn("Seed bàn cờ lúc khởi động không chạy xong (sẽ thử lại khi gọi API/gameplay): {}", e.getMessage());
            }
        };
    }
}
