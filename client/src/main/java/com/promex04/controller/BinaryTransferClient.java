package com.promex04.controller;

import java.io.IOException;
import java.net.Socket;

/**
 * Client để nhận binary file qua socket riêng
 * Kết nối đến port được server cung cấp và nhận file data
 */
public class BinaryTransferClient {
    
    /**
     * Nhận file từ server qua socket cố định
     * @param serverHost Host của server
     * @param port Port cố định của binary socket
     * @param filePath Đường dẫn file cần tải
     * @param fileSize Kích thước file cần nhận
     * @return Dữ liệu file dưới dạng byte array
     * @throws IOException Nếu có lỗi khi kết nối hoặc nhận dữ liệu
     */
    public static byte[] receiveFile(String serverHost, int port, String filePath, long fileSize) throws IOException {
        Socket socket = null;
        try {
            // Kết nối đến binary socket cố định
            socket = new Socket(serverHost, port);
            
            // Tối ưu socket options
            socket.setTcpNoDelay(true);
            socket.setReceiveBufferSize(256 * 1024); // 256KB receive buffer
            socket.setSendBufferSize(256 * 1024); // 256KB send buffer
            
            // Gửi file path đến server (UTF-8, kết thúc bằng \n)
            // Sử dụng OutputStream trực tiếp để đảm bảo gửi ngay lập tức
            java.io.OutputStream out = socket.getOutputStream();
            byte[] pathBytes = (filePath + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            out.write(pathBytes);
            out.flush(); // Đảm bảo gửi ngay lập tức
            
            // Đọc binary data từ socket
            java.io.BufferedInputStream in = new java.io.BufferedInputStream(
                socket.getInputStream(), 65536); // 64KB buffer
            
            byte[] fileData = new byte[(int) fileSize];
            int totalRead = 0;
            byte[] readBuffer = new byte[65536]; // 64KB chunks
            
            long startTime = System.currentTimeMillis();
            
            while (totalRead < fileSize) {
                int remaining = (int) fileSize - totalRead;
                int toRead = Math.min(readBuffer.length, remaining);
                int bytesRead = in.read(readBuffer, 0, toRead);
                if (bytesRead == -1) {
                    throw new IOException("Unexpected end of stream");
                }
                // Copy vào fileData
                System.arraycopy(readBuffer, 0, fileData, totalRead, bytesRead);
                totalRead += bytesRead;
                
                if (totalRead % 100000 == 0 || totalRead == fileSize) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double speed = totalRead / 1024.0 / (elapsed / 1000.0); // KB/s
                    System.out.println(String.format("[BinaryTransfer] Read %d/%d bytes (%.1f KB/s)", 
                            totalRead, fileSize, speed));
                }
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            double speed = fileSize / 1024.0 / (elapsed / 1000.0); // KB/s
            System.out.println(String.format("[BinaryTransfer] Completed: %d bytes in %d ms (%.1f KB/s)", 
                    totalRead, elapsed, speed));
            
            return fileData;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
}

