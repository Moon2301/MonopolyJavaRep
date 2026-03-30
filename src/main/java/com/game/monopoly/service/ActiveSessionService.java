package com.game.monopoly.service;

import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.model.inGameData.Game;
import com.game.monopoly.model.inGameData.Room;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.GameRepository;
import com.game.monopoly.repository.RoomRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActiveSessionService {

    private final UserProfileRepository userProfileRepository;
    private final GameRepository gameRepository;
    private final RoomRepository roomRepository;

    /**
     * @param targetRoomId phòng định vào/join (null khi tạo phòng mới hoặc bắt đầu đấu máy)
     */
    public void assertCanStartNewMultiplayerContext(Long accountId, Long targetRoomId) {
        if (accountId == null) {
            return;
        }
        UserProfile me =
                userProfileRepository
                        .findByAccount_AccountId(accountId)
                        .orElseThrow(() -> new RuntimeException("UserProfile not found"));
        List<Game> playing =
                gameRepository.findPlayingGamesForHumanProfile(
                        me.getUserProfileId(), GameStatus.PLAYING);
        if (playing.isEmpty()) {
            return;
        }
        Game g = playing.get(0);
        if (Boolean.TRUE.equals(g.getSoloVsAi())) {
            throw new RuntimeException(
                    "Bạn đang trong ván đấu máy — hãy kết thúc ván trước khi vào phòng multiplayer.");
        }
        Optional<Room> roomOpt = roomRepository.findByActiveGameId(g.getGameId());
        if (roomOpt.isEmpty()) {
            throw new RuntimeException("Bạn đang trong ván multiplayer — hãy quay lại phòng hoặc kết thúc ván.");
        }
        Room room = roomOpt.get();
        if (targetRoomId != null && room.getRoomId().equals(targetRoomId)) {
            return;
        }
        String code = room.getRoomCode() != null ? room.getRoomCode() : "";
        throw new RuntimeException(
                "Bạn đang trong ván tại phòng khác (mã: "
                        + code
                        + "). Hãy quay lại phòng đó hoặc kết thúc ván trước.");
    }
}
