package com.promex04.service;

import com.promex04.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Xử lý giao tiếp với một client cụ thể
 * Mỗi ClientHandler chạy trong một Thread riêng
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private static final int DEFAULT_TOTAL_ROUNDS = 15;

    private final Socket socket;
    private final ClientManager clientManager;
    private final UserService userService;
    private final GameService gameService;
    private BinaryTransferService binaryTransferService;

    private PrintWriter out;
    private BufferedReader in;
    private User currentUser;
    private boolean inGame = false;
    private Game currentGame;
    private GameRound currentRound;
    private LocalDateTime roundStartTime;
    private ScheduledFuture<?> roundTimer;
    private ScheduledExecutorService scheduler;
    private ClientHandler opponentHandler;
    private volatile long lastHeartbeatAt = System.currentTimeMillis();
    // Theo dõi tiến độ cá nhân (không đồng bộ)
    private int myRoundNumber = 1; // 1..totalRounds
    private int myCompleted = 0; // số câu đã hoàn thành
    private volatile boolean myFinished = false; // volatile để đảm bảo visibility giữa các thread
    private int player1ScoreCache = 0;
    private int player2ScoreCache = 0;
    // Prefetch playlist và trạng thái sẵn sàng
    private java.util.List<AudioSegment> myPlaylist;
    private boolean myPrefetchReady = false;
    private boolean roundsStarted = false;
    private String pendingArtist;
    private String pendingGenre;
    private int totalRounds = DEFAULT_TOTAL_ROUNDS; // Số lượng câu trong game hiện tại

    public ClientHandler(Socket socket, ClientManager clientManager,
            UserService userService, GameService gameService) {
        this.socket = socket;
        this.clientManager = clientManager;
        this.userService = userService;
        this.gameService = gameService;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    public void setBinaryTransferService(BinaryTransferService binaryTransferService) {
        this.binaryTransferService = binaryTransferService;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            // Tăng buffer size của BufferedReader để giảm số lần đọc
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()), 65536); // 64KB buffer

            // Schedule inactivity watchdog to detect dead connections
            scheduler.scheduleAtFixedRate(() -> {
                long idleMs = System.currentTimeMillis() - lastHeartbeatAt;
                // If no heartbeat/message from client for 90s, close connection
                if (idleMs > 90_000) {
                    logger.warn("Client idle too long ({} ms), closing: {}", idleMs,
                            currentUser != null ? currentUser.getUsername() : socket.getRemoteSocketAddress());
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            }, 30, 30, TimeUnit.SECONDS);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                handleMessage(inputLine.trim());
            }
        } catch (IOException e) {
            logger.error("Lỗi đọc dữ liệu từ client", e);
        } finally {
            cleanup();
        }
    }

    private void handleMessage(String message) {
        logger.debug("Nhận message: {}", message);
        // Update last heartbeat on any received message
        lastHeartbeatAt = System.currentTimeMillis();
        String[] parts = message.split(":", 3);
        if (parts.length == 0)
            return;

        String command = parts[0];

        try {
            switch (command) {
                case "LOGIN":
                    handleLogin(parts);
                    break;
                case "REGISTER":
                    handleRegister(parts);
                    break;
                case "PING":
                    // Heartbeat from client
                    lastHeartbeatAt = System.currentTimeMillis();
                    sendMessage("PONG");
                    break;
                case "CHAT_LOBBY":
                    handleChatLobby(parts);
                    break;
                case "CHALLENGE":
                    handleChallenge(parts);
                    break;
                case "CHALLENGE_RESPONSE":
                    handleChallengeResponse(parts);
                    break;
                case "GAME_SUBMIT":
                    handleGameSubmit(parts);
                    break;
                case "LEAVE_GAME":
                    handleLeaveGame();
                    break;
                case "GET_RANKING":
                    handleGetRanking();
                    break;
                case "PREFETCH_DONE":
                    handlePrefetchDone();
                    break;
                case "LIST_AUDIO_TAGS":
                    handleAudioTagsRequest();
                    break;
                case "SEARCH_ARTISTS":
                    handleSearchArtists(parts);
                    break;
                case "SEARCH_GENRES":
                    handleSearchGenres(parts);
                    break;
                case "REQUEST_AUDIO_FILE":
                    handleRequestAudioFile(parts);
                    break;
                default:
                    sendMessage("ERROR:Unknown command");
            }
        } catch (Exception e) {
            logger.error("Lỗi xử lý command: " + command, e);
            sendMessage("ERROR:" + e.getMessage());
        }
    }

    private void handleLogin(String[] parts) {
        if (parts.length < 3) {
            sendMessage("LOGIN_FAILED:Định dạng không hợp lệ");
            return;
        }

        String username = parts[1].trim();
        String password = parts[2];

        // Kiểm tra xem đã đăng nhập chưa
        if (currentUser != null) {
            sendMessage("LOGIN_FAILED:Bạn đã đăng nhập rồi");
            return;
        }

        // Kiểm tra username rỗng
        if (username.isEmpty()) {
            sendMessage("LOGIN_FAILED:Tên đăng nhập không được để trống");
            return;
        }

        // Kiểm tra đăng nhập
        var userOpt = userService.authenticate(username, password);
        if (userOpt.isEmpty()) {
            sendMessage("LOGIN_FAILED:Tên đăng nhập hoặc mật khẩu không đúng");
            return;
        }

        currentUser = userOpt.get();

        // Kiểm tra xem user này đã đăng nhập ở client khác chưa
        ClientHandler existingHandler = clientManager.getClient(username);
        if (existingHandler != null && existingHandler != this) {
            // Gửi thông báo cho client cũ và xóa khỏi manager
            existingHandler.sendMessage("ERROR:Bạn đã đăng nhập ở thiết bị khác");
            clientManager.removeClient(username);
            // Handler cũ sẽ tự cleanup khi socket đóng trong cleanup()
        }

        // Đăng ký với ClientManager
        clientManager.addClient(username, this);

        // Gửi thông báo đăng nhập thành công
        sendMessage("LOGIN_SUCCESS:" + currentUser.getUsername() + ":" + currentUser.getTotalScore());

        // Broadcast cập nhật danh sách lobby đến tất cả clients (bao gồm cả client mới)
        clientManager.broadcastLobbyUpdate();

        // Tự động gửi bảng xếp hạng
        clientManager.broadcastRanking();

        logger.info("User đăng nhập: {}", username);
    }

    private void handleRegister(String[] parts) {
        if (parts.length < 3) {
            sendMessage("REGISTER_FAILED:Định dạng không hợp lệ");
            return;
        }

        String username = parts[1].trim();
        String password = parts[2];

        // Kiểm tra xem đã đăng nhập chưa
        if (currentUser != null) {
            sendMessage("REGISTER_FAILED:Bạn đã đăng nhập rồi");
            return;
        }

        // Kiểm tra username rỗng
        if (username.isEmpty()) {
            sendMessage("REGISTER_FAILED:Tên đăng nhập không được để trống");
            return;
        }

        // Kiểm tra độ dài username và password
        if (username.length() < 3 || username.length() > 20) {
            sendMessage("REGISTER_FAILED:Tên đăng nhập phải từ 3-20 ký tự");
            return;
        }

        if (password.length() < 3 || password.length() > 50) {
            sendMessage("REGISTER_FAILED:Mật khẩu phải từ 3-50 ký tự");
            return;
        }

        // Kiểm tra ký tự hợp lệ cho username (chỉ chữ cái, số, dấu gạch dưới)
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            sendMessage("REGISTER_FAILED:Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới");
            return;
        }

        // Kiểm tra username đã tồn tại chưa
        var existingUser = userService.findByUsername(username);
        if (existingUser.isPresent()) {
            sendMessage("REGISTER_FAILED:Tên đăng nhập đã tồn tại");
            return;
        }

        try {
            // Tạo tài khoản mới
            currentUser = userService.createUser(username, password);
            logger.info("Đăng ký tài khoản mới: {}", username);

            // Đăng ký với ClientManager
            clientManager.addClient(username, this);

            // Gửi thông báo đăng ký thành công
            sendMessage("REGISTER_SUCCESS:" + currentUser.getUsername() + ":" + currentUser.getTotalScore());

            // Broadcast cập nhật danh sách lobby đến tất cả clients (bao gồm cả client mới)
            clientManager.broadcastLobbyUpdate();

            // Tự động gửi bảng xếp hạng
            clientManager.broadcastRanking();
        } catch (Exception e) {
            logger.error("Lỗi đăng ký: {}", e.getMessage(), e);
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Lỗi hệ thống khi đăng ký";
            }
            sendMessage("REGISTER_FAILED:" + errorMsg);
        }
    }

    private void handleChatLobby(String[] parts) {
        if (currentUser == null || parts.length < 2) {
            sendMessage("ERROR:Not logged in or invalid message");
            return;
        }

        // Hỗ trợ tin nhắn có dấu ':' bằng cách ghép phần còn lại
        String message;
        if (parts.length >= 3) {
            message = parts[1] + ":" + parts[2];
        } else {
            message = parts[1];
        }

        clientManager.broadcastToLobby("MSG_LOBBY:" + currentUser.getUsername() + ":" + message);
    }

    private void handleChallenge(String[] parts) {
        if (currentUser == null || inGame || parts.length < 2) {
            sendMessage("ERROR:Cannot challenge");
            return;
        }

        String opponentUsername = parts[1];
        String[] preferenceParts = parts.length >= 3 ? parseFilterPayload(parts[2])
                : new String[] { null, null, String.valueOf(DEFAULT_TOTAL_ROUNDS) };
        String selectedArtist = preferenceParts[0];
        String selectedGenre = preferenceParts[1];
        int selectedTotalRounds = parseTotalRounds(preferenceParts.length > 2 ? preferenceParts[2] : null);

        if (opponentUsername.equals(currentUser.getUsername())) {
            sendMessage("ERROR:Cannot challenge yourself");
            return;
        }

        ClientHandler opponent = clientManager.getClient(opponentUsername);
        if (opponent == null || opponent.isInGame()) {
            sendMessage("CHALLENGE_FAILED:Opponent not available");
            return;
        }

        // Lưu lựa chọn chủ đề và số lượng câu
        this.pendingArtist = selectedArtist;
        this.pendingGenre = selectedGenre;
        this.totalRounds = selectedTotalRounds;
        opponent.pendingArtist = selectedArtist;
        opponent.pendingGenre = selectedGenre;
        opponent.totalRounds = selectedTotalRounds;

        String metadataPayload = buildFilterPayload(selectedArtist, selectedGenre, selectedTotalRounds);

        // Gửi thách đấu đến đối thủ
        opponent.sendMessage("CHALLENGE_REQUEST:" + currentUser.getUsername() + ":" + metadataPayload);

        // Lưu trữ handler của đối thủ để xử lý response
        this.opponentHandler = opponent;
        opponent.opponentHandler = this;

        sendMessage("CHALLENGE_SENT:" + opponentUsername + ":" + metadataPayload);
    }

    private void handleChallengeResponse(String[] parts) {
        if (currentUser == null || parts.length < 2 || opponentHandler == null) {
            sendMessage("ERROR:Invalid challenge response");
            return;
        }

        String response = parts[1];

        if ("accept".equalsIgnoreCase(response)) {
            // Bắt đầu game - người gửi thách đấu là player1, người chấp nhận là player2
            User challenger = opponentHandler.currentUser; // Người gửi CHALLENGE
            User accepter = currentUser; // Người chấp nhận

            String selectedArtist = resolvePendingArtist();
            String selectedGenre = resolvePendingGenre();

            Game game = gameService.createGame(challenger, accepter, selectedArtist, selectedGenre);

            this.currentGame = game;
            this.inGame = true;
            opponentHandler.currentGame = game;
            opponentHandler.inGame = true;
            this.player1ScoreCache = 0;
            this.player2ScoreCache = 0;
            opponentHandler.player1ScoreCache = 0;
            opponentHandler.player2ScoreCache = 0;

            // Gửi thông báo chấp nhận cho cả hai
            sendMessage("CHALLENGE_ACCEPTED:" + opponentHandler.currentUser.getUsername());
            opponentHandler.sendMessage("CHALLENGE_ACCEPTED:" + currentUser.getUsername());

            // Cập nhật trạng thái lobby để mọi người thấy 2 người đang bận
            clientManager.broadcastLobbyUpdate();

            // Khởi tạo tiến độ cho từng người
            this.myRoundNumber = 1;
            this.myCompleted = 0;
            this.myFinished = false;
            opponentHandler.myRoundNumber = 1;
            opponentHandler.myCompleted = 0;
            opponentHandler.myFinished = false;

            // Chuẩn bị playlist prefetch (giống nhau) cho cả hai theo chủ đề đã chọn
            // selectedArtist và selectedGenre đã được lấy ở trên khi tạo game
            List<AudioSegment> sharedPlaylist = gameService.getRandomAudioSegments(totalRounds, selectedArtist, selectedGenre);
            this.myPlaylist = new ArrayList<>(sharedPlaylist);
            this.myPrefetchReady = false;
            this.roundsStarted = false; // Reset flag để bắt đầu game mới
            opponentHandler.myPlaylist = new ArrayList<>(sharedPlaylist);
            opponentHandler.myPrefetchReady = false;
            opponentHandler.roundsStarted = false; // Reset flag để bắt đầu game mới

            // Gửi danh sách file cần tải trước cho mỗi người chơi
            String thisPrefetch = buildPrefetchMessage(this.myPlaylist);
            String oppPrefetch = buildPrefetchMessage(opponentHandler.myPlaylist);
            sendMessage(thisPrefetch);
            opponentHandler.sendMessage(oppPrefetch);
            logger.info("Sent PREFETCH to {} ({} files) and {} ({} files)",
                    this.currentUser.getUsername(), this.myPlaylist.size(),
                    opponentHandler.currentUser.getUsername(), opponentHandler.myPlaylist.size());

            // Gửi điểm/tiến độ ban đầu
            sendPersonalizedScoreUpdate(this);
            sendPersonalizedScoreUpdate(opponentHandler);
            sendProgressToSelf();
            opponentHandler.sendProgressToSelf();
            // Chờ cả hai PREFETCH_DONE mới bắt đầu câu đầu tiên
            clearPendingPreferences();
            if (opponentHandler != null) {
                opponentHandler.clearPendingPreferences();
            }
        } else {
            sendMessage("CHALLENGE_REJECTED:" + opponentHandler.currentUser.getUsername());
            opponentHandler.sendMessage("CHALLENGE_REJECTED:" + currentUser.getUsername());

            if (opponentHandler != null) {
                opponentHandler.clearPendingPreferences();
            }
            clearPendingPreferences();
            opponentHandler.opponentHandler = null;
            this.opponentHandler = null;
        }
    }

    private String buildPrefetchMessage(java.util.List<AudioSegment> list) {
        StringBuilder sb = new StringBuilder("PREFETCH:");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0)
                sb.append(';');
            String p = list.get(i).getFilePath();
            // Đảm bảo bắt đầu bằng '/'
            if (p != null && !p.startsWith("/"))
                p = "/" + p;
            sb.append(p);
        }
        return sb.toString();
    }

    private void handleGameSubmit(String[] parts) {
        if (currentUser == null || !inGame || currentRound == null || parts.length < 2) {
            sendMessage("ERROR:Invalid game submission");
            return;
        }

        try {
            int answerIndex = Integer.parseInt(parts[1]);
            long timeMs = java.time.Duration.between(roundStartTime, LocalDateTime.now()).toMillis();

            // Xử lý câu trả lời
            GameService.RoundResult result = gameService.processRoundAnswer(
                    currentRound, currentUser.getUsername(), answerIndex, timeMs);
            updateScoreCaches(currentUser.getUsername(), result.getPoints());

            // Hủy timer nếu còn chạy
            if (roundTimer != null) {
                roundTimer.cancel(false);
            }

            // Gửi kết quả cho người chơi này
            sendMessage("ROUND_RESULT:" +
                    (result.isCorrect() ? "correct" : "wrong") + ":" +
                    result.getPoints() + ":" + timeMs);

            // Hoàn tất round cá nhân và chuyển câu tiếp theo (không chờ đối thủ)
            finishPersonalRoundAndContinue();
        } catch (NumberFormatException e) {
            sendMessage("ERROR:Invalid answer index");
        }
    }

    private void handlePrefetchDone() {
        // Đánh dấu client này đã xong tải trước
        this.myPrefetchReady = true;
        String username = currentUser != null ? currentUser.getUsername() : "unknown";
        String opponentName = opponentHandler != null
                ? (opponentHandler.currentUser != null ? opponentHandler.currentUser.getUsername() : "null")
                : "null";
        boolean opponentReady = opponentHandler != null && opponentHandler.myPrefetchReady;
        logger.info("PREFETCH_DONE from {} (opponent: {}, opponent ready: {}, rounds started: {}, inGame: {})",
                username, opponentName, opponentReady, this.roundsStarted, this.inGame);

        // Kiểm tra điều kiện để bắt đầu game
        if (opponentHandler == null) {
            logger.error("PREFETCH_DONE from {} but opponentHandler is null! Cannot start game.", username);
            return;
        }

        if (!inGame) {
            logger.warn("PREFETCH_DONE from {} but not in game! inGame={}", username, inGame);
            return;
        }

        // Khi cả hai sẵn sàng thì bắt đầu vòng đầu tiên cho cả hai
        if (opponentReady) {
            // Sử dụng synchronized để đảm bảo thread-safe khi kiểm tra và set roundsStarted
            // Sử dụng currentGame làm lock nếu có, nếu không thì dùng opponentHandler
            Object lock = currentGame != null ? currentGame : (opponentHandler != null ? opponentHandler : this);
            synchronized (lock) {
                // Kiểm tra lại điều kiện sau khi lock để tránh race condition
                boolean bothReady = opponentHandler != null && opponentHandler.myPrefetchReady && this.myPrefetchReady;
                if (bothReady && !this.roundsStarted && !opponentHandler.roundsStarted) {
                    this.roundsStarted = true;
                    opponentHandler.roundsStarted = true;
                    logger.info("Both players ready. Starting rounds for {} and {}",
                            currentUser != null ? currentUser.getUsername() : "P1",
                            opponentHandler.currentUser != null ? opponentHandler.currentUser.getUsername() : "P2");
                    try {
                        startNextRoundFor(this);
                        startNextRoundFor(opponentHandler);
                        logger.info("Successfully started rounds for both players");
                    } catch (Exception e) {
                        logger.error("Error starting rounds", e);
                        // Reset flags nếu có lỗi
                        this.roundsStarted = false;
                        if (opponentHandler != null) {
                            opponentHandler.roundsStarted = false;
                        }
                    }
                } else {
                    logger.warn(
                            "Attempted to start rounds but conditions not met (bothReady: {}, this.roundsStarted: {}, opponent.roundsStarted: {})",
                            bothReady, this.roundsStarted,
                            opponentHandler != null ? opponentHandler.roundsStarted : false);
                }
            }
        } else {
            logger.info("Waiting for opponent {} to finish prefetch (opponent ready: {})",
                    opponentHandler.currentUser != null ? opponentHandler.currentUser.getUsername() : "unknown",
                    opponentHandler.myPrefetchReady);
            // Thông báo cho client biết đang chờ đối thủ
            sendMessage("WAITING_OPPONENT_PREFETCH:" +
                    (opponentHandler.currentUser != null ? opponentHandler.currentUser.getUsername() : "unknown"));
        }
    }

    private void handleGetRanking() {
        if (currentUser == null) {
            sendMessage("ERROR:Not logged in");
            return;
        }

        List<User> ranking = userService.getRanking();
        StringBuilder sb = new StringBuilder("RANKING:");

        int rank = 1;
        for (User user : ranking) {
            if (sb.length() > "RANKING:".length())
                sb.append(";");
            sb.append(rank).append(",")
                    .append(user.getUsername()).append(",")
                    .append(user.getTotalScore()).append(",")
                    .append(user.getCorrectAnswers()).append(",")
                    .append(user.getGamesWon());
            rank++;
        }

        sendMessage(sb.toString());
    }

    private void startNextRound() {
        // Không còn dùng cho chế độ không đồng bộ. Giữ lại để tương thích cũ.
        startNextRoundFor(this);
    }

    private void finishRound() {
        // Không còn dùng trong chế độ không đồng bộ. Thay bằng
        // finishPersonalRoundAndContinue().
        finishPersonalRoundAndContinue();
    }

    private void startNextRoundFor(ClientHandler target) {
        // Hủy timer cũ nếu có
        if (target.roundTimer != null) {
            target.roundTimer.cancel(false);
        }

        // Kiểm tra đã hoàn thành tất cả câu chưa
        if (target.myRoundNumber > target.totalRounds) {
            target.myFinished = true;
            if (bothPlayersFinished()) {
                finishGame();
            }
            return;
        }

        // Kiểm tra game và user hợp lệ
        if (target.currentGame == null) {
            logger.error("Cannot start round for {}: currentGame is null",
                    target.currentUser != null ? target.currentUser.getUsername() : "unknown");
            return;
        }
        if (target.currentUser == null) {
            logger.error("Cannot start round: currentUser is null");
            return;
        }

        // Tạo round mới cho target theo playlist đã chuẩn bị (fallback random nếu
        // thiếu)
        AudioSegment segmentForRound = null;
        if (target.myPlaylist != null) {
            int idx = target.myRoundNumber - 1;
            if (idx >= 0 && idx < target.myPlaylist.size()) {
                segmentForRound = target.myPlaylist.get(idx);
            }
        }
        if (segmentForRound == null) {
            segmentForRound = gameService.getRandomAudioSegment();
        }

        try {
            // Sử dụng findOrCreateRoundWithSegment để đảm bảo cả hai người chơi dùng chung
            // một round
            target.currentRound = gameService.findOrCreateRoundWithSegment(target.currentGame, target.myRoundNumber,
                    segmentForRound);
            target.roundStartTime = LocalDateTime.now();
            logger.debug("Found/created round {} for {} (round ID: {})", target.myRoundNumber,
                    target.currentUser != null ? target.currentUser.getUsername() : "unknown",
                    target.currentRound.getId());
        } catch (Exception e) {
            logger.error("Error creating round for {}",
                    target.currentUser != null ? target.currentUser.getUsername() : "unknown", e);
            return;
        }

        // Sử dụng delimiter "||" để tránh xung đột với dấu ':' trong option names
        String roundData = "ROUND_START:" +
                target.currentRound.getAudioSegment().getId() + "||" +
                target.currentRound.getRoundNumber() + "||" +
                target.currentRound.getOption1() + "||" +
                target.currentRound.getOption2() + "||" +
                target.currentRound.getOption3() + "||" +
                target.currentRound.getOption4() + "||" +
                target.currentRound.getAudioSegment().getFilePath();

        logger.info("Sending ROUND_START to {}: round {}",
                target.currentUser != null ? target.currentUser.getUsername() : "unknown",
                target.currentRound.getRoundNumber());
        target.sendMessage(roundData);

        // Bắt đầu timer 15 giây cho target
        target.roundTimer = target.scheduler.schedule(() -> {
            if (target.currentRound != null) {
                // Hết giờ - tự động submit sai
                GameService.RoundResult autoResult = gameService.processRoundAnswer(
                        target.currentRound, target.currentUser.getUsername(), 0, 15000);
                target.updateScoreCaches(target.currentUser.getUsername(), autoResult.getPoints());
                target.sendMessage("ROUND_RESULT:timeout:0:15000");
                target.finishPersonalRoundAndContinue();
            }
        }, 15, TimeUnit.SECONDS);
    }

    private void finishPersonalRoundAndContinue() {
        // Đánh dấu round hiện tại hoàn thành
        gameService.finishRound(currentRound);
        myCompleted++;
        myRoundNumber++;

        // Gửi tổng điểm hiện tại
        sendPersonalizedScoreUpdate(this);
        if (opponentHandler != null)
            sendPersonalizedScoreUpdate(opponentHandler);

        // Gửi tiến độ tới từng client (cá nhân hoá theo góc nhìn)
        sendProgressToSelf();
        if (opponentHandler != null)
            opponentHandler.sendProgressToSelf();

        // Nếu đã xong tất cả câu của mình
        if (myRoundNumber > totalRounds) {
            myFinished = true;
            String myUsername = currentUser != null ? currentUser.getUsername() : "unknown";
            String opponentUsername = opponentHandler != null && opponentHandler.currentUser != null
                    ? opponentHandler.currentUser.getUsername()
                    : "null";
            boolean opponentFinished = opponentHandler != null ? opponentHandler.myFinished : false;

            logger.info("[{}] Đã hoàn thành {} câu. myFinished={}, opponentFinished={}, opponent={}",
                    myUsername, totalRounds, myFinished, opponentFinished, opponentUsername);

            // Sử dụng synchronized trên currentGame để đảm bảo chỉ một thread gọi
            // finishGame()
            // và kiểm tra điều kiện một cách an toàn
            if (currentGame != null) {
                synchronized (currentGame) {
                    // Kiểm tra lại điều kiện sau khi lock để đảm bảo tính chính xác
                    boolean bothFinished = bothPlayersFinished();
                    logger.info(
                            "[{}] Trong synchronized block: bothPlayersFinished()={}, myFinished={}, opponent.myFinished={}",
                            myUsername, bothFinished, myFinished,
                            opponentHandler != null ? opponentHandler.myFinished : false);

                    if (bothFinished) {
                        logger.info("[{}] Cả hai người chơi đã hoàn thành, kết thúc game ngay lập tức",
                                myUsername);
                        finishGame();
                        return;
                    }
                }
            } else {
                logger.warn("[{}] currentGame == null, không thể kết thúc game", myUsername);
            }

            logger.info("[{}] Chờ đối thủ {} hoàn thành... (opponentFinished={})",
                    myUsername, opponentUsername, opponentFinished);
            return; // Chờ đối thủ xong
        }

        // Đợi một chút rồi tiếp tục cho riêng mình
        scheduler.schedule(() -> startNextRoundFor(this), 1, TimeUnit.SECONDS);
    }

    private boolean bothPlayersFinished() {
        boolean opp = opponentHandler != null && opponentHandler.myFinished;
        return myFinished && opp;
    }

    private void sendProgressToSelf() {
        int my = calculateDisplayRound(this);
        int opp = opponentHandler != null ? calculateDisplayRound(opponentHandler) : 0;
        sendMessage("PROGRESS:" + my + ":" + opp);
    }

    private int calculateDisplayRound(ClientHandler handler) {
        if (handler == null) {
            return 0;
        }
        if (handler.myFinished) {
            return handler.totalRounds;
        }
        int nextRound = handler.myCompleted + 1;
        if (nextRound > handler.totalRounds) {
            nextRound = handler.totalRounds;
        }
        return Math.max(1, nextRound);
    }

    private void sendPersonalizedScoreUpdate(ClientHandler target) {
        Game game = target.currentGame != null ? target.currentGame : this.currentGame;
        if (game == null || target.currentUser == null)
            return;
        boolean targetIsP1 = game.getPlayer1().getUsername().equals(target.currentUser.getUsername());
        int my = targetIsP1 ? target.player1ScoreCache : target.player2ScoreCache;
        int opp = targetIsP1 ? target.player2ScoreCache : target.player1ScoreCache;
        target.sendMessage("SCORE_UPDATE:" + my + ":" + opp);
    }

    private void updateScoreCaches(String playerUsername, int points) {
        if (currentGame == null || playerUsername == null) {
            return;
        }

        synchronized (currentGame) {
            boolean isPlayer1 = currentGame.getPlayer1().getUsername().equals(playerUsername);
            if (isPlayer1) {
                player1ScoreCache = Math.max(0, player1ScoreCache + points);
            } else {
                player2ScoreCache = Math.max(0, player2ScoreCache + points);
            }

            currentGame.setPlayer1Score(player1ScoreCache);
            currentGame.setPlayer2Score(player2ScoreCache);

            // Đồng bộ điểm với đối thủ: chỉ cập nhật điểm của người chơi vừa trả lời
            // để đảm bảo điểm của hai người chơi độc lập với nhau
            if (opponentHandler != null) {
                if (opponentHandler.currentGame == null) {
                    opponentHandler.currentGame = currentGame;
                } else if (opponentHandler.currentGame != currentGame) {
                    opponentHandler.currentGame = currentGame;
                }

                // Chỉ cập nhật điểm của người chơi vừa trả lời trong cache của đối thủ
                if (isPlayer1) {
                    opponentHandler.player1ScoreCache = this.player1ScoreCache;
                    opponentHandler.currentGame.setPlayer1Score(player1ScoreCache);
                } else {
                    opponentHandler.player2ScoreCache = this.player2ScoreCache;
                    opponentHandler.currentGame.setPlayer2Score(player2ScoreCache);
                }
            }
        }
    }

    private void finishGame() {
        if (currentGame == null) {
            logger.warn("[{}] finishGame() được gọi nhưng currentGame == null",
                    currentUser != null ? currentUser.getUsername() : "unknown");
            return;
        }

        String myUsername = currentUser != null ? currentUser.getUsername() : "unknown";
        logger.info("[{}] Bắt đầu kết thúc game. Player1Score={}, Player2Score={}",
                myUsername, player1ScoreCache, player2ScoreCache);

        // Lưu reference đến opponentHandler trước khi reset để đảm bảo gửi message được
        ClientHandler opponent = opponentHandler;

        synchronized (currentGame) {
            currentGame.setPlayer1Score(player1ScoreCache);
            currentGame.setPlayer2Score(player2ScoreCache);
        }
        Game finishedGame = gameService.finishGame(currentGame);

        // Xác định người thắng
        String winner = finishedGame.getWinner() != null
                ? finishedGame.getWinner().getUsername()
                : "draw";

        // Gửi kết quả cuối cùng được cá nhân hóa cho từng người chơi
        // Format: GAME_OVER:myScore:opponentScore:winner
        boolean thisIsPlayer1 = finishedGame.getPlayer1().getUsername().equals(currentUser.getUsername());
        int myScore = thisIsPlayer1 ? finishedGame.getPlayer1Score() : finishedGame.getPlayer2Score();
        int opponentScore = thisIsPlayer1 ? finishedGame.getPlayer2Score() : finishedGame.getPlayer1Score();

        String gameResult = "GAME_OVER:" + myScore + ":" + opponentScore + ":" + winner;
        logger.info("[{}] Gửi GAME_OVER cho chính mình: {}", myUsername, gameResult);
        sendMessage(gameResult);

        // Gửi message cho đối thủ ngay lập tức (sử dụng reference đã lưu)
        if (opponent != null && opponent.currentUser != null) {
            // Gửi kết quả cho đối thủ (đảo ngược điểm số)
            boolean opponentIsPlayer1 = finishedGame.getPlayer1().getUsername()
                    .equals(opponent.currentUser.getUsername());
            int opponentMyScore = opponentIsPlayer1 ? finishedGame.getPlayer1Score() : finishedGame.getPlayer2Score();
            int opponentOpponentScore = opponentIsPlayer1 ? finishedGame.getPlayer2Score()
                    : finishedGame.getPlayer1Score();

            String opponentGameResult = "GAME_OVER:" + opponentMyScore + ":" + opponentOpponentScore + ":" + winner;
            logger.info("[{}] Gửi GAME_OVER cho đối thủ {}: {}",
                    myUsername,
                    opponent.currentUser.getUsername(),
                    opponentGameResult);
            opponent.sendMessage(opponentGameResult);

            // Reset trạng thái của đối thủ
            opponent.inGame = false;
            opponent.currentGame = null;
            opponent.currentRound = null;
            opponent.opponentHandler = null;
            opponent.myFinished = false; // Reset flag
        } else {
            logger.warn("[{}] Không thể gửi GAME_OVER cho đối thủ vì opponentHandler == null", myUsername);
        }

        // Reset trạng thái của chính mình
        inGame = false;
        currentGame = null;
        currentRound = null;
        opponentHandler = null;
        myPlaylist = null;
        myPrefetchReady = false;
        roundsStarted = false;
        myFinished = false; // Reset flag

        // Broadcast cập nhật lobby sau khi game kết thúc
        clientManager.broadcastLobbyUpdate();

        // Cập nhật thống kê đã xong, broadcast ranking mới đến tất cả clients
        clientManager.broadcastRanking();

        logger.info("[{}] Đã hoàn tất kết thúc game và gửi GAME_OVER cho cả hai người chơi", myUsername);
    }

    private void handleLeaveGame() {
        if (currentUser == null) {
            sendMessage("ERROR:Not logged in");
            return;
        }
        if (!inGame || currentGame == null) {
            sendMessage("ERROR:Not in game");
            return;
        }

        // Hủy timer nếu có
        if (roundTimer != null) {
            roundTimer.cancel(false);
        }

        // Đánh dấu người rời trận đã hoàn tất cá nhân và rời game
        myFinished = true;
        myRoundNumber = totalRounds + 1; // > totalRounds để dừng phát câu mới
        inGame = false;
        currentRound = null;

        // Người còn lại vẫn tiếp tục game (vẫn bận). Không gửi GAME_OVER cho họ.
        // Gửi cập nhật lobby để phản ánh trạng thái: mình rảnh, đối thủ bận
        clientManager.broadcastLobbyUpdate();

        // Nếu đối thủ cũng đã hoàn tất, kết thúc game và cập nhật ranking
        if (bothPlayersFinished()) {
            finishGame();
        }
        // Reset prefetch state của người rời
        myPlaylist = null;
        myPrefetchReady = false;
        roundsStarted = false;
    }

    private void handleAudioTagsRequest() {
        List<String> artists = gameService.getDistinctArtists();
        List<String> genres = gameService.getDistinctGenres();
        String artistPayload = encodeList(artists);
        String genrePayload = encodeList(genres);
        sendMessage("AUDIO_TAGS:" + artistPayload + ":" + genrePayload);
    }

    private void handleSearchArtists(String[] parts) {
        if (currentUser == null) {
            sendMessage("ERROR:Not logged in");
            return;
        }
        String keyword = parts.length >= 2 ? decodeComponent(parts[1]) : "";
        List<String> artists = gameService.searchArtists(keyword);
        String artistPayload = encodeList(artists);
        sendMessage("SEARCH_ARTISTS_RESULT:" + artistPayload);
    }

    private void handleSearchGenres(String[] parts) {
        if (currentUser == null) {
            sendMessage("ERROR:Not logged in");
            return;
        }
        String keyword = parts.length >= 2 ? decodeComponent(parts[1]) : "";
        List<String> genres = gameService.searchGenres(keyword);
        String genrePayload = encodeList(genres);
        sendMessage("SEARCH_GENRES_RESULT:" + genrePayload);
    }

    private void handleRequestAudioFile(String[] parts) {
        if (parts.length < 2) {
            sendMessage("ERROR:Invalid audio file request");
            return;
        }

        String filePath = parts[1];
        // Đảm bảo bắt đầu bằng '/'
        if (!filePath.startsWith("/")) {
            filePath = "/" + filePath;
        }

        logger.info("[{}] Request audio file: {}",
                currentUser != null ? currentUser.getUsername() : "unknown", filePath);

        try {
            // Tìm file trong resources/static
            // Trong JAR, đường dẫn là "static/path/to/file.mp3"
            // Trong development, có thể là "src/main/resources/static/path/to/file.mp3"
            java.io.InputStream fileStream = getClass().getClassLoader()
                    .getResourceAsStream("static" + filePath);

            if (fileStream == null) {
                // Thử không có "static" prefix (nếu file đã ở root của resources)
                fileStream = getClass().getClassLoader()
                        .getResourceAsStream(filePath.substring(1)); // Bỏ dấu '/' đầu tiên
            }

            if (fileStream == null) {
                logger.warn("[{}] File not found: {} (tried: static{} and {})",
                        currentUser != null ? currentUser.getUsername() : "unknown",
                        filePath, filePath, filePath.substring(1));
                sendMessage("AUDIO_FILE_ERROR:" + filePath + ":File not found");
                return;
            }

            logger.info("[{}] Found file: {} (size: {} bytes)",
                    currentUser != null ? currentUser.getUsername() : "unknown",
                    filePath, fileStream.available());

            // Đọc toàn bộ file vào memory để biết size
            byte[] fileData = fileStream.readAllBytes();
            fileStream.close();

            // Tạo socket riêng cho binary transfer
            if (binaryTransferService == null) {
                logger.error("[{}] BinaryTransferService is null, cannot transfer file",
                        currentUser != null ? currentUser.getUsername() : "unknown");
                sendMessage("AUDIO_FILE_ERROR:" + filePath + ":BinaryTransferService not available");
                return;
            }

            // Đăng ký file để transfer qua port cố định
            // registerFileForTransfer sẽ normalize và trả về normalizedPath
            String normalizedPath = filePath.startsWith("/") ? filePath : "/" + filePath;
            int binaryPort = binaryTransferService.registerFileForTransfer(normalizedPath, fileData);

            // Gửi thông tin file sẵn sàng cho client qua text socket
            // Format: BINARY_READY:port:filePath:fileSize
            // Gửi normalizedPath để đảm bảo client có thể match với pendingFiles
            String readyMessage = "BINARY_READY:" + binaryPort + ":" + normalizedPath + ":" + fileData.length;
            logger.info("[{}] Sending BINARY_READY: {}",
                    currentUser != null ? currentUser.getUsername() : "unknown", readyMessage);
            sendMessage(readyMessage);

            logger.info("[{}] File registered for transfer on port {}: {} ({} bytes)",
                    currentUser != null ? currentUser.getUsername() : "unknown",
                    binaryPort, filePath, fileData.length);
        } catch (Exception e) {
            logger.error("Error sending audio file: " + filePath, e);
            sendMessage("AUDIO_FILE_ERROR:" + filePath + ":" + e.getMessage());
        }
    }

    private String resolvePendingArtist() {
        if (pendingArtist != null && !pendingArtist.isBlank()) {
            return pendingArtist;
        }
        if (opponentHandler != null && opponentHandler.pendingArtist != null
                && !opponentHandler.pendingArtist.isBlank()) {
            return opponentHandler.pendingArtist;
        }
        return null;
    }

    private String resolvePendingGenre() {
        if (pendingGenre != null && !pendingGenre.isBlank()) {
            return pendingGenre;
        }
        if (opponentHandler != null && opponentHandler.pendingGenre != null
                && !opponentHandler.pendingGenre.isBlank()) {
            return opponentHandler.pendingGenre;
        }
        return null;
    }

    private void clearPendingPreferences() {
        pendingArtist = null;
        pendingGenre = null;
    }

    private String buildFilterPayload(String artist, String genre, int totalRounds) {
        return encodeComponent(artist) + "||" + encodeComponent(genre) + "||" + totalRounds;
    }

    private String[] parseFilterPayload(String payload) {
        String safePayload = payload == null ? "" : payload;
        String[] parts = safePayload.split("\\|\\|", -1);
        String artist = parts.length > 0 ? decodeComponent(parts[0]) : null;
        String genre = parts.length > 1 ? decodeComponent(parts[1]) : null;
        String totalRoundsStr = parts.length > 2 ? parts[2] : null;
        return new String[] { normalizePreference(artist), normalizePreference(genre), totalRoundsStr };
    }

    private int parseTotalRounds(String value) {
        if (value == null || value.isEmpty()) {
            return DEFAULT_TOTAL_ROUNDS;
        }
        try {
            int rounds = Integer.parseInt(value);
            // Giới hạn từ 5 đến 50
            return Math.max(5, Math.min(50, rounds));
        } catch (NumberFormatException e) {
            return DEFAULT_TOTAL_ROUNDS;
        }
    }

    private String normalizePreference(String value) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String encodeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .map(this::encodeComponent)
                .collect(Collectors.joining(","));
    }

    private String encodeComponent(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private String decodeComponent(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    public synchronized void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            logger.debug("Gửi message: {}", message);
        }
    }

    private void cleanup() {
        try {
            if (currentUser != null) {
                // Nếu đang trong game và đối thủ còn tồn tại: không kết thúc game của đối thủ.
                // Đánh dấu bản thân đã hoàn tất, giữ đối thủ tiếp tục chơi.
                if (inGame && opponentHandler != null) {
                    try {
                        // Đánh dấu người rời đã xong
                        myFinished = true;
                        myRoundNumber = totalRounds + 1;
                        inGame = false;
                        currentRound = null;

                        // Thông báo cho đối thủ biết bạn đã rời, nhưng không kết thúc game của họ
                        opponentHandler.opponentHandler = null; // Mất tham chiếu đối thủ
                        opponentHandler.sendMessage("INFO:Đối thủ đã rời trận, bạn có thể tiếp tục");
                    } catch (Exception ignored) {
                    }
                }

                clientManager.removeClient(currentUser.getUsername());
                clientManager.broadcastToLobby("MSG_LOBBY:SYSTEM:" +
                        currentUser.getUsername() + " đã rời khỏi game");
                // Broadcast cập nhật lobby sau khi remove client
                clientManager.broadcastLobbyUpdate();
            }

            if (roundTimer != null) {
                roundTimer.cancel(false);
            }
            if (scheduler != null) {
                scheduler.shutdown();
            }

            // Reset các trạng thái prefetch
            myPlaylist = null;
            myPrefetchReady = false;
            roundsStarted = false;

            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();

            logger.info("Client đã ngắt kết nối: {}",
                    currentUser != null ? currentUser.getUsername() : "Unknown");
        } catch (IOException e) {
            logger.error("Lỗi cleanup client", e);
        }
    }

    // Getters
    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isInGame() {
        return inGame;
    }
}
