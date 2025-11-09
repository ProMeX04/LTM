package com.promex04;

import com.promex04.controller.GameController;
import com.promex04.view.GameOverView;
import com.promex04.view.GameView;
import com.promex04.view.LoadingView;
import com.promex04.view.LobbyView;
import com.promex04.view.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application class cho Client
 */
public class ClientApplication extends Application {
    private Stage primaryStage;
    private GameController controller;
    private LoginView loginView;
    private LobbyView lobbyView;
    private GameView gameView;
    private LoadingView loadingView;
    private GameOverView gameOverView;
    private Scene loginScene;
    private Scene lobbyScene;
    private Scene gameScene;
    private Scene loadingScene;
    private Scene gameOverScene;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.controller = new GameController();

        // Initialize views
        loginView = new LoginView(controller);
        lobbyView = new LobbyView(controller);
        gameView = new GameView(controller);
        gameOverView = new GameOverView(controller);

        // Initialize scenes once to avoid reusing roots across scenes
        loginScene = new Scene(loginView, 400, 400);
        lobbyScene = new Scene(lobbyView, 1200, 700);
        gameScene = new Scene(gameView, 600, 400);
        loadingView = new LoadingView(controller);
        loadingScene = new Scene(loadingView, 800, 500);
        gameOverScene = new Scene(gameOverView, 600, 400);

        // Setup navigation callbacks
        setupNavigation();

        // Show login screen first
        showLogin();

        primaryStage.setTitle("Game Đoán Âm Thanh");
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    private void setupNavigation() {
        // Khi login thành công, hiển thị lobby
        controller.setOnLoginSuccess(() -> {
            showLobby();
        });

        // Khi đăng ký thành công, hiển thị lobby (như đăng nhập)
        controller.setOnRegisterSuccess(() -> {
            showLobby();
        });

        // Khi game bắt đầu, hiển thị game view ngay (loading sẽ hiển thị trong
        // GameView)
        controller.setOnGameStart(() -> {
            javafx.application.Platform.runLater(() -> {
                showGame();
            });
        });

        // Khi server gửi ROUND_START đầu tiên, game đã sẵn sàng (cả 2 người đã tải
        // xong)
        controller.setOnGameReady(() -> {
            javafx.application.Platform.runLater(() -> {
                System.out.println("[ClientApplication] onGameReady called - game is ready");
                // GameView sẽ tự ẩn loading overlay khi nhận được onGameReady
            });
        });

        // Khi game kết thúc, hiển thị màn hình kết thúc game
        controller.setOnGameOver(() -> {
            System.out.println("[ClientApplication] onGameOver callback được gọi");
            showGameOver();
        });

        // Kết nối nút "Quay về sảnh" trong GameOverView với showLobby
        gameOverView.getBackToLobbyButton().setOnAction(e -> {
            showLobby();
        });

        // Khi đăng xuất, quay về màn hình đăng nhập
        controller.setOnLogout(() -> {
            showLogin();
        });

        // Khi rời trận (client-side), quay về lobby ngay
        controller.setOnLeftMatch(() -> {
            showLobby();
        });
    }

    private void showLogin() {
        primaryStage.setScene(loginScene);
        primaryStage.sizeToScene();
        loginView.reset();
    }

    private void showLobby() {
        primaryStage.setScene(lobbyScene);
        primaryStage.sizeToScene();
    }

    private void showGame() {
        primaryStage.setScene(gameScene);
        primaryStage.sizeToScene();
    }

    private void showGameOver() {
        System.out.println("[ClientApplication] showGameOver() được gọi");
        // Cập nhật UI của GameOverView với thông tin mới nhất
        gameOverView.updateGameOverUI();
        System.out.println("[ClientApplication] Đã cập nhật UI, chuyển sang gameOverScene");
        primaryStage.setScene(gameOverScene);
        primaryStage.sizeToScene();
        System.out.println("[ClientApplication] Đã chuyển scene thành công");
    }

    @Override
    public void stop() throws Exception {
        if (controller != null) {
            controller.disconnect();
        }
        super.stop();
    }

    public static void main(String[] args) {
        // Không cần GStreamer nữa - sử dụng MP3SPI (Java thuần)
        launch(args);
    }
}
