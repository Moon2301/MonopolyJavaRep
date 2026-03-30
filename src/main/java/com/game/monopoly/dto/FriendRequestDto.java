package com.game.monopoly.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendRequestDto {
    /** Username người muốn kết bạn — JSON có thể dùng {@code friendUsername} (tương thích cũ). */
    @JsonAlias("friendUsername")
    private String username;
}
