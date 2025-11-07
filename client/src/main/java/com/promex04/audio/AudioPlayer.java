package com.promex04.audio;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Audio player sử dụng Java Sound API với MP3SPI - không cần GStreamer
 */
public class AudioPlayer {
    private Clip clip;
    private AudioInputStream audioInputStream;
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private Runnable onPlaybackFinished;
    private Runnable onError;

    static {
        // Kiểm tra MP3SPI service provider
        try {
            // MP3SPI sẽ tự động đăng ký khi class được load
            // Kiểm tra xem MP3 có được hỗ trợ không
            javax.sound.sampled.AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();
            boolean hasMp3Support = false;
            for (javax.sound.sampled.AudioFileFormat.Type type : types) {
                if (type.toString().toLowerCase().contains("mp3") ||
                        type.toString().toLowerCase().equals("mp3")) {
                    hasMp3Support = true;
                    break;
                }
            }
            if (hasMp3Support) {
                System.out.println("[AudioPlayer] MP3SPI initialized - MP3 support available");
            } else {
                System.err
                        .println("[AudioPlayer] Warning: MP3 support may not be available. Check MP3SPI dependencies.");
                System.err.println("[AudioPlayer] Available audio types: " + java.util.Arrays.toString(types));
            }
        } catch (Exception e) {
            System.err.println("[AudioPlayer] Warning: Could not check MP3SPI: " + e.getMessage());
        }
    }

    public AudioPlayer() {
    }

    /**
     * Load audio từ file hoặc URL
     */
    public boolean load(String source) {
        stop();

        try {
            InputStream inputStream = null;

            // Xử lý file: URI (từ cache)
            if (source.startsWith("file:")) {
                try {
                    java.net.URI uri = java.net.URI.create(source);
                    File file = new File(uri);
                    if (!file.exists()) {
                        System.err.println("[AudioPlayer] File not found: " + source);
                        if (onError != null)
                            onError.run();
                        return false;
                    }
                    inputStream = new FileInputStream(file);
                    System.out.println("[AudioPlayer] Loading from file: " + file.getAbsolutePath());
                } catch (Exception e) {
                    System.err.println("[AudioPlayer] Error opening file: " + e.getMessage());
                    if (onError != null)
                        onError.run();
                    return false;
                }
            }
            // Xử lý HTTP URL - cần buffer toàn bộ vào memory
            else if (source.startsWith("http://") || source.startsWith("https://")) {
                try {
                    System.out.println("[AudioPlayer] Loading from URL: " + source);
                    java.net.URI uri = java.net.URI.create(source);
                    URL url = uri.toURL();
                    java.net.URLConnection conn = url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(30000);

                    // Đọc toàn bộ vào ByteArrayInputStream để đảm bảo không bị đóng sớm
                    try (InputStream urlStream = conn.getInputStream()) {
                        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                        byte[] data = new byte[8192];
                        int nRead;
                        while ((nRead = urlStream.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }
                        buffer.flush();
                        inputStream = new java.io.ByteArrayInputStream(buffer.toByteArray());
                        System.out.println("[AudioPlayer] Buffered " + buffer.size() + " bytes from URL");
                    }
                } catch (Exception e) {
                    System.err.println("[AudioPlayer] Error opening URL: " + e.getMessage());
                    e.printStackTrace();
                    if (onError != null)
                        onError.run();
                    return false;
                }
            }
            // Xử lý file path
            else {
                File file = new File(source);
                if (!file.exists()) {
                    System.err.println("[AudioPlayer] File not found: " + source);
                    if (onError != null)
                        onError.run();
                    return false;
                }
                inputStream = new FileInputStream(file);
                System.out.println("[AudioPlayer] Loading from file path: " + file.getAbsolutePath());
            }

            // Tạo AudioInputStream với MP3SPI
            System.out.println("[AudioPlayer] Creating AudioInputStream from source...");
            try {
                audioInputStream = AudioSystem.getAudioInputStream(inputStream);
                System.out.println("[AudioPlayer] AudioInputStream created successfully");
            } catch (UnsupportedAudioFileException e) {
                System.err.println("[AudioPlayer] Unsupported audio format. MP3SPI may not be loaded correctly.");
                System.err.println("[AudioPlayer] Available audio file types: ");
                javax.sound.sampled.AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();
                for (javax.sound.sampled.AudioFileFormat.Type type : types) {
                    System.err.println("  - " + type);
                }
                throw e;
            }

            // Lấy format và convert nếu cần
            AudioFormat baseFormat = audioInputStream.getFormat();
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);

            // Convert format nếu cần
            System.out.println("[AudioPlayer] Base format: " + baseFormat);
            System.out.println("[AudioPlayer] Decoded format: " + decodedFormat);

            AudioInputStream decodedInputStream = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream);

            // Tạo Clip
            DataLine.Info info = new DataLine.Info(Clip.class, decodedFormat);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("[AudioPlayer] Line not supported: " + decodedFormat);
                if (onError != null)
                    onError.run();
                return false;
            }

            System.out.println("[AudioPlayer] Opening clip...");
            clip = (Clip) AudioSystem.getLine(info);

