package com.game.monopoly.service;

import com.game.monopoly.dto.*;
import com.game.monopoly.MonopolyGameRules;
import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.model.enums.RoomMode;
import com.game.monopoly.model.enums.RoomStatus;
import com.game.monopoly.model.enums.RoomVisibility;
import com.game.monopoly.model.inGameData.Game;
import com.game.monopoly.model.inGameData.GamePlayer;
import com.game.monopoly.model.inGameData.Room;
import com.game.monopoly.model.inGameData.RoomPlayer;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.GamePlayerRepository;
import com.game.monopoly.repository.GameRepository;
import com.game.monopoly.repository.HeroRepository;
import com.game.monopoly.repository.RoomPlayerRepository;
import com.game.monopoly.repository.RoomRepository;
import com.game.monopoly.repository.UserProfileRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final String DEFAULT_AVATAR = "/images/avatar-default.png";

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final UserProfileRepository userProfileRepository;
    private final AccountRepository accountRepository;
    private final HeroRepository heroRepository;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final PasswordEncoder passwordEncoder;
    private final HeroOwnershipService heroOwnershipService;
    private final SocialService socialService;
    private final ActiveSessionService activeSessionService;

    @Transactional(readOnly = true)
    public RoomListResponse getRooms(RoomStatus status, RoomVisibility visibility, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Specification<Room> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (visibility != null) {
                predicates.add(cb.equal(root.get("visibility"), visibility));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Room> roomPage = roomRepository.findAll(specification, pageable);
        List<Room> rooms = roomPage.getContent();
        Map<Long, String> hostNames = loadHostNames(rooms);
        Map<Long, Integer> playerCounts = loadPlayerCounts(rooms);

        List<RoomListItemResponse> items = rooms.stream()
                .map(room -> RoomListItemResponse.builder()
                        .roomId(room.getRoomId())
                        .roomCode(room.getRoomCode())
                        .name(room.getName())
                        .hostName(hostNames.getOrDefault(room.getHostPlayerId(), "Unknown"))
                        .mode(room.getMode())
                        .currentPlayers(playerCounts.getOrDefault(room.getRoomId(), 0))
                        .maxPlayers(room.getMaxPlayers())
                        .visibility(room.getVisibility())
                        .status(room.getStatus())
                        .build())
                .toList();

        return RoomListResponse.builder()
                .items(items)
                .page(roomPage.getNumber())
                .size(roomPage.getSize())
                .totalItems(roomPage.getTotalElements())
                .build();
    }

    @Transactional
    public RoomCreateResponse createRoom(RoomCreateRequest request, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        UserProfile profile = getProfile(account.getAccountId());

        RoomVisibility visibility = request.getVisibility() != null ? request.getVisibility() : RoomVisibility.PUBLIC;
        RoomMode mode = request.getMode() != null ? request.getMode() : RoomMode.CLASSIC;
        int maxPlayers = normalizeMaxPlayers(request.getMaxPlayers());

        validatePasswordForVisibility(visibility, request.getPassword());

        Room room = Room.builder()
                .roomCode(generateUniqueRoomCode())
                .name(normalizeRoomName(request.getName(), profile.getUsername()))
                .hostPlayerId(account.getAccountId())
                .visibility(visibility)
                .passwordHash(visibility == RoomVisibility.PRIVATE ? passwordEncoder.encode(request.getPassword()) : null)
                .mode(mode)
                .maxPlayers(maxPlayers)
                .status(RoomStatus.WAITING)
                .build();
        room = roomRepository.save(room);

        roomPlayerRepository.save(RoomPlayer.builder()
                .room(room)
                .account(account)
                .slotIndex(1)
                .isHost(true)
                .isReady(false)
                .build());

        return RoomCreateResponse.builder()
                .roomId(room.getRoomId())
                .roomCode(room.getRoomCode())
                .redirectUrl("/private-table?roomId=" + room.getRoomId())
                .build();
    }

    @Transactional
    public RoomJoinResponse joinRoom(RoomJoinRequest request, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        Room room = roomRepository.findByRoomCode(normalizeRoomCode(request.getRoomCode()))
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new RuntimeException("Room is not joinable");
        }

        if (roomPlayerRepository.findByRoom_RoomIdAndAccount_AccountId(room.getRoomId(), account.getAccountId()).isPresent()) {
            return RoomJoinResponse.builder()
                    .roomId(room.getRoomId())
                    .redirectUrl("/private-table?roomId=" + room.getRoomId())
                    .build();
        }

        activeSessionService.assertCanStartNewMultiplayerContext(accountId, room.getRoomId());

        if (room.getVisibility() == RoomVisibility.PRIVATE) {
            if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), room.getPasswordHash())) {
                throw new RuntimeException("Invalid room password");
            }
        }

        List<RoomPlayer> players = roomPlayerRepository.findByRoom_RoomIdOrderBySlotIndexAsc(room.getRoomId());
        if (players.size() >= room.getMaxPlayers()) {
            throw new RuntimeException("Room is full");
        }

        roomPlayerRepository.save(RoomPlayer.builder()
                .room(room)
                .account(account)
                .slotIndex(findNextAvailableSlot(players, room.getMaxPlayers()))
                .isHost(false)
                .isReady(false)
                .build());

        return RoomJoinResponse.builder()
                .roomId(room.getRoomId())
                .redirectUrl("/private-table?roomId=" + room.getRoomId())
                .build();
    }

    /** Giống {@link #joinRoom} nhưng tìm phòng theo {@code roomId} — cho link mời từ thông báo. */
    @Transactional
    public RoomJoinResponse joinRoomByRoomId(Long roomId, Long accountId, String password) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new RuntimeException("Room is not joinable");
        }

        if (roomPlayerRepository.findByRoom_RoomIdAndAccount_AccountId(roomId, account.getAccountId()).isPresent()) {
            return RoomJoinResponse.builder()
                    .roomId(room.getRoomId())
                    .redirectUrl("/private-table?roomId=" + room.getRoomId())
                    .build();
        }

        activeSessionService.assertCanStartNewMultiplayerContext(accountId, roomId);

        if (room.getVisibility() == RoomVisibility.PRIVATE) {
            if (password == null || !passwordEncoder.matches(password, room.getPasswordHash())) {
                throw new RuntimeException("Invalid room password");
            }
        }

        List<RoomPlayer> players = roomPlayerRepository.findByRoom_RoomIdOrderBySlotIndexAsc(roomId);
        if (players.size() >= room.getMaxPlayers()) {
            throw new RuntimeException("Room is full");
        }

        roomPlayerRepository.save(RoomPlayer.builder()
                .room(room)
                .account(account)
                .slotIndex(findNextAvailableSlot(players, room.getMaxPlayers()))
                .isHost(false)
                .isReady(false)
                .build());

        return RoomJoinResponse.builder()
                .roomId(room.getRoomId())
                .redirectUrl("/private-table?roomId=" + room.getRoomId())
                .build();
    }

    @Transactional(readOnly = true)
    public RoomDetailResponse getRoomDetail(Long roomId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        Room room = getRoom(roomId);
        List<RoomPlayer> roomPlayers = roomPlayerRepository.findByRoom_RoomIdOrderBySlotIndexAsc(roomId);
        RoomPlayer currentRoomPlayer = roomPlayers.stream()
                .filter(player -> Objects.equals(player.getAccount().getAccountId(), account.getAccountId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Player is not in this room"));

        Map<Long, UserProfile> profilesByAccountId = loadProfilesByAccountId(roomPlayers);
        Map<Integer, Hero> heroesById = loadHeroesById(roomPlayers);

        UserProfile currentProfile = profilesByAccountId.get(account.getAccountId());
        if (currentProfile == null) {
            throw new RuntimeException("UserProfile not found");
        }

        String currentAvatarUrl = currentProfile.getAvatarUrl();
        if (currentAvatarUrl == null || currentAvatarUrl.isBlank()) {
            // Important: return null so frontend can fallback to initial letters
            currentAvatarUrl = null;
        }

        java.util.Set<Long> profileIdsInRoom =
                roomPlayers.stream()
                        .map(rp -> profilesByAccountId.get(rp.getAccount().getAccountId()))
                        .filter(Objects::nonNull)
                        .map(UserProfile::getUserProfileId)
                        .collect(Collectors.toSet());

        List<RoomDetailResponse.InviteFriendDto> inviteFriends =
                socialService.getFriends(account.getAccountId()).stream()
                        .filter(f -> "ACCEPTED".equalsIgnoreCase(f.getStatus()))
                        .filter(f -> !profileIdsInRoom.contains(f.getUserProfileId()))
                        .filter(f -> "ONLINE".equals(f.getPresenceStatus()))
                        .map(
                                f ->
                                        RoomDetailResponse.InviteFriendDto.builder()
                                                .userProfileId(f.getUserProfileId())
                                                .username(f.getUsername())
                                                .avatarUrl(f.getAvatarUrl())
                                                .presenceStatus(f.getPresenceStatus())
                                                .build())
                        .toList();

        return RoomDetailResponse.builder()
                .room(RoomDetailResponse.RoomDto.builder()
                        .roomId(room.getRoomId())
                        .roomCode(room.getRoomCode())
                        .name(room.getName())
                        .visibility(room.getVisibility())
                        .mode(room.getMode())
                        .maxPlayers(room.getMaxPlayers())
                        .status(room.getStatus())
                        .hostPlayerId(room.getHostPlayerId())
                        .activeGameId(room.getActiveGameId())
                        .isStarted(room.getIsStarted())
                        .build())
                .currentPlayer(RoomDetailResponse.CurrentPlayerDto.builder()
                        .playerId(account.getAccountId())
                        .username(currentProfile.getUsername())
                        .avatarUrl(currentAvatarUrl)
                        .coins(currentProfile.getGold())
                        .tickets(currentProfile.getDiamonds())
                        .selectedHero(toHeroDto(currentRoomPlayer.getSelectedHeroId(), heroesById))
                        .build())
                .players(roomPlayers.stream()
                        .map(player -> toPlayerDto(player, profilesByAccountId, heroesById))
                        .toList())
                .inviteFriends(inviteFriends)
                .build();
    }

    @Transactional
    public MessageResponse updateRoomSettings(Long roomId, RoomSettingsUpdateRequest request, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        Room room = getRoom(roomId);
        RoomPlayer roomPlayer = getRoomPlayer(roomId, account.getAccountId());
        requireHost(roomPlayer);

        if (request.getVisibility() != null) {
            room.setVisibility(request.getVisibility());
        }
        if (request.getMode() != null) {
            room.setMode(request.getMode());
        }
        if (request.getMaxPlayers() != null) {
            int normalizedMaxPlayers = normalizeMaxPlayers(request.getMaxPlayers());
            long currentPlayers = roomPlayerRepository.countByRoom_RoomId(roomId);
            if (normalizedMaxPlayers < currentPlayers) {
                throw new RuntimeException("Max players cannot be less than current players");
            }
            room.setMaxPlayers(normalizedMaxPlayers);
        }
        if (room.getVisibility() == RoomVisibility.PRIVATE) {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new RuntimeException("Password is required for private room");
            }
            room.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        } else {
            room.setPasswordHash(null);
        }

        roomRepository.save(room);
        return MessageResponse.builder()
                .message("Room settings updated")
                .build();
    }

    @Transactional
    public RoomHeroSelectResponse selectHero(Long roomId, RoomHeroSelectRequest request, Long accountId) {
        if (request.getHeroId() == null) {
            throw new RuntimeException("Hero is required");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        RoomPlayer roomPlayer = getRoomPlayer(roomId, account.getAccountId());
        Hero hero = heroRepository.findById(request.getHeroId())
                .orElseThrow(() -> new RuntimeException("Hero not found"));
        if (!heroOwnershipService.isHeroOwned(accountId, hero.getCharacterId())) {
            throw new RuntimeException("Bạn chỉ được chọn nhân vật đã sở hữu");
        }

        roomPlayer.setSelectedHeroId(hero.getCharacterId());
        roomPlayerRepository.save(roomPlayer);

        return RoomHeroSelectResponse.builder()
                .playerId(account.getAccountId())
                .selectedHeroId(hero.getCharacterId())
                .selectedHeroName(hero.getName())
                .build();
    }

    /**
     * Gán hero mặc định từ hồ sơ (current hero) hoặc hero sở hữu đầu tiên nếu chưa chọn — dùng từ phòng chờ.
     */
    @Transactional
    public RoomHeroSelectResponse applyDefaultHeroFromProfile(Long roomId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        RoomPlayer rp = getRoomPlayer(roomId, account.getAccountId());
        if (rp.getSelectedHeroId() != null
                && heroOwnershipService.isHeroOwned(accountId, rp.getSelectedHeroId())) {
            Hero h = heroRepository.findById(rp.getSelectedHeroId()).orElse(null);
            return RoomHeroSelectResponse.builder()
                    .playerId(account.getAccountId())
                    .selectedHeroId(rp.getSelectedHeroId())
                    .selectedHeroName(h != null ? h.getName() : null)
                    .build();
        }
        UserProfile profile = getProfile(account.getAccountId());
        Integer resolved = resolveMandatoryHeroIdForAccount(profile, accountId);
        rp.setSelectedHeroId(resolved);
        roomPlayerRepository.save(rp);
        Hero hero = heroRepository.findById(resolved).orElseThrow();
        return RoomHeroSelectResponse.builder()
                .playerId(account.getAccountId())
                .selectedHeroId(resolved)
                .selectedHeroName(hero.getName())
                .build();
    }

    private Integer resolveMandatoryHeroIdForAccount(UserProfile profile, Long accountId) {
        Integer cur = profile.getCurrentHeroId();
        if (cur != null && heroOwnershipService.isHeroOwned(accountId, cur)) {
            return cur;
        }
        Set<Integer> owned = heroOwnershipService.getOwnedHeroIds(accountId);
        if (owned.isEmpty()) {
            throw new RuntimeException(
                    "Bạn chưa có nhân vật — hãy đặt hero mặc định trong Hồ sơ hoặc mở khóa trong Cửa hàng");
        }
        return owned.stream().min(Integer::compareTo).orElseThrow();
    }

    private void ensureSelectedHeroForStart(RoomPlayer rp, UserProfile profile, Long accountId) {
        if (rp.getSelectedHeroId() != null
                && heroOwnershipService.isHeroOwned(accountId, rp.getSelectedHeroId())) {
            return;
        }
        Integer resolved = resolveMandatoryHeroIdForAccount(profile, accountId);
        rp.setSelectedHeroId(resolved);
        roomPlayerRepository.save(rp);
    }

    @Transactional
    public RoomReadyResponse setReady(Long roomId, RoomReadyRequest request, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        RoomPlayer roomPlayer = getRoomPlayer(roomId, account.getAccountId());
        boolean ready = Boolean.TRUE.equals(request.getReady());
        roomPlayer.setIsReady(ready);
        roomPlayerRepository.save(roomPlayer);

        return RoomReadyResponse.builder()
                .playerId(account.getAccountId())
                .ready(ready)
                .build();
    }

    @Transactional
    public RoomStartResponse startGame(Long roomId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        Room room = getRoom(roomId);
        RoomPlayer roomPlayer = getRoomPlayer(roomId, account.getAccountId());
        requireHost(roomPlayer);

        List<RoomPlayer> roomPlayers = roomPlayerRepository.findByRoom_RoomIdOrderBySlotIndexAsc(roomId);
        if (roomPlayers.size() < 2) {
            throw new RuntimeException("Cần ít nhất 2 người chơi để bắt đầu");
        }
        boolean guestNotReady =
                roomPlayers.stream()
                        .anyMatch(
                                player ->
                                        !Boolean.TRUE.equals(player.getIsHost())
                                                && !Boolean.TRUE.equals(player.getIsReady()));
        if (guestNotReady) {
            throw new RuntimeException("Tất cả người chơi (trừ chủ phòng) phải bấm sẵn sàng");
        }

        room.setStatus(RoomStatus.STARTING);
        roomRepository.save(room);

        Map<Long, UserProfile> profilesByAccountId = loadProfilesByAccountId(roomPlayers);

        for (RoomPlayer player : roomPlayers) {
            UserProfile profile = profilesByAccountId.get(player.getAccount().getAccountId());
            if (profile == null) {
                throw new RuntimeException("UserProfile not found for room player");
            }
            ensureSelectedHeroForStart(player, profile, player.getAccount().getAccountId());
        }

        Game game = Game.builder()
                .mapId(1)
                .createdBy(account.getAccountId())
                .status(GameStatus.PLAYING)
                .maxPlayers(room.getMaxPlayers())
                .currentTurn(1)
                .currentPlayerOrder(1)
                .turnState("WAIT_ROLL")
                .version(1)
                .eliminationSequence(0)
                .soloVsAi(false)
                .createdAt(LocalDateTime.now())
                .startedAt(LocalDateTime.now())
                .humanTurnStartedAt(LocalDateTime.now())
                .build();
        game = gameRepository.save(game);

        List<GamePlayer> gamePlayers = new ArrayList<>();
        for (RoomPlayer player : roomPlayers) {
            UserProfile profile = profilesByAccountId.get(player.getAccount().getAccountId());
            if (profile == null) {
                throw new RuntimeException("UserProfile not found for room player");
            }
            gamePlayers.add(GamePlayer.builder()
                    .gameId(game.getGameId())
                    .userProfileId(profile.getUserProfileId())
                    .characterId(player.getSelectedHeroId())
                    .turnOrder(player.getSlotIndex())
                    .balance(MonopolyGameRules.IN_GAME_STARTING_BALANCE)
                    .position(0)
                    .isBankrupt(false)
                    .isBot(false)
                    .build());
        }
        gamePlayerRepository.saveAll(gamePlayers);

        room.setStatus(RoomStatus.IN_GAME);
        room.setActiveGameId(game.getGameId());
        room.setIsStarted(true);
        roomRepository.save(room);
        for (RoomPlayer rp : roomPlayers) {
            rp.setIsPlaying(true);
            roomPlayerRepository.save(rp);
        }

        return RoomStartResponse.builder()
                .gameId(game.getGameId())
                .redirectUrl("/game-board?gameId=" + game.getGameId() + "&roomId=" + roomId)
                .build();
    }

    @Transactional
    public MessageResponse leaveRoom(Long roomId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        Room room = getRoom(roomId);
        RoomPlayer leavingPlayer = getRoomPlayer(roomId, account.getAccountId());

        roomPlayerRepository.delete(leavingPlayer);

        List<RoomPlayer> remainingPlayers = roomPlayerRepository.findByRoom_RoomIdOrderBySlotIndexAsc(roomId);
        if (remainingPlayers.isEmpty()) {
            roomRepository.delete(room);
            return MessageResponse.builder()
                    .message("Left room")
                    .build();
        }

        if (Boolean.TRUE.equals(leavingPlayer.getIsHost())) {
            RoomPlayer newHost = remainingPlayers.get(0);
            remainingPlayers.forEach(player -> player.setIsHost(false));
            newHost.setIsHost(true);
            roomPlayerRepository.saveAll(remainingPlayers);
            room.setHostPlayerId(newHost.getAccount().getAccountId());
            roomRepository.save(room);
        }

        return MessageResponse.builder()
                .message("Left room")
                .build();
    }

    private Map<Long, String> loadHostNames(List<Room> rooms) {
        if (rooms.isEmpty()) {
            return Map.of();
        }

        List<Long> hostAccountIds = rooms.stream()
                .map(Room::getHostPlayerId)
                .distinct()
                .toList();

        return userProfileRepository.findByAccount_AccountIdIn(hostAccountIds).stream()
                .collect(Collectors.toMap(profile -> profile.getAccount().getAccountId(), UserProfile::getUsername));
    }

    private Map<Long, Integer> loadPlayerCounts(List<Room> rooms) {
        Map<Long, Integer> playerCounts = new HashMap<>();
        for (Room room : rooms) {
            playerCounts.put(room.getRoomId(), Math.toIntExact(roomPlayerRepository.countByRoom_RoomId(room.getRoomId())));
        }
        return playerCounts;
    }

    private Map<Long, UserProfile> loadProfilesByAccountId(List<RoomPlayer> roomPlayers) {
        List<Long> accountIds = roomPlayers.stream()
                .map(player -> player.getAccount().getAccountId())
                .distinct()
                .toList();

        return userProfileRepository.findByAccount_AccountIdIn(accountIds).stream()
                .collect(Collectors.toMap(profile -> profile.getAccount().getAccountId(), profile -> profile));
    }

    private Map<Integer, Hero> loadHeroesById(List<RoomPlayer> roomPlayers) {
        List<Integer> heroIds = roomPlayers.stream()
                .map(RoomPlayer::getSelectedHeroId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (heroIds.isEmpty()) {
            return Map.of();
        }

        return heroRepository.findAllById(heroIds).stream()
                .collect(Collectors.toMap(Hero::getCharacterId, hero -> hero));
    }

    private RoomDetailResponse.PlayerDto toPlayerDto(
            RoomPlayer roomPlayer,
            Map<Long, UserProfile> profilesByAccountId,
            Map<Integer, Hero> heroesById
    ) {
        Long accountId = roomPlayer.getAccount().getAccountId();
        UserProfile profile = profilesByAccountId.get(accountId);
        Hero hero = roomPlayer.getSelectedHeroId() != null ? heroesById.get(roomPlayer.getSelectedHeroId()) : null;

        String avatarUrl = null;
        if (profile != null) {
            avatarUrl = profile.getAvatarUrl();
            if (avatarUrl != null && avatarUrl.isBlank()) {
                avatarUrl = null;
            }
        }

        return RoomDetailResponse.PlayerDto.builder()
                .playerId(accountId)
                .username(profile != null ? profile.getUsername() : "Unknown")
                .avatarUrl(avatarUrl)
                .selectedHeroName(hero != null ? hero.getName() : null)
                .selectedHeroId(roomPlayer.getSelectedHeroId() != null ? roomPlayer.getSelectedHeroId().longValue() : null)
                .isHost(roomPlayer.getIsHost())
                .isReady(roomPlayer.getIsReady())
                .slotIndex(roomPlayer.getSlotIndex())
                .build();
    }

    private RoomDetailResponse.HeroDto toHeroDto(Integer heroId, Map<Integer, Hero> heroesById) {
        if (heroId == null) {
            return null;
        }

        Hero hero = heroesById.get(heroId);
        String heroName = hero != null ? hero.getName() : "Hero " + heroId;

        return RoomDetailResponse.HeroDto.builder()
                .heroId(heroId.longValue())
                .name(heroName)
                .imageUrl(null)
                .build();
    }

    private UserProfile getProfile(Long accountId) {
        return userProfileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));
    }

    private Room getRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }

    private RoomPlayer getRoomPlayer(Long roomId, Long accountId) {
        return roomPlayerRepository.findByRoom_RoomIdAndAccount_AccountId(roomId, accountId)
                .orElseThrow(() -> new RuntimeException("Player is not in this room"));
    }

    private void requireHost(RoomPlayer roomPlayer) {
        if (!Boolean.TRUE.equals(roomPlayer.getIsHost())) {
            throw new RuntimeException("Only host can perform this action");
        }
    }

    private int normalizeMaxPlayers(Integer maxPlayers) {
        if (maxPlayers == null) {
            return 4;
        }
        if (maxPlayers < 2 || maxPlayers > 4) {
            throw new RuntimeException("Max players must be between 2 and 4");
        }
        return maxPlayers;
    }

    private String normalizeRoomName(String name, String username) {
        if (name == null || name.isBlank()) {
            return username + " Room";
        }
        return name.trim();
    }

    private void validatePasswordForVisibility(RoomVisibility visibility, String password) {
        if (visibility == RoomVisibility.PRIVATE && (password == null || password.isBlank())) {
            throw new RuntimeException("Password is required for private room");
        }
    }

    private String generateUniqueRoomCode() {
        String roomCode;
        do {
            roomCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (roomRepository.existsByRoomCode(roomCode));
        return roomCode;
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new RuntimeException("Room code is required");
        }
        return roomCode.trim().toUpperCase();
    }

    private int findNextAvailableSlot(List<RoomPlayer> players, int maxPlayers) {
        Set<Integer> occupiedSlots = players.stream()
                .map(RoomPlayer::getSlotIndex)
                .collect(Collectors.toSet());

        for (int slot = 1; slot <= maxPlayers; slot++) {
            if (!occupiedSlots.contains(slot)) {
                return slot;
            }
        }
        throw new RuntimeException("No available slot");
    }

}
