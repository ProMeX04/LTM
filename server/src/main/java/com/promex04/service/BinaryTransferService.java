package com.promex04.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import jakarta.annotation.PreDestroy;

/**
 * Service quản lý socket cố định cho binary file transfer
 * Tất cả file transfer đều qua một port cố định để dễ cấu hình firewall
 */
@Service
public class BinaryTransferService implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(BinaryTransferService.class);
    
    @Value("${binary.transfer.port:7777}")
    private int binaryTransferPort;
    
    private ServerSocket serverSocket;
    private boolean running = false;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // Map để lưu file data: filePath -> FileData
    private final ConcurrentHashMap<String, FileData> fileCache = new ConcurrentHashMap<>();
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        startBinaryTransferServer();
    }
    
    /**
     * Khởi động binary transfer server trên port cố định
     */
    private void startBinaryTransferServer() {
        try {
            serverSocket = new ServerSocket(binaryTransferPort);
            serverSocket.setReceiveBufferSize(256 * 1024); // 256KB receive buffer
            running = true;
            logger.info("Binary Transfer Server đã khởi động trên port: {}", binaryTransferPort);
            
            // Bắt đầu thread chấp nhận kết nối
            executorService.submit(this::acceptConnections);
        } catch (IOException e) {
            logger.error("Lỗi khởi động Binary Transfer Server trên port {}", binaryTransferPort, e);
        }
    }
    
    /**
     * Chấp nhận các kết nối từ client
     */
    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                // Tối ưu socket options
                clientSocket.setTcpNoDelay(true);
                clientSocket.setReceiveBufferSize(256 * 1024);
                clientSocket.setSendBufferSize(256 * 1024);
                
                logger.debug("Binary transfer client connected from: {}", clientSocket.getRemoteSocketAddress());
                
                // Xử lý mỗi kết nối trong thread riêng
                executorService.submit(() -> handleClientConnection(clientSocket));
            } catch (IOException e) {
                if (running) {
                    logger.error("Lỗi chấp nhận kết nối binary transfer", e);
                }
            }
        }
    }
    
    /**
     * Xử lý kết nối từ client: đọc file path và gửi file về
     */
    private void handleClientConnection(Socket clientSocket) {
        try {
            // Đặt timeout cho socket để tránh đợi vô hạn
            clientSocket.setSoTimeout(5000); // 5 giây timeout
            
            // Đọc file path từ client (UTF-8, kết thúc bằng \n)
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            
            String filePath = reader.readLine();
            if (filePath == null || filePath.isEmpty()) {
                logger.warn("Client không gửi file path hoặc timeout khi đọc");
                clientSocket.close();
                return;
            }
            
            // Bỏ timeout sau khi đọc xong filePath để có thể đọc binary data không giới hạn
            clientSocket.setSoTimeout(0);
            
            // Normalize filePath để đảm bảo khớp với key trong cache
            String normalizedPath = filePath.startsWith("/") ? filePath : "/" + filePath;
            logger.info("Client requested file: {} (normalized: {}) from {}", filePath, normalizedPath, 
                clientSocket.getRemoteSocketAddress());
            
            // Tìm file trong cache với normalizedPath
            FileData fileData = fileCache.get(normalizedPath);
            if (fileData == null) {
                logger.error("File không tìm thấy trong cache: {} (normalized: {}). Available keys: {}", 
                    filePath, normalizedPath, fileCache.keySet());
                clientSocket.close();
                return;
            }
            
            logger.info("Found file in cache: {} ({} bytes)", normalizedPath, fileData.data.length);
            
            logger.info("Sending file: {} ({} bytes) to {}", normalizedPath, fileData.data.length, 
                clientSocket.getRemoteSocketAddress());
            
            // Gửi file data
            java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(
                clientSocket.getOutputStream(), 65536); // 64KB buffer
            
            long startTime = System.currentTimeMillis();
            int chunkSize = 65536; // 64KB chunks
            int offset = 0;
            while (offset < fileData.data.length) {
                int remaining = fileData.data.length - offset;
                int toWrite = Math.min(chunkSize, remaining);
                out.write(fileData.data, offset, toWrite);
                offset += toWrite;
            }
            out.flush();
            
            long elapsed = System.currentTimeMillis() - startTime;
            double speed = fileData.data.length / 1024.0 / (elapsed / 1000.0);
            logger.info("Binary transfer completed: {} bytes in {} ms ({:.1f} KB/s) for file: {}", 
                fileData.data.length, elapsed, speed, normalizedPath);
            
            // KHÔNG xóa file khỏi cache ngay sau khi gửi
            // File sẽ được overwrite khi có file mới với cùng path, hoặc được xóa khi game kết thúc
            // Điều này đảm bảo client có thể retry nếu cần và tránh race condition
            
        } catch (IOException e) {
            logger.error("Error handling binary transfer connection", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    /**
     * Đăng ký file để transfer (thay vì tạo socket mới)
     * @param filePath Đường dẫn file (đã được normalize)
     * @param fileData Dữ liệu file
     * @return Port cố định (luôn là binaryTransferPort)
     */
    public int registerFileForTransfer(String filePath, byte[] fileData) {
        // Đảm bảo file path bắt đầu bằng '/'
        String normalizedPath = filePath.startsWith("/") ? filePath : "/" + filePath;
        
        // Lưu file vào cache với normalizedPath làm key
        // Nếu đã có file với cùng path, sẽ overwrite (điều này OK vì file mới sẽ thay thế file cũ)
        FileData oldData = fileCache.put(normalizedPath, new FileData(normalizedPath, fileData));
        if (oldData != null) {
            logger.warn("Overwriting existing file in cache: {} (old size: {} bytes, new size: {} bytes)", 
                normalizedPath, oldData.data.length, fileData.length);
        }
        
        logger.info("File registered for transfer: {} ({} bytes) on port {} (cache size: {})", 
            normalizedPath, fileData.length, binaryTransferPort, fileCache.size());
        
        return binaryTransferPort;
    }
    
    /**
     * Lấy normalized path từ filePath (để đảm bảo consistency)
     */
    public String getNormalizedPath(String filePath) {
        return filePath.startsWith("/") ? filePath : "/" + filePath;
    }
    
    /**
     * Lấy port cố định
     */
    public int getBinaryTransferPort() {
        return binaryTransferPort;
    }
    
    @PreDestroy
    public void shutdown() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("Error closing binary transfer server socket", e);
            }
        }
        executorService.shutdown();
    }
    
    /**
     * Thông tin về file data
     */
    private static class FileData {
        final String filePath;
        final byte[] data;
        
        FileData(String filePath, byte[] data) {
            this.filePath = filePath;
            this.data = data;
        }
    }
}
