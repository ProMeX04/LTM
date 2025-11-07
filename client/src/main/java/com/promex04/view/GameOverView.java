package com.promex04.view;

import com.promex04.controller.GameController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameOverView extends VBox {
    private final GameController controller;
    private Label headerLabel;
    private Label myScoreLabel;
    private Label opponentScoreLabel;
    private Label resultLabel;
    private Button backToLobbyButton;

    public GameOverView(GameController controller) {
        this.controller = controller;
        initializeUI();
        setupCallbacks();
    }

    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(30);
        setPadding(new Insets(40));
        setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");

        // Header
        headerLabel = new Label();
        headerLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 36));
        headerLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 20, 0.3, 0, 8);");

        // Score section
        VBox scoreSection = createScoreSection();

        // Result message
        resultLabel = new Label();
        resultLabel.setFont(Font.font("Poppins", FontWeight.SEMI_BOLD, 20));
        resultLabel.setTextFill(Color.rgb(255, 255, 255, 0.9));
        resultLabel.setAlignment(Pos.CENTER);
        resultLabel.setWrapText(true);
        resultLabel.setMaxWidth(600);

        // Back to lobby button
        backToLobbyButton = new Button("Quay về sảnh");
        backToLobbyButton.setFont(Font.font("Poppins", FontWeight.BOLD, 16));
        backToLobbyButton.setPrefWidth(200);
        backToLobbyButton.setPrefHeight(50);
        backToLobbyButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a); -fx-text-fill: white; "
                        + "-fx-background-radius: 12px; -fx-cursor: hand; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0.3, 0, 6);");
        backToLobbyButton.setOnMouseEntered(e -> backToLobbyButton
                .setStyle("-fx-background-color: linear-gradient(to right, #16a34a, #15803d); -fx-text-fill: white; "
                        + "-fx-background-radius: 12px; -fx-cursor: hand; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 16, 0.35, 0, 8);"));
        backToLobbyButton.setOnMouseExited(e -> backToLobbyButton
                .setStyle("-fx-background-color: linear-gradient(to right, #22c55e, #16a34a); -fx-text-fill: white; "
                        + "-fx-background-radius: 12px; -fx-cursor: hand; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0.3, 0, 6);"));
        backToLobbyButton.setOnAction(e -> {
            // Quay về lobby sẽ được xử lý bởi ClientApplication
            // Chỉ cần gọi callback hoặc để ClientApplication tự xử lý
        });

        getChildren().addAll(headerLabel, scoreSection, resultLabel, backToLobbyButton);
    }

    private VBox createScoreSection() {
        VBox scoreCard = new VBox(20);
        scoreCard.setAlignment(Pos.CENTER);
        scoreCard.setStyle("-fx-background-color: rgba(15,23,42,0.7); -fx-background-radius: 20; -fx-padding: 40; "
                + "-fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 20; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 24, 0.3, 0, 12);");

        Label scoreTitle = new Label("Điểm số cuối cùng");
        scoreTitle.setFont(Font.font("Poppins", FontWeight.SEMI_BOLD, 18));
        scoreTitle.setTextFill(Color.rgb(200, 210, 220));

        HBox scoreDisplay = new HBox(40);
        scoreDisplay.setAlignment(Pos.CENTER);

        VBox myScoreBox = new VBox(12);
        myScoreBox.setAlignment(Pos.CENTER);
        Label myLabel = new Label("Bạn");
        myLabel.setFont(Font.font("Poppins", FontWeight.MEDIUM, 16));
        myLabel.setTextFill(Color.rgb(200, 210, 220));
        myScoreLabel = new Label("0");
        myScoreLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 48));
        myScoreBox.getChildren().addAll(myLabel, myScoreLabel);

        Label vsLabel = new Label("VS");
        vsLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 24));
        vsLabel.setTextFill(Color.rgb(148, 163, 184));

        VBox opponentScoreBox = new VBox(12);
        opponentScoreBox.setAlignment(Pos.CENTER);
        Label opponentLabel = new Label("Đối thủ");
        opponentLabel.setFont(Font.font("Poppins", FontWeight.MEDIUM, 16));
        opponentLabel.setTextFill(Color.rgb(200, 210, 220));
        opponentScoreLabel = new Label("0");
        opponentScoreLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 48));
        opponentScoreBox.getChildren().addAll(opponentLabel, opponentScoreLabel);

        scoreDisplay.getChildren().addAll(myScoreBox, vsLabel, opponentScoreBox);
        scoreCard.getChildren().addAll(scoreTitle, scoreDisplay);

        return scoreCard;
    }

    private void setupCallbacks() {
        // Cập nhật UI khi game kết thúc
        controller.setOnGameOver(() -> {
            updateGameOverUI();
        });
    }

    public void updateGameOverUI() {
        String gameOverMessage = controller.getGameOverMessage();
        int myScore = controller.getMyScore();
        int opponentScore = controller.getOpponentScore();
        boolean isWin = gameOverMessage.contains("thắng");
        boolean isDraw = gameOverMessage.contains("Hòa");

        // Cập nhật header
        if (isWin) {
            headerLabel.setText("🎉 Bạn đã thắng! 🎉");
            headerLabel.setTextFill(Color.rgb(34, 197, 94));
        } else if (isDraw) {
            headerLabel.setText("🤝 Hòa!");
            headerLabel.setTextFill(Color.rgb(250, 204, 21));
        } else {
            headerLabel.setText("😔 Bạn đã thua");
            headerLabel.setTextFill(Color.rgb(239, 68, 68));
        }

        // Cập nhật điểm số
        myScoreLabel.setText(String.valueOf(myScore));
        opponentScoreLabel.setText(String.valueOf(opponentScore));

        // Cập nhật màu điểm số
        if (isWin) {
            myScoreLabel.setTextFill(Color.rgb(34, 197, 94));
            opponentScoreLabel.setTextFill(Color.rgb(71, 85, 105));
        } else if (isDraw) {
            myScoreLabel.setTextFill(Color.rgb(250, 204, 21));
            opponentScoreLabel.setTextFill(Color.rgb(250, 204, 21));
        } else {
            myScoreLabel.setTextFill(Color.rgb(71, 85, 105));
            opponentScoreLabel.setTextFill(Color.rgb(239, 68, 68));
        }

        // Cập nhật thông điệp kết quả
        resultLabel.setText(gameOverMessage);
    }

    public Button getBackToLobbyButton() {
        return backToLobbyButton;
    }
}
