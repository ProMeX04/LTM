package com.promex04.controller;

import com.promex04.ClientConfig;
import com.promex04.model.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller quản lý tất cả logic game và giao tiếp với server qua Socket
 */
public class GameController {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private boolean connected = false;
    private java.util.concurrent.ScheduledExecutorService heartbeatScheduler;

    // State
    @Getter
    private String currentUsername;
    @Getter
    private int currentScore = 0;
    @Getter
    private ObservableList<User> lobbyUsers = FXCollections.observableArrayList();
    @Getter
    private ObservableList<String> chatMessages = FXCollections.observableArrayList();
    @Getter
    private ObservableList<RankingEntry> rankingList = FXCollections.observableArrayList();
    @Getter
    private ObservableList<String> availableArtists = FXCollections.observableArrayList();
    @Getter
    private ObservableList<String> availableGenres = FXCollections.observableArrayList();

    @Getter
    private GameRound currentRound;
    @Getter
    private int myScore = 0;
    @Getter
    private int opponentScore = 0;
    @Getter
    private boolean inGame = false;
    private final java.util.Map<String, String> audioCache = new java.util.concurrent.ConcurrentHashMap<>(); // serverPath
                                                                                                             // -> file:
                                                                                                             // URI
    private final java.util.Set<String> prefetchFailedPaths = java.util.concurrent.ConcurrentHashMap.newKeySet();
    // Map để đồng bộ nhận file: path -> {size, received, error}
    private final java.util.Map<String, java.util.Map<String, Object>> pendingFiles = new java.util.concurrent.ConcurrentHashMap<>();
    @Getter
    private int prefetchTotal = 0;
    @Getter
    private int prefetchCompleted = 0;
    @Getter
    private String prefetchCurrentPath;
    @Getter
    private long prefetchCurrentBytes = 0;
    @Getter
    private long prefetchCurrentTotalBytes = -1; // -1 nếu không biết
    @Getter
    private int prefetchCurrentPercent = 0; // 0..100 (nếu biết tổng)
    @Getter
    private javafx.collections.ObservableList<String> prefetchLogs = javafx.collections.FXCollections
            .observableArrayList();

    // Callbacks cho UI updates
    @Setter
    private Runnable onLoginSuccess;
    @Setter
    private Runnable onLoginFailed;
    @Setter
    private Runnable onRegisterSuccess;
    @Setter
    private Runnable onRegisterFailed;
    @Setter
    private Runnable onLobbyUpdate;
    @Setter
    private Runnable onChatUpdate;
    @Setter
    private Runnable onChallengeReceived;
    @Setter
    private Runnable onChallengeSent;
    @Setter
    private Runnable onChallengeRejected;
    @Setter
    private Runnable onGameStart;
    @Setter
    private Runnable onRoundStart;
    @Setter
    private Runnable onRoundResult;
    @Setter
    private Runnable onGameOver;
    @Setter
    private Runnable onScoreUpdate;
    @Setter
    private Runnable onProgressUpdate;
    @Setter
    private Runnable onError;
    @Setter
    private Runnable onRankingUpdate;
    @Setter
    private Runnable onLogout;
    @Setter
    private Runnable onLeftMatch;
    @Setter
    private Runnable onPrefetchStart;
    @Setter
    private Runnable onPrefetchDone;
    @Setter
    private Runnable onPrefetchProgress;
    @Setter
    private Runnable onGameReady;

    // Callback parameters
    @Getter
    private String challengeFromUsername;
    @Getter
    private String challengeToUsername;
    @Getter
    private String challengeRejectedBy;
    @Getter
    private String challengeArtist;
    @Getter
    private String challengeGenre;
    @Getter
    private String errorMessage;
    @Getter
    private String gameOverMessage;
    @Getter
    private int myProgress = 0;
    @Getter
    private int opponentProgress = 0;
    @Getter
    private boolean lastAnswerCorrect = false;
    @Getter
    private int lastAnswerPoints = 0;
    @Getter
    private long lastAnswerTimeMs = 0;
    @Getter
    private int lastSelectedAnswer = 0;

    public GameController() {
    }

