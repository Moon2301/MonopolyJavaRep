package com.game.monopoly.config;

import com.game.monopoly.service.PresenceRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@RequiredArgsConstructor
public class AccountPresenceFilter extends OncePerRequestFilter {

    private final PresenceRegistry presenceRegistry;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String h = request.getHeader("X-Account-Id");
        if (h != null && !h.isBlank()) {
            try {
                presenceRegistry.touch(Long.parseLong(h.trim()));
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        filterChain.doFilter(request, response);
    }
}
