package com.promex04.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import jakarta.annotation.PreDestroy;

/**
 * Service quản lý ServerSocket và chấp nhận các kết nối client mới
 */
@Service
public class SocketServerService implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SocketServerService.class);
    
    private final ClientManager clientManager;
    private final UserService userService;
    private final GameService gameService;
    private final BinaryTransferService binaryTransferService;
    
    @Value("${game.server.port:8888}")
    private int serverPort;
    
    private ServerSocket serverSocket;
    private boolean running = false;

    @Autowired
    public SocketServerService(ClientManager clientManager, UserService userService, 
            GameService gameService, BinaryTransferService binaryTransferService) {
        this.clientManager = clientManager;
        this.userService = userService;
        this.gameService = gameService;
        this.binaryTransferService = binaryTransferService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        startServer();
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(serverPort);
            // Tối ưu ServerSocket để tăng tốc độ
            serverSocket.setReceiveBufferSize(256 * 1024); // 256KB receive buffer
            running = true;
            logger.info("Socket Server đã khởi động trên port: {}", serverPort);
            
            // Bắt đầu thread chấp nhận kết nối
            new Thread(this::acceptConnections).start();
        } catch (IOException e) {
            logger.error("Lỗi khởi động server socket", e);
        }
    }

    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                // Tối ưu socket options để tăng tốc độ
                clientSocket.setTcpNoDelay(true); // Tắt Nagle algorithm để giảm latency
                clientSocket.setReceiveBufferSize(256 * 1024); // 256KB receive buffer
                clientSocket.setSendBufferSize(256 * 1024); // 256KB send buffer
                
                logger.info("Client kết nối từ: {}", clientSocket.getRemoteSocketAddress());
                
                // Tạo ClientHandler với Spring DI
                ClientHandler handler = new ClientHandler(
                    clientSocket, 
                    clientManager, 
                    userService, 
                    gameService
                );
                
                // Set binary transfer service
                handler.setBinaryTransferService(binaryTransferService);
                
                // Bắt đầu thread xử lý client
                new Thread(handler).start();
            } catch (IOException e) {
                if (running) {
                    logger.error("Lỗi chấp nhận kết nối", e);
                }
            }
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Lỗi đóng server socket", e);
        }
    }

    @PreDestroy
    public void onShutdown() {
        logger.info("Shutting down Socket Server...");
        stopServer();
    }
}