    public void connect(String serverHost, int serverPort) throws IOException {
        // Đóng kết nối cũ nếu có
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        // Dừng thread listener cũ nếu có
        if (listenerThread != null && listenerThread.isAlive()) {
            // Thread sẽ tự dừng khi socket đóng
        }

        // Tạo kết nối mới
        socket = new Socket(serverHost, serverPort);
        
        // Tối ưu socket options để tăng tốc độ
        socket.setTcpNoDelay(true); // Tắt Nagle algorithm để giảm latency
        socket.setReceiveBufferSize(256 * 1024); // 256KB receive buffer
        socket.setSendBufferSize(256 * 1024); // 256KB send buffer
        
        out = new PrintWriter(socket.getOutputStream(), true);
        // Tăng buffer size của BufferedReader để giảm số lần đọc
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()), 65536); // 64KB buffer
        connected = true;

        // Bắt đầu thread lắng nghe server
        listenerThread = new Thread(this::listenToServer);
        listenerThread.setDaemon(true);
        listenerThread.start();

        // Khởi động heartbeat để giữ kết nối
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdownNow();
        }
        heartbeatScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                if (isConnected()) {
                    sendCommand("PING");
                }
            } catch (Exception ignored) {
            }
        }, 5, 25, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void listenToServer() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                handleServerMessage(message);
            }
        } catch (IOException e) {
            if (connected) {
                connected = false;
                if (heartbeatScheduler != null) {
                    heartbeatScheduler.shutdownNow();
                    heartbeatScheduler = null;
                }
                Platform.runLater(() -> {
                    if (onError != null) {
                        errorMessage = "Mất kết nối với server: " + e.getMessage();
                        onError.run();
                    }
                });
            }
        } finally {
            connected = false;
        }
    }

    private void handleServerMessage(String message) {
        Platform.runLater(() -> {
            // Xử lý LOBBY_UPDATE đặc biệt vì nó có thể chứa nhiều dấu ":"
            if (message.startsWith("LOBBY_UPDATE:")) {
                String lobbyData = message.substring("LOBBY_UPDATE:".length());
                handleLobbyUpdate(new String[] { "LOBBY_UPDATE", lobbyData });
                return;
            }

            // Xử lý đặc biệt cho các message có nhiều dấu ':'
            String[] parts;
            if (message.startsWith("ROUND_RESULT:")) {
                // ROUND_RESULT:correct/wrong/timeout:points:timeMs
                parts = message.split(":", 4);
            } else if (message.startsWith("GAME_OVER:")) {
                // GAME_OVER:myScore:opponentScore:winner
                parts = message.split(":", 4);
            } else if (message.startsWith("BINARY_READY:")) {
                // BINARY_READY:port:filePath:fileSize (filePath có thể chứa ":")
                // Cần split đủ để lấy tất cả các phần
                String[] tempParts = message.split(":", -1);
                if (tempParts.length >= 4) {
                    // parts[0] = "BINARY_READY"
                    // parts[1] = port
                    // parts[2..n-2] = filePath (có thể chứa ":")
                    // parts[n-1] = fileSize
                    parts = new String[4];
                    parts[0] = tempParts[0];
                    parts[1] = tempParts[1];
                    parts[2] = String.join(":", java.util.Arrays.copyOfRange(tempParts, 2, tempParts.length - 1));
                    parts[3] = tempParts[tempParts.length - 1];
                } else {
                    parts = tempParts;
                }
            } else {
                parts = message.split(":", 3);
            }
            if (parts.length == 0)
                return;

            String command = parts[0];

            try {
                switch (command) {
                    case "LOGIN_SUCCESS":
                        handleLoginSuccess(parts);
                        break;
                    case "LOGIN_FAILED":
                        errorMessage = parts.length > 1 ? parts[1] : "Đăng nhập thất bại";
                        if (onLoginFailed != null)
                            onLoginFailed.run();
                        break;
                    case "REGISTER_SUCCESS":
                        handleRegisterSuccess(parts);
                        break;
                    case "REGISTER_FAILED":
                        errorMessage = parts.length > 1 ? parts[1] : "Đăng ký thất bại";
                        if (onRegisterFailed != null)
                            onRegisterFailed.run();
                        break;
                    case "LOBBY_UPDATE":
                        handleLobbyUpdate(parts);
                        break;
                    case "MSG_LOBBY":
                        handleChatMessage(parts);
                        break;
                    case "CHALLENGE_REQUEST":
                        handleChallengeRequest(parts);
                        break;
                    case "CHALLENGE_ACCEPTED":
                        handleChallengeAccepted(parts);
                        break;
                    case "CHALLENGE_REJECTED":
                        handleChallengeRejected(parts);
                        break;
                    case "ROUND_START":
                        handleRoundStart(parts);
                        break;
                    case "PREFETCH":
                        handlePrefetch(parts);
                        break;
                    case "ROUND_RESULT":
                        handleRoundResult(parts);
                        break;
                    case "SCORE_UPDATE":
                        handleScoreUpdate(parts);
                        break;
                    case "PROGRESS":
                        handleProgress(parts);
                        break;
                    case "GAME_OVER":
                        handleGameOver(parts);
                        break;
                    case "RANKING":
                        handleRanking(parts);
                        break;
                    case "AUDIO_TAGS":
                        handleAudioTags(parts);
                        break;
                    case "PONG":
                        // Phản hồi heartbeat - bỏ qua
                        break;
                    case "WAITING_OPPONENT":
                        // Chờ đối thủ trả lời
                        break;
                    case "WAITING_OPPONENT_PREFETCH":
                        // Chờ đối thủ tải xong nhạc
                        if (parts.length >= 2) {
                            String opponentName = parts[1];
                            appendPrefetchLog("Đang chờ đối thủ " + opponentName + " tải xong nhạc...");
                        }
                        break;
                    case "ERROR":
                        errorMessage = parts.length > 1 ? parts[1] : "Unknown error";
                        if (onError != null)
                            onError.run();
                        break;
                    case "CHALLENGE_SENT":
                        handleChallengeSent(parts);
                        break;
                    case "CHALLENGE_FAILED":
                        // Đối thủ không sẵn sàng hoặc không tồn tại
                        errorMessage = parts.length > 1 ? parts[1] : "Không thể gửi thách đấu";
                        // Xóa trạng thái đã gửi nếu đang set
                        challengeToUsername = null;
                        if (onError != null)
                            onError.run();
                        break;
                    case "BINARY_READY":
                        handleBinaryReady(parts);
                        break;
                    case "AUDIO_FILE_ERROR":
                        handleAudioFileError(parts);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage = "Lỗi xử lý message: " + e.getMessage();
                if (onError != null)
                    onError.run();
            }
        });
    }

    private void handleLoginSuccess(String[] parts) {
        if (parts.length >= 3) {
            currentUsername = parts[1];
            currentScore = Integer.parseInt(parts[2]);
            if (onLoginSuccess != null)
                onLoginSuccess.run();
        }
    }

    private void handleRegisterSuccess(String[] parts) {
        if (parts.length >= 3) {
            currentUsername = parts[1];
            currentScore = Integer.parseInt(parts[2]);
            if (onRegisterSuccess != null)
                onRegisterSuccess.run();
        }
    }

    private void handleLobbyUpdate(String[] parts) {
        lobbyUsers.clear();
        if (parts.length >= 2 && !parts[1].isEmpty()) {
            String[] users = parts[1].split(";");
            for (String userStr : users) {
                if (!userStr.trim().isEmpty()) {
                    String[] userParts = userStr.split(",");
                    if (userParts.length >= 3) {
                        try {
                            String username = userParts[0];
                            int score = Integer.parseInt(userParts[1]);
                            String status = userParts[2];
                            // Chỉ hiển thị những người chơi không phải là mình
                            if (currentUsername == null || !username.equals(currentUsername)) {
                                lobbyUsers.add(new User(username, score, status));
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Lỗi parse user: " + userStr);
                        }
                    }
                }
            }
        }
        System.out.println("LOBBY_UPDATE nhận được: " + lobbyUsers.size() + " người chơi");
        if (onLobbyUpdate != null)
            onLobbyUpdate.run();
    }

    private void handleChatMessage(String[] parts) {
        if (parts.length >= 3) {
            String username = parts[1];
            String message = parts[2];
            // Không hiển thị thông báo SYSTEM về việc rời game
            if ("SYSTEM".equals(username) && message != null && message.contains("đã rời")) {
                return;
            }
            chatMessages.add(username + ": " + message);
            if (onChatUpdate != null)
                onChatUpdate.run();
        }
    }

    private void handleChallengeRequest(String[] parts) {
        if (parts.length >= 2) {
            challengeFromUsername = parts[1];
            String[] filters = parseFilterPayload(parts.length >= 3 ? parts[2] : "");
            challengeArtist = filters[0];
            challengeGenre = filters[1];
            // totalRounds được lưu trong filters[2] nhưng không cần hiển thị trong UI
            if (onChallengeReceived != null)
                onChallengeReceived.run();
        }
    }

    private void handleChallengeAccepted(String[] parts) {
        if (parts.length >= 2) {
            inGame = true;
            // Đã vào game, không còn trạng thái đã mời
            challengeToUsername = null;
            challengeFromUsername = null;
            challengeArtist = null;
            challengeGenre = null;
            if (onGameStart != null)
                onGameStart.run();
        }
    }

    private void handleChallengeRejected(String[] parts) {
        // Xử lý từ chối thách đấu: cập nhật người từ chối và callback UI
        challengeRejectedBy = parts.length >= 2 ? parts[1] : null;
        challengeToUsername = null;
        challengeFromUsername = null;
        challengeArtist = null;
        challengeGenre = null;
        if (onChallengeRejected != null)
            onChallengeRejected.run();
    }

    private void handleAudioTags(String[] parts) {
        String artistsPayload = parts.length >= 2 ? parts[1] : "";
        String genresPayload = parts.length >= 3 ? parts[2] : "";
        java.util.List<String> artists = decodeList(artistsPayload);
        java.util.List<String> genres = decodeList(genresPayload);
        availableArtists.setAll(artists);
        availableGenres.setAll(genres);
    }

    private void handleChallengeSent(String[] parts) {
        // Xác nhận đã gửi thách đấu đến đối thủ
        challengeToUsername = parts.length >= 2 ? parts[1] : null;
        if (onChallengeSent != null)
            onChallengeSent.run();
    }

    private void handleRoundStart(String[] parts) {
        // ROUND_START sử dụng delimiter "||" để tránh xung đột với dấu ':' trong option
        // names
        // Format:
        // ROUND_START:audioId||roundNumber||option1||option2||option3||option4||audioPath
        if (parts.length < 2) {
            System.err.println("[GameController] Invalid ROUND_START message: too short. Parts: "
                    + java.util.Arrays.toString(parts));
            return;
        }

        try {
            // Ghép lại tất cả các phần sau "ROUND_START:" (có thể có nhiều phần nếu dùng
            // delimiter cũ)
            StringBuilder dataBuilder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (dataBuilder.length() > 0) {
                    dataBuilder.append(':');
                }
                dataBuilder.append(parts[i]);
            }
            String data = dataBuilder.toString();

            // Thử split bằng "||" trước (format mới)
            String[] dataParts = data.split("\\|\\|", -1);

            // Nếu không có "||", có thể server chưa rebuild, thử format cũ với ":"
            if (dataParts.length == 1 && data.contains(":")) {
                System.out.println(
                        "[GameController] Warning: ROUND_START message doesn't contain '||', trying old format");
                dataParts = data.split(":", -1);
                // Format cũ: audioId:roundNumber:option1:option2:option3:option4:audioPath
                // Nhưng option có thể chứa ':', nên cần lấy từ cuối lên
                if (dataParts.length >= 7) {
                    Long audioId = Long.parseLong(dataParts[0]);
                    int roundNumber = Integer.parseInt(dataParts[1]);
                    String audioPath = dataParts[dataParts.length - 1];
                    String option4 = dataParts[dataParts.length - 2];
                    String option3 = dataParts[dataParts.length - 3];
                    String option2 = dataParts[dataParts.length - 4];
                    // Option1 có thể chứa ':', ghép lại
                    StringBuilder option1Builder = new StringBuilder();
                    for (int i = 2; i < dataParts.length - 4; i++) {
                        if (option1Builder.length() > 0)
                            option1Builder.append(':');
                        option1Builder.append(dataParts[i]);
                    }
                    String option1 = option1Builder.toString();

                    currentRound = new GameRound(audioId, roundNumber, option1, option2, option3, option4, audioPath);
                    System.out.println("[GameController] ROUND_START received (old format): round " + roundNumber);

                    if (roundNumber == 1 && onGameReady != null) {
                        System.out.println("[GameController] Calling onGameReady for first round");
                        onGameReady.run();
                    }
                    if (onRoundStart != null) {
                        System.out.println("[GameController] Calling onRoundStart");
                        onRoundStart.run();
                    }
                    return;
                }
            }

            if (dataParts.length < 7) {
                System.err.println("[GameController] Invalid ROUND_START message: not enough parts after split. Got "
                        + dataParts.length + " parts");
                System.err.println("[GameController] Full message: " + java.util.Arrays.toString(parts));
                System.err.println("[GameController] Data: " + data);
                return;
            }

            Long audioId = Long.parseLong(dataParts[0]);
            int roundNumber = Integer.parseInt(dataParts[1]);
            String option1 = dataParts[2];
            String option2 = dataParts[3];
            String option3 = dataParts[4];
            String option4 = dataParts[5];
            String audioPath = dataParts[6];

            currentRound = new GameRound(audioId, roundNumber, option1, option2, option3, option4, audioPath);
            System.out.println("[GameController] ROUND_START received: round " + roundNumber);

            // Chỉ gọi onGameReady cho round đầu tiên (để chuyển từ loading sang game view)
            if (roundNumber == 1 && onGameReady != null) {
                System.out.println("[GameController] Calling onGameReady for first round");
                onGameReady.run();
            }

            // Luôn gọi onRoundStart để cập nhật UI
            if (onRoundStart != null) {
                System.out.println("[GameController] Calling onRoundStart");
                onRoundStart.run();
            }
        } catch (Exception e) {
            System.err.println("[GameController] Error parsing ROUND_START message: " + e.getMessage());
            System.err.println("[GameController] Message parts: " + java.util.Arrays.toString(parts));
            e.printStackTrace();
        }
    }

    private void handlePrefetch(String[] parts) {
        // parts[1] is semicolon-separated server-relative paths
        if (parts.length < 2)
            return;
        String list = parts[1];
        String[] paths = list.isEmpty() ? new String[0] : list.split(";");
        appendPrefetchLog(String.format("Nhận danh sách PREFETCH: %d tệp", paths.length));

        // Start background download
        if (onPrefetchStart != null)
            onPrefetchStart.run();
        prefetchTotal = paths.length;
        prefetchCompleted = 0;
        prefetchLogs.clear();
        prefetchFailedPaths.clear();

        new Thread(() -> {
            boolean hasError = false;

            // Nếu không có file nào cần tải, gửi PREFETCH_DONE ngay
            if (paths.length == 0) {
                appendPrefetchLog("Không có file nào cần tải, gửi PREFETCH_DONE ngay...");
                sendCommand("PREFETCH_DONE");
                if (onPrefetchDone != null) {
                    javafx.application.Platform.runLater(onPrefetchDone);
                }
                return;
            }

            for (int i = 0; i < paths.length; i++) {
                String serverPath = paths[i];
                String normalizedPath = normalizeServerPath(serverPath);
                String fileName = extractFileName(serverPath);
                appendPrefetchLog(String.format("[%d/%d] Bắt đầu tải: %s", i + 1, prefetchTotal, fileName));
                
                try {
                    // Tạo fileInfo trước để đảm bảo nó tồn tại khi handleBinaryReady được gọi
                    Object lock = new Object();
                    java.util.Map<String, Object> fileInfo = new java.util.HashMap<>();
                    fileInfo.put("port", -1);
                    fileInfo.put("size", 0L);
                    fileInfo.put("filePath", null);
                    fileInfo.put("received", false);
                    fileInfo.put("error", null);
                    fileInfo.put("lock", lock);
                    pendingFiles.put(normalizedPath, fileInfo);
                    
                    // Gửi yêu cầu file qua Socket
                    sendCommand("REQUEST_AUDIO_FILE:" + normalizedPath);
                    
                    // Đợi nhận BINARY_READY message từ listener thread
                    synchronized (lock) {
                        // Đợi tối đa 30 giây để nhận port
                        long startTime = System.currentTimeMillis();
                        while ((Integer) fileInfo.get("port") == -1 && 
                               fileInfo.get("error") == null && 
                               (System.currentTimeMillis() - startTime) < 30000) {
                            try {
                                lock.wait(1000); // Đợi 1 giây mỗi lần
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                    
                    if (fileInfo.get("error") != null) {
                        throw (Exception) fileInfo.get("error");
                    }
                    
                    int binaryPort = (Integer) fileInfo.get("port");
                    long fileSize = (Long) fileInfo.get("size");
                    String filePath = (String) fileInfo.get("filePath");
                    
                    if (binaryPort == -1) {
                        throw new java.net.SocketTimeoutException("Timeout waiting for binary port: " + fileName);
                    }
                    
                    if (filePath == null) {
                        throw new java.net.SocketTimeoutException("File path not received: " + fileName);
                    }
                    
                    // Kết nối đến binary socket cố định và nhận file
                    // Lấy host từ socket hiện tại
                    String serverHost = socket.getInetAddress().getHostName();
                    // Nếu là localhost, dùng IP address
                    if (serverHost.equals("localhost") || serverHost.equals("127.0.0.1")) {
                        serverHost = socket.getInetAddress().getHostAddress();
                    }
                    byte[] fileData = BinaryTransferClient.receiveFile(serverHost, binaryPort, filePath, fileSize);
                    
                    // Lưu file vào temp và cache
                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("audio_cache_", ".mp3");
                    java.nio.file.Files.write(tmp, fileData);
                    
                    if (normalizedPath != null && !normalizedPath.isEmpty()) {
                        audioCache.put(normalizedPath, tmp.toUri().toString());
                        prefetchFailedPaths.remove(normalizedPath);
                    }
                    
                    appendPrefetchLog(String.format("[%d/%d] ✓ Xong: %s (%.1f KB)", i + 1, prefetchTotal, fileName,
                            fileSize / 1024.0));
                    
                    // Xóa khỏi pendingFiles
                    pendingFiles.remove(normalizedPath);
                    
                } catch (java.net.SocketTimeoutException ex) {
                    hasError = true;
                    if (normalizedPath != null && !normalizedPath.isEmpty()) {
                        prefetchFailedPaths.add(normalizedPath);
                    }
                    appendPrefetchLog(String.format("[%d/%d] ✗ LỖI timeout %s: Quá thời gian chờ", i + 1, prefetchTotal,
                            fileName));
                    ex.printStackTrace();
                } catch (Exception ex) {
                    hasError = true;
                    if (normalizedPath != null && !normalizedPath.isEmpty()) {
                        prefetchFailedPaths.add(normalizedPath);
                    }
                    appendPrefetchLog(String.format("[%d/%d] ✗ LỖI tải %s: %s (%s)", i + 1, prefetchTotal, fileName,
                            ex.getClass().getSimpleName(), ex.getMessage()));
                    ex.printStackTrace();
                }

                prefetchCompleted = i + 1;
                if (onPrefetchProgress != null)
                    javafx.application.Platform.runLater(onPrefetchProgress);
            }

            // Thông báo kết quả
            if (hasError) {
                appendPrefetchLog("⚠ Có một số file tải lỗi, nhưng vẫn tiếp tục game...");
            } else {
                appendPrefetchLog("✓ Tất cả file đã tải thành công!");
            }

            // Notify server ready (vẫn gửi ngay cả khi có lỗi để game có thể tiếp tục)
            appendPrefetchLog("→ Gửi PREFETCH_DONE đến server...");
            System.out.println("[GameController] Sending PREFETCH_DONE to server");
            sendCommand("PREFETCH_DONE");
            if (onPrefetchDone != null) {
                javafx.application.Platform.runLater(onPrefetchDone);
            }
        }, "prefetch-thread").start();
    }

    private void appendPrefetchLog(String line) {
        System.out.println("[PREFETCH] " + line);
        javafx.application.Platform.runLater(() -> prefetchLogs.add(line));
    }

    private String extractFileName(String serverPath) {
        if (serverPath == null)
            return "(unknown)";
        int idx = serverPath.lastIndexOf('/') + 1;
        if (idx <= 0 || idx >= serverPath.length())
            return serverPath;
        return serverPath.substring(idx);
    }

    private String buildHttpUrl(String serverPath) {
        String normalized = normalizeServerPath(serverPath);
        if (normalized == null || normalized.isEmpty()) {
            return ClientConfig.getHttpBaseUrl();
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        try {
            java.net.URI uri = new java.net.URI(
                    "http",
                    null,
                    ClientConfig.getDefaultHost(),
                    ClientConfig.getDefaultHttpPort(),
                    normalized,
                    null,
                    null);
            String url = uri.toASCIIString();
            // Sửa lỗi '//' sau http://
            url = url.replace(":///", "://");
            return url;
        } catch (Exception e) {
            String baseUrl = ClientConfig.getHttpBaseUrl();
            String encoded = encodePath(normalized);
            while (encoded.startsWith("//")) {
                encoded = encoded.substring(1);
            }
            // Loại bỏ '/' thừa ở cuối baseUrl và đầu encoded
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            if (encoded.startsWith("/")) {
                encoded = encoded.substring(1);
            }
            String url = baseUrl + "/" + encoded;
            // Sửa lỗi '//' sau http://
            url = url.replace(":///", "://");
            return url;
        }
    }

    private String encodePath(String path) {
        if (path == null || path.isEmpty())
            return "/";
        String[] parts = path.split("/", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String seg = parts[i];
            if (i == 0) {
                if (seg.isEmpty()) {
                    sb.append('/');
                    continue;
                }
            } else {
                sb.append('/');
            }
            if (seg.isEmpty())
                continue;
            try {
                String enc = java.net.URLEncoder.encode(seg, java.nio.charset.StandardCharsets.UTF_8)
                        .replace("+", "%20");
                sb.append(enc);
            } catch (Exception ignored) {
                sb.append(seg);
            }
        }
        return sb.toString();
    }

    private String normalizeServerPath(String path) {
        if (path == null)
            return null;
        String trimmed = path.trim();
        if (trimmed.isEmpty())
            return "";
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file:")) {
            return trimmed;
        }
        String normalized = trimmed.replace('\\', '/');
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    // Trả về nguồn media từ cache (file:) nếu có, nếu không thì null
    public String getResolvedAudioSource(String serverPath) {
        if (serverPath == null || serverPath.isBlank())
            return null;
        String normalizedPath = normalizeServerPath(serverPath);
        if (normalizedPath == null || normalizedPath.isEmpty())
            return null;
        String cached = audioCache.get(normalizedPath);
        if (cached != null) {
            System.out.println("[GameController] ✓ Sử dụng file cache: " + normalizedPath + " -> " + cached);
            return cached; // file: URI
        }
        // Không có trong cache - file chưa được prefetch, trả về null
        System.out.println("[GameController] ⚠ File chưa có trong cache: " + normalizedPath);
        return null;
    }

    public void invalidateCachedAudio(String serverPath) {
        String normalizedPath = normalizeServerPath(serverPath);
        if (normalizedPath != null && !normalizedPath.isEmpty()) {
            audioCache.remove(normalizedPath);
        }
    }

    public boolean hasPrefetchFailures() {
        return !prefetchFailedPaths.isEmpty();
    }

    private void handleRoundResult(String[] parts) {
        System.out.println("[GameController] ROUND_RESULT received: " + java.util.Arrays.toString(parts));
        
        // Format: ROUND_RESULT:result:points:timeMs
        // Có thể có 3 hoặc 4 parts tùy vào cách split
        if (parts.length >= 3) {
            String result = parts[1]; // "correct", "wrong", hoặc "timeout"
            
            // Parse points và timeMs
            int points = 0;
            long timeMs = 0;
            
            if (parts.length >= 4) {
                // Format chuẩn: parts[2] = points, parts[3] = timeMs
                try {
                    points = Integer.parseInt(parts[2]);
                    timeMs = Long.parseLong(parts[3]);
                } catch (NumberFormatException e) {
                    System.err.println("[GameController] Error parsing points/timeMs: " + e.getMessage());
                }
            } else if (parts.length == 3) {
                // Format có thể là: ROUND_RESULT:wrong:0:304 (bị split thành 3 parts)
                // parts[2] = "0:304", cần split thêm
                String[] timeParts = parts[2].split(":");
                if (timeParts.length >= 2) {
                    try {
                        points = Integer.parseInt(timeParts[0]);
                        timeMs = Long.parseLong(timeParts[1]);
                    } catch (NumberFormatException e) {
                        System.err.println("[GameController] Error parsing points/timeMs from combined string: " + e.getMessage());
                    }
                }
            }
            
            lastAnswerCorrect = "correct".equals(result);
            lastAnswerPoints = points;
            lastAnswerTimeMs = timeMs;
            
            // Nếu là timeout, đánh dấu là sai và reset selectedAnswer
            if ("timeout".equals(result)) {
                lastAnswerCorrect = false;
                lastSelectedAnswer = 0;
            }
            
            System.out.println("[GameController] Parsed result - Correct: " + lastAnswerCorrect + ", Points: " + lastAnswerPoints + ", TimeMs: " + lastAnswerTimeMs + ", Selected: " + lastSelectedAnswer);
            
            if (onRoundResult != null) {
                System.out.println("[GameController] Calling onRoundResult callback");
                onRoundResult.run();
            } else {
                System.out.println("[GameController] WARNING: onRoundResult callback is null!");
            }
        } else {
            System.out.println("[GameController] ERROR: ROUND_RESULT message has insufficient parts: " + parts.length);
        }
    }

    private void handleScoreUpdate(String[] parts) {
        if (parts.length >= 3) {
            myScore = Integer.parseInt(parts[1]);
            opponentScore = Integer.parseInt(parts[2]);
            System.out.println("[GameController] SCORE_UPDATE received - My: " + myScore + ", Opponent: " + opponentScore);
            if (onScoreUpdate != null)
                onScoreUpdate.run();
        } else {
            System.err.println("[GameController] Invalid SCORE_UPDATE message: " + java.util.Arrays.toString(parts));
        }
    }

    private void handleProgress(String[] parts) {
        if (parts.length >= 3) {
            myProgress = Integer.parseInt(parts[1]);
            opponentProgress = Integer.parseInt(parts[2]);
            if (onProgressUpdate != null)
                onProgressUpdate.run();
        }
    }

    private void handleGameOver(String[] parts) {
        System.out.println("[GameController] handleGameOver được gọi với parts: " + java.util.Arrays.toString(parts));
        if (parts.length >= 4) {
            try {
                int myFinalScore = Integer.parseInt(parts[1]);
                int opponentFinalScore = Integer.parseInt(parts[2]);
                String winner = parts[3];

                System.out.println("[GameController] GAME_OVER - MyScore: " + myFinalScore + ", OpponentScore: " + opponentFinalScore + ", Winner: " + winner);

                myScore = myFinalScore;
                opponentScore = opponentFinalScore;

                if ("draw".equals(winner)) {
                    gameOverMessage = "Hòa! " + myScore + " - " + opponentScore;
                } else if (winner.equals(currentUsername)) {
                    gameOverMessage = "Bạn thắng! " + myScore + " - " + opponentScore;
                } else {
                    gameOverMessage = "Bạn thua! " + myScore + " - " + opponentScore;
                }

                System.out.println("[GameController] GameOverMessage: " + gameOverMessage);

                inGame = false;
                // Kết thúc trận, clear trạng thái mời
                challengeToUsername = null;
                if (onGameOver != null) {
                    System.out.println("[GameController] Gọi callback onGameOver");
                    onGameOver.run();
                } else {
                    System.err.println("[GameController] WARNING: onGameOver callback is null!");
                }
            } catch (Exception e) {
                System.err.println("[GameController] Lỗi xử lý GAME_OVER: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("[GameController] GAME_OVER message không đủ parts: " + parts.length);
        }
    }

    private void handleRanking(String[] parts) {
        rankingList.clear();
        if (parts.length >= 2 && !parts[1].isEmpty()) {
            String[] entries = parts[1].split(";");
            for (String entryStr : entries) {
                String[] entryParts = entryStr.split(",");
                if (entryParts.length >= 5) {
                    int rank = Integer.parseInt(entryParts[0]);
                    String username = entryParts[1];
                    int totalScore = Integer.parseInt(entryParts[2]);
                    int correctAnswers = Integer.parseInt(entryParts[3]);
                    int gamesWon = Integer.parseInt(entryParts[4]);
                    rankingList.add(new RankingEntry(rank, username, totalScore, correctAnswers, gamesWon));
                }
            }
        }
        if (onRankingUpdate != null)
            onRankingUpdate.run();
    }

    private void handleBinaryReady(String[] parts) {
        // Format: BINARY_READY:port:filePath:fileSize
        // parts đã được normalize thành 4 phần: [0]=command, [1]=port, [2]=filePath, [3]=fileSize
        if (parts.length < 4) {
            System.err.println("[GameController] Invalid BINARY_READY message: parts.length=" + parts.length);
            System.err.println("[GameController] Message parts: " + java.util.Arrays.toString(parts));
            return;
        }

        try {
            int port = Integer.parseInt(parts[1]);
            String filePath = parts[2]; // filePath đã được normalize
            long fileSize = Long.parseLong(parts[3]);
            
            System.out.println("[GameController] Received BINARY_READY: port=" + port + ", file=" + filePath + ", size=" + fileSize);

            // Server đã gửi normalizedPath, nhưng client có thể đã normalize khác khi tạo fileInfo
            // Thử lookup với cả filePath gốc và normalizedPath
            String normalizedPath = normalizeServerPath(filePath);
            System.out.println("[GameController] Server filePath: " + filePath);
            System.out.println("[GameController] Client normalized: " + normalizedPath);
            System.out.println("[GameController] Pending files keys: " + pendingFiles.keySet());
            
            // Thử lookup với normalizedPath trước (cách client đã tạo)
            java.util.Map<String, Object> fileInfo = pendingFiles.get(normalizedPath);
            
            // Nếu không tìm thấy, thử với filePath gốc từ server
            if (fileInfo == null && !filePath.equals(normalizedPath)) {
                fileInfo = pendingFiles.get(filePath);
                if (fileInfo != null) {
                    // Cập nhật key trong pendingFiles để đồng bộ
                    pendingFiles.remove(filePath);
                    pendingFiles.put(normalizedPath, fileInfo);
                }
            }
            
            if (fileInfo == null) {
                System.err.println("[GameController] No pending file info for: " + normalizedPath + " or " + filePath);
                System.err.println("[GameController] Available keys: " + pendingFiles.keySet());
                return;
            }
            
            // Cập nhật thông tin port, size và filePath
            fileInfo.put("port", port);
            fileInfo.put("size", fileSize);
            fileInfo.put("filePath", filePath);
            
            // Notify prefetch thread đang đợi
            Object lock = fileInfo.get("lock");
            synchronized (lock) {
                lock.notifyAll();
            }
        } catch (Exception e) {
            System.err.println("[GameController] Error parsing BINARY_READY message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleAudioFileError(String[] parts) {
        // Format: AUDIO_FILE_ERROR:path:errorMessage
        if (parts.length >= 2) {
            String filePath = parts[1];
            String errorMsg = parts.length >= 3 ? parts[2] : "Unknown error";
            
            java.util.Map<String, Object> fileInfo = pendingFiles.get(filePath);
            if (fileInfo != null) {
                fileInfo.put("error", new Exception(errorMsg));
                synchronized (fileInfo.get("lock")) {
                    fileInfo.notifyAll();
                }
            }
            
            System.err.println("[GameController] Audio file error: " + filePath + " - " + errorMsg);
        }
    }

    // Public methods để gửi commands đến server
    public void login(String username, String password) {
        sendCommand("LOGIN:" + username + ":" + password);
    }

    public void register(String username, String password) {
        sendCommand("REGISTER:" + username + ":" + password);
    }

    public void sendChatMessage(String message) {
        sendCommand("CHAT_LOBBY:" + message);
    }

    public void challenge(String opponentUsername) {
        challenge(opponentUsername, null, null, 15);
    }

    public void challenge(String opponentUsername, String artist, String genre) {
        challenge(opponentUsername, artist, genre, 15);
    }

    public void challenge(String opponentUsername, String artist, String genre, int totalRounds) {
        String payload = buildFilterPayload(artist, genre, totalRounds);
        sendCommand("CHALLENGE:" + opponentUsername + ":" + payload);
    }

    public void respondToChallenge(boolean accept) {
        sendCommand("CHALLENGE_RESPONSE:" + (accept ? "accept" : "reject"));
    }

    public void submitAnswer(int answerIndex) {
        if (currentRound != null) {
            lastSelectedAnswer = answerIndex;
            sendCommand("GAME_SUBMIT:" + answerIndex);
        }
    }

    public void leaveGame() {
        if (inGame) {
            sendCommand("LEAVE_GAME");
        }
        // Điều hướng về lobby ngay lập tức cho trải nghiệm tốt hơn
        Platform.runLater(() -> {
            if (onLeftMatch != null)
                onLeftMatch.run();
        });
    }

    public void requestRanking() {
        sendCommand("GET_RANKING");
    }

    public void requestAudioTags() {
        sendCommand("LIST_AUDIO_TAGS");
    }

    private String buildFilterPayload(String artist, String genre, int totalRounds) {
        return encodeComponent(artist) + "||" + encodeComponent(genre) + "||" + totalRounds;
    }

    private String[] parseFilterPayload(String payload) {
        String safePayload = payload == null ? "" : payload;
        String[] parts = safePayload.split("\\|\\|", -1);
        String artist = parts.length > 0 ? decodeComponent(parts[0]) : null;
        String genre = parts.length > 1 ? decodeComponent(parts[1]) : null;
        int totalRounds = parts.length > 2 ? parseTotalRounds(parts[2]) : 15;
        return new String[] { normalizePreference(artist), normalizePreference(genre), String.valueOf(totalRounds) };
    }

    private int parseTotalRounds(String value) {
        try {
            int rounds = Integer.parseInt(value);
            // Giới hạn từ 5 đến 50
            return Math.max(5, Math.min(50, rounds));
        } catch (NumberFormatException e) {
            return 15; // Mặc định
        }
    }

    private java.util.List<String> decodeList(String payload) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (payload == null || payload.isEmpty()) {
            return result;
        }
        String[] parts = payload.split(",");
        for (String part : parts) {
            String value = normalizePreference(decodeComponent(part));
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private String encodeComponent(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8);
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

    private String normalizePreference(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void sendCommand(String command) {
        if (out != null) {
            out.println(command);
        }
    }

    public void disconnect() {
        connected = false;
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
            heartbeatScheduler = null;
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    // Getters and setters are generated by Lombok

    // Đăng xuất: ngắt kết nối, reset state và callback về màn hình đăng nhập
    public void logout() {
        disconnect();

        currentUsername = null;
        currentScore = 0;
        myScore = 0;
        opponentScore = 0;
        inGame = false;
        currentRound = null;

        lobbyUsers.clear();
        chatMessages.clear();
        rankingList.clear();
        availableArtists.clear();
        availableGenres.clear();

        if (onLogout != null) {
            onLogout.run();
        }
    }
}
