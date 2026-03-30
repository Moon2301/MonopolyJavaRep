package com.game.monopoly;

/**
 * Tiền trong ván — tách khỏi gold tài khoản (shop / profile).
 */
public final class MonopolyGameRules {

    private MonopolyGameRules() {}

    public static final long IN_GAME_STARTING_BALANCE = 1000L;

    /** Bot khó: hơi nhiều tiền hơn để AI cạnh tranh. */
    public static final long IN_GAME_BOT_HARD_EXTRA = 1000L;

    /** Tiền thưởng mỗi lần đi qua / hoàn thành vòng qua ô xuất phát. */
    public static final long PASS_GO_BONUS = 200L;
}
