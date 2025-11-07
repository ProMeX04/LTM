package com.promex04.service;

import com.promex04.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý các client đang kết nối
 */
@Service
public class ClientManager {
    private static final Logger logger = LoggerFactory.getLogger(ClientManager.class);
    private final Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void addClient(String username, ClientHandler handler) {
        connectedClients.put(username, handler);
    }

    public void removeClient(String username) {
        connectedClients.remove(username);
    }

    public ClientHandler getClient(String username) {
        return connectedClients.get(username);
    }

    public Map<String, ClientHandler> getAllClients() {
        return connectedClients;
    }

    public String getLobbyStatus() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ClientHandler> entry : connectedClients.entrySet()) {
            ClientHandler handler = entry.getValue();
            User user = handler.getCurrentUser();
            if (user != null) {
                String status = handler.isInGame() ? "bận" : "rỗi";
                if (sb.length() > 0)
                    sb.append(";");
                sb.append(user.getUsername()).append(",")
                        .append(user.getTotalScore()).append(",")
                        .append(status);
            }
        }
        return sb.toString();
    }

    public void broadcastToLobby(String message) {
        for (ClientHandler handler : connectedClients.values()) {
            if (!handler.isInGame()) {
                handler.sendMessage(message);
            }
        }
    }

    /**
     * Broadcast bảng xếp hạng đến tất cả clients đang kết nối
     */
    public void broadcastRanking() {
        if (userService == null) {
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

        String rankingMessage = sb.toString();
        for (ClientHandler handler : connectedClients.values()) {
            handler.sendMessage(rankingMessage);
        }
    }

    /**
     * Broadcast cập nhật danh sách lobby đến tất cả clients đang kết nối
     */
    public void broadcastLobbyUpdate() {
        String lobbyStatus = getLobbyStatus();
        String message = "LOBBY_UPDATE:" + lobbyStatus;
        logger.info("Broadcasting LOBBY_UPDATE to {} clients: {}", connectedClients.size(), lobbyStatus);
        int sentCount = 0;
        for (ClientHandler handler : connectedClients.values()) {
            if (handler != null && handler.getCurrentUser() != null) {
                handler.sendMessage(message);
                sentCount++;
            }
        }
        logger.info("Sent LOBBY_UPDATE to {} clients", sentCount);
    }
}