            // Đọc toàn bộ audio vào memory trước khi mở clip (quan trọng cho HTTP và file
            // cache)
            // Clip cần toàn bộ data trong memory để phát được
            try {
                System.out.println("[AudioPlayer] Buffering audio data into memory...");
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                byte[] data = new byte[8192];
                int nRead;
                long totalRead = 0;
                while ((nRead = decodedInputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                    totalRead += nRead;
                }
                buffer.flush();
                decodedInputStream.close();

                // Tạo AudioInputStream mới từ buffer với format đã decode
                java.io.ByteArrayInputStream bufferedInput = new java.io.ByteArrayInputStream(buffer.toByteArray());
                long frameLength = AudioSystem.NOT_SPECIFIED;
                if (decodedFormat.getFrameSize() > 0 && decodedFormat.getFrameRate() > 0) {
                    // Tính số frame dựa trên kích thước buffer và frame size
                    frameLength = buffer.size() / decodedFormat.getFrameSize();
                }
                decodedInputStream = new AudioInputStream(bufferedInput, decodedFormat, frameLength);
                System.out.println("[AudioPlayer] Buffered " + totalRead + " bytes (" +
                        (totalRead / 1024) + " KB) into memory, frame length: " + frameLength);
            } catch (Exception e) {
                System.err.println("[AudioPlayer] Error buffering audio: " + e.getMessage());
                e.printStackTrace();
                if (onError != null)
                    onError.run();
                return false;
            }

            System.out.println("[AudioPlayer] Opening clip with buffered data...");
            clip.open(decodedInputStream);
            System.out.println(
                    "[AudioPlayer] Clip opened successfully, length: " + clip.getMicrosecondLength() / 1000 + " ms");

            // Thêm listener khi phát xong
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && isPlaying.get()) {
                    isPlaying.set(false);
                    if (onPlaybackFinished != null) {
                        onPlaybackFinished.run();
                    }
                }
            });

            return true;
        } catch (UnsupportedAudioFileException e) {
            System.err.println("[AudioPlayer] Unsupported audio format: " + e.getMessage());
            if (onError != null)
                onError.run();
            return false;
        } catch (LineUnavailableException e) {
            System.err.println("[AudioPlayer] Line unavailable: " + e.getMessage());
            if (onError != null)
                onError.run();
            return false;
        } catch (IOException e) {
            System.err.println("[AudioPlayer] IO error: " + e.getMessage());
            if (onError != null)
                onError.run();
            return false;
        } catch (Exception e) {
            System.err.println("[AudioPlayer] Unexpected error: " + e.getMessage());
            e.printStackTrace();
            if (onError != null)
                onError.run();
            return false;
        }
    }

    /**
     * Phát audio
     */
    public void play() {
        if (clip == null) {
            System.err.println("[AudioPlayer] No audio loaded");
            if (onError != null)
                onError.run();
            return;
        }

        try {
            // Đảm bảo clip đã sẵn sàng
            if (!clip.isOpen()) {
                System.err.println("[AudioPlayer] Clip is not open");
                if (onError != null)
                    onError.run();
                return;
            }

            if (isPlaying.get() && clip.isRunning()) {
                stop();
            }

            clip.setFramePosition(0); // Reset về đầu
            System.out.println("[AudioPlayer] Starting playback...");
            clip.start();
            isPlaying.set(true);
            System.out.println("[AudioPlayer] Playback started, clip running: " + clip.isRunning());
        } catch (Exception e) {
            System.err.println("[AudioPlayer] Error playing: " + e.getMessage());
            e.printStackTrace();
            if (onError != null)
                onError.run();
        }
    }

    /**
     * Dừng phát
     */
    public void stop() {
        if (clip != null) {
            try {
                if (clip.isRunning()) {
                    clip.stop();
                }
                isPlaying.set(false);
            } catch (Exception e) {
                System.err.println("[AudioPlayer] Error stopping: " + e.getMessage());
            }
        }
    }

    /**
     * Tạm dừng
     */
    public void pause() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            isPlaying.set(false);
        }
    }

    /**
     * Tiếp tục phát
     */
    public void resume() {
        if (clip != null && !clip.isRunning()) {
            clip.start();
            isPlaying.set(true);
        }
    }

    /**
     * Giải phóng tài nguyên
     */
    public void dispose() {
        stop();
        if (clip != null) {
            try {
                clip.close();
            } catch (Exception e) {
                // Ignore
            }
            clip = null;
        }
        if (audioInputStream != null) {
            try {
                audioInputStream.close();
            } catch (Exception e) {
                // Ignore
            }
            audioInputStream = null;
        }
    }

    /**
     * Kiểm tra đang phát
     */
    public boolean isPlaying() {
        return isPlaying.get() && clip != null && clip.isRunning();
    }

    /**
     * Set callback khi phát xong
     */
    public void setOnPlaybackFinished(Runnable callback) {
        this.onPlaybackFinished = callback;
    }

    /**
     * Set callback khi có lỗi
     */
    public void setOnError(Runnable callback) {
        this.onError = callback;
    }

    /**
     * Lấy độ dài audio (microseconds)
     */
    public long getMicrosecondLength() {
        if (clip != null) {
            return clip.getMicrosecondLength();
        }
        return 0;
    }

    /**
     * Lấy vị trí hiện tại (microseconds)
     */
    public long getMicrosecondPosition() {
        if (clip != null) {
            return clip.getMicrosecondPosition();
        }
        return 0;
    }
}
