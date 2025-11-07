package com.promex04.view;

import com.promex04.controller.GameController;
import com.promex04.model.GameRound;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import com.promex04.audio.AudioPlayer;

public class GameView extends VBox {
    private GameController controller;

    // Progress components
    private ProgressIndicator statusIndicator;

    // Buttons
    private Button option1Button;
    private Button option2Button;
    private Button option3Button;
    private Button option4Button;
    private Button playAudioButton;

    // Score display components (2 rows: player name, current round, score)
    private Label myPlayerNameLabel;
    private Label myCurrentRoundLabel;
    private Label myScoreValueLabel;
    private Label opponentPlayerNameLabel;
    private Label opponentCurrentRoundLabel;
    private Label opponentScoreValueLabel;

    // Minimal labels
    private Label timerValueLabel;
    private Label audioStatusLabel;

    private Timeline timerTimeline;
    private int remainingSeconds = 15;
    private AudioPlayer audioPlayer;

    public GameView(GameController controller) {
        this.controller = controller;
        initializeUI();
        setupCallbacks();
    }

    private void initializeUI() {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(20);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");

        // Header với progress bar cho round
        HBox header = createHeader();

        // Score section với progress bars
        VBox scoreSection = createScoreSection();

        // Audio section
        VBox audioSection = createAudioSection();

        // Options grid
        VBox optionsSection = createOptionsSection();

        // Status với progress indicator
        HBox statusSection = createStatusSection();

        VBox rightColumn = new VBox(18, audioSection, optionsSection, statusSection);
        rightColumn.setAlignment(Pos.TOP_CENTER);
        rightColumn.setPrefWidth(560);

        VBox leftColumn = new VBox(16, scoreSection);
        leftColumn.setAlignment(Pos.TOP_LEFT);
        leftColumn.setPrefWidth(280);

        HBox mainContent = new HBox(24, leftColumn, rightColumn);
        mainContent.setAlignment(Pos.TOP_CENTER);

        getChildren().addAll(header, mainContent);
    }

    private HBox createHeader() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Timer chỉ có label, không có progress bar
        timerValueLabel = new Label("15s");
        timerValueLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 24));
        timerValueLabel.setTextFill(Color.rgb(250, 204, 21));
        timerValueLabel
                .setStyle("-fx-background-color: rgba(15,23,42,0.55); -fx-background-radius: 12; -fx-padding: 8 16; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 16, 0.2, 0, 6);");

        Button exitButton = new Button("Rời trận");
        exitButton.setStyle("-fx-background-color: linear-gradient(to right, #ef4444, #f97316); -fx-text-fill: white; "
                + "-fx-background-radius: 10px; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 16;");
        exitButton.setOnAction(e -> handleLeaveMatch());

        HBox header = new HBox(20, spacer, timerValueLabel, exitButton);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox createScoreSection() {
        VBox scoreCard = new VBox(12);
        scoreCard.setAlignment(Pos.TOP_LEFT);
        scoreCard.setStyle("-fx-background-color: rgba(15,23,42,0.5); -fx-background-radius: 16; -fx-padding: 20;"
                + "-fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 16;");

        // Row 1: My player info
        HBox myRow = new HBox(16);
        myRow.setAlignment(Pos.CENTER_LEFT);

        myPlayerNameLabel = new Label("Bạn");
        myPlayerNameLabel.setFont(Font.font("Poppins", FontWeight.SEMI_BOLD, 16));
        myPlayerNameLabel.setTextFill(Color.rgb(226, 232, 240));
        myPlayerNameLabel.setPrefWidth(80);

        myCurrentRoundLabel = new Label("Câu 0");
        myCurrentRoundLabel.setFont(Font.font("Poppins", FontWeight.NORMAL, 14));
        myCurrentRoundLabel.setTextFill(Color.rgb(200, 210, 220));
        myCurrentRoundLabel.setPrefWidth(60);

        myScoreValueLabel = new Label("0");
        myScoreValueLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 18));
        myScoreValueLabel.setTextFill(Color.rgb(34, 197, 94));
        HBox.setHgrow(myScoreValueLabel, Priority.ALWAYS);

        myRow.getChildren().addAll(myPlayerNameLabel, myCurrentRoundLabel, myScoreValueLabel);

        // Row 2: Opponent player info
        HBox opponentRow = new HBox(16);
        opponentRow.setAlignment(Pos.CENTER_LEFT);

        opponentPlayerNameLabel = new Label("Đối thủ");
        opponentPlayerNameLabel.setFont(Font.font("Poppins", FontWeight.SEMI_BOLD, 16));
        opponentPlayerNameLabel.setTextFill(Color.rgb(226, 232, 240));
        opponentPlayerNameLabel.setPrefWidth(80);

        opponentCurrentRoundLabel = new Label("Câu 0");
        opponentCurrentRoundLabel.setFont(Font.font("Poppins", FontWeight.NORMAL, 14));
        opponentCurrentRoundLabel.setTextFill(Color.rgb(200, 210, 220));
        opponentCurrentRoundLabel.setPrefWidth(60);

        opponentScoreValueLabel = new Label("0");
        opponentScoreValueLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 18));
        opponentScoreValueLabel.setTextFill(Color.rgb(239, 68, 68));
        HBox.setHgrow(opponentScoreValueLabel, Priority.ALWAYS);

        opponentRow.getChildren().addAll(opponentPlayerNameLabel, opponentCurrentRoundLabel, opponentScoreValueLabel);

        scoreCard.getChildren().addAll(myRow, opponentRow);

        return scoreCard;
    }

    private VBox createAudioSection() {
        VBox audioCard = new VBox(14);
        audioCard.setAlignment(Pos.TOP_LEFT);
        audioCard.setStyle("-fx-background-color: rgba(15,23,42,0.6); -fx-background-radius: 16; -fx-padding: 20; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 20, 0.25, 0, 12);");

        audioStatusLabel = new Label("Đang chờ...");
        audioStatusLabel.setFont(Font.font("Poppins", FontWeight.NORMAL, 13));
        audioStatusLabel.setTextFill(Color.rgb(226, 232, 240));
        audioStatusLabel.setWrapText(true);

        playAudioButton = new Button("Nghe lại");
        playAudioButton
                .setStyle("-fx-background-color: linear-gradient(to right, #22c55e, #16a34a); -fx-text-fill: white;"
                        + "-fx-background-radius: 10px; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 20;");
        playAudioButton.setOnAction(e -> replayAudio());

        audioCard.getChildren().addAll(audioStatusLabel, playAudioButton);

        return audioCard;
    }

    private VBox createOptionsSection() {
        GridPane optionsGrid = new GridPane();
        optionsGrid.setHgap(16);
        optionsGrid.setVgap(16);
        optionsGrid.setAlignment(Pos.CENTER);

        option1Button = createOptionButton("Lựa chọn 1");
        option2Button = createOptionButton("Lựa chọn 2");
        option3Button = createOptionButton("Lựa chọn 3");
        option4Button = createOptionButton("Lựa chọn 4");

        optionsGrid.add(option1Button, 0, 0);
        optionsGrid.add(option2Button, 1, 0);
        optionsGrid.add(option3Button, 0, 1);
        optionsGrid.add(option4Button, 1, 1);

        option1Button.setOnAction(e -> submitAnswer(1));
        option2Button.setOnAction(e -> submitAnswer(2));
        option3Button.setOnAction(e -> submitAnswer(3));
        option4Button.setOnAction(e -> submitAnswer(4));

        VBox optionsCard = new VBox(14, optionsGrid);
        optionsCard.setAlignment(Pos.TOP_CENTER);
        optionsCard.setStyle("-fx-background-color: rgba(15,23,42,0.55); -fx-background-radius: 16; -fx-padding: 20; "
                + "-fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 16;");

        return optionsCard;
    }

    private HBox createStatusSection() {
        statusIndicator = new ProgressIndicator();
        statusIndicator.setPrefSize(24, 24);
        statusIndicator.setStyle("-fx-progress-color: #22c55e;");
        statusIndicator.setVisible(false);

        statusLabel = new Label("Chờ âm thanh...");
        statusLabel.setFont(Font.font("Poppins", FontWeight.MEDIUM, 13));
        statusLabel.setTextFill(Color.rgb(224, 231, 255));

        HBox statusBox = new HBox(10, statusIndicator, statusLabel);
        statusBox.setAlignment(Pos.CENTER);
        return statusBox;
    }

    private Button createOptionButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(260);
        button.setPrefHeight(80);
        button.setWrapText(true);
        button.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        button.setFont(Font.font("Poppins", FontWeight.SEMI_BOLD, 15));
        button.setStyle("-fx-background-color: rgba(255,255,255,0.92); -fx-text-fill: #4338ca; "
                + "-fx-background-radius: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0.2, 0, 6);");

        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: white; -fx-text-fill: #312e81; "
                + "-fx-background-radius: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.26), 16, 0.25, 0, 8);"));

        button.setOnMouseExited(
                e -> button.setStyle("-fx-background-color: rgba(255,255,255,0.92); -fx-text-fill: #4338ca; "
                        + "-fx-background-radius: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0.2, 0, 6);"));

        return button;
    }

    private void setupCallbacks() {
        // Khởi tạo điểm số khi game bắt đầu
        controller.setOnGameStart(() -> {
            updateScore();
            updateProgress();
        });

        controller.setOnRoundStart(() -> {
            GameRound round = controller.getCurrentRound();
            if (round != null) {
                updateRoundUI(round);
                playAudioForRound(round);
                startTimer();
                // Không cập nhật điểm số ở đây, chỉ cập nhật từ SCORE_UPDATE từ server
            }
        });

        controller.setOnRoundResult(() -> {
            stopTimer();
            boolean isCorrect = controller.isLastAnswerCorrect();
            int selectedAnswer = controller.getLastSelectedAnswer();
            int points = controller.getLastAnswerPoints();

            // Update status indicator
            if (isCorrect) {
                statusIndicator.setStyle("-fx-progress-color: #22c55e;");
                statusIndicator.setProgress(1.0);
                statusLabel.setText("Đúng! +" + points + " điểm");
                statusLabel.setTextFill(Color.rgb(34, 197, 94));
            } else {
                statusIndicator.setStyle("-fx-progress-color: #ef4444;");
                statusIndicator.setProgress(1.0);
                if (selectedAnswer == 0) {
                    statusLabel.setText("Hết giờ! 0 điểm");
                } else {
                    statusLabel.setText("Sai! 0 điểm");
                }
                statusLabel.setTextFill(Color.RED);
            }
            statusIndicator.setVisible(true);

            // Highlight đáp án đã chọn
            if (selectedAnswer > 0) {
                highlightSelectedAnswer(selectedAnswer, isCorrect);
            }

            disableOptions();
            // Không cập nhật điểm số ở đây vì server sẽ gửi SCORE_UPDATE riêng
        });

        controller.setOnScoreUpdate(() -> {
            // Đây là nơi duy nhất cập nhật điểm số từ server
            updateScore();
        });

        controller.setOnProgressUpdate(() -> {
            updateProgress();
        });

        controller.setOnGameOver(() -> {
            stopTimer();
            if (audioPlayer != null) {
                audioPlayer.dispose();
                audioPlayer = null;
            }
            // GameOverView sẽ được hiển thị bởi ClientApplication
        });

        controller.setOnPrefetchStart(() -> {
            statusIndicator.setVisible(true);
            statusIndicator.setProgress(-1); // Indeterminate
            statusLabel.setText("Đang tải âm thanh...");
            playAudioButton.setDisable(true);
        });

        controller.setOnPrefetchDone(() -> {
            statusIndicator.setVisible(false);
            if (controller.hasPrefetchFailures()) {
                statusLabel.setText("Một số âm thanh chưa tải được");
            } else {
                statusLabel.setText("Sẵn sàng!");
            }
            playAudioButton.setDisable(false);
        });
    }

    private void updateRoundUI(GameRound round) {
        option1Button.setText(round.getOption1());
        option2Button.setText(round.getOption2());
        option3Button.setText(round.getOption3());
        option4Button.setText(round.getOption4());

        // Cập nhật số câu hiện tại dựa trên progress của từng người chơi
        int myProgress = controller.getMyProgress();
        int opponentProgress = controller.getOpponentProgress();

        // Cập nhật câu của người chơi dựa trên round number (round mới bắt đầu)
        int roundNumber = round.getRoundNumber();
        if (myProgress > 0) {
            myCurrentRoundLabel.setText("Câu " + myProgress);
        } else {
            myCurrentRoundLabel.setText("Câu " + roundNumber);
        }

        // Cập nhật câu của đối thủ dựa trên progress của họ
        if (opponentProgress > 0) {
            opponentCurrentRoundLabel.setText("Câu " + opponentProgress);
        } else {
            opponentCurrentRoundLabel.setText("Câu 0");
        }

        // Cập nhật audio status
        audioStatusLabel.setText("Câu " + roundNumber);

        resetButtonStyles();
        enableOptions();

        statusIndicator.setVisible(false);
        statusLabel.setText("Nghe âm thanh và chọn đáp án");
        statusLabel.setTextFill(Color.rgb(224, 231, 255));
    }

    private void resetButtonStyles() {
        String defaultStyle = "-fx-background-color: rgba(255,255,255,0.92); -fx-text-fill: #4338ca; "
                + "-fx-background-radius: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0.2, 0, 6);";
        option1Button.setStyle(defaultStyle);
        option2Button.setStyle(defaultStyle);
        option3Button.setStyle(defaultStyle);
        option4Button.setStyle(defaultStyle);
    }

    private void highlightSelectedAnswer(int answerIndex, boolean isCorrect) {
        Button selectedButton = getButtonByIndex(answerIndex);
        if (selectedButton == null)
            return;

        if (isCorrect) {
            selectedButton.setStyle(
                    "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a); -fx-text-fill: white; "
                            + "-fx-background-radius: 16; -fx-cursor: default; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 16, 0.25, 0, 8);");
        } else {
            selectedButton.setStyle(
                    "-fx-background-color: linear-gradient(to right, #ef4444, #dc2626); -fx-text-fill: white; "
                            + "-fx-background-radius: 16; -fx-cursor: default; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 16, 0.25, 0, 8);");
        }
    }

    private Button getButtonByIndex(int index) {
        switch (index) {
            case 1:
                return option1Button;
            case 2:
                return option2Button;
            case 3:
                return option3Button;
            case 4:
                return option4Button;
            default:
                return null;
        }
    }

    private void playAudioForRound(GameRound round) {
        if (round == null)
            return;
        playAudio(round.getAudioFilePath());
    }

    private void replayAudio() {
        GameRound round = controller.getCurrentRound();
        if (round != null) {
            playAudio(round.getAudioFilePath());
        }
    }

    private void playAudio(String audioPath) {
        if (audioPlayer != null) {
            audioPlayer.dispose();
            audioPlayer = null;
        }

        if (audioPath == null || audioPath.isBlank()) {
            audioStatusLabel.setText("Không có đường dẫn âm thanh");
            return;
        }

        String source;
        try {
            source = controller.getResolvedAudioSource(audioPath);
            if (source == null) {
                String raw = audioPath.trim();
                if (raw.startsWith("file:")) {
                    source = raw;
                } else {
                    // File chưa có trong cache - không thể phát
                    audioStatusLabel.setText("File chưa được tải");
                    playAudioButton.setDisable(false);
                    return;
                }
            }

            audioPlayer = new AudioPlayer();
            audioStatusLabel.setText("Đang tải...");
            playAudioButton.setDisable(true);

            audioPlayer.setOnPlaybackFinished(() -> {
                javafx.application.Platform.runLater(() -> {
                    // Giữ nguyên số câu, không hiển thị "Đã phát xong"
                    String roundText = controller.getCurrentRound() != null
                            ? "Câu " + controller.getCurrentRound().getRoundNumber()
                            : "";
                    audioStatusLabel.setText(roundText);
                });
            });

            audioPlayer.setOnError(() -> {
                javafx.application.Platform.runLater(() -> {
                    audioStatusLabel.setText("Lỗi phát âm thanh");
                    playAudioButton.setDisable(false);
                });
            });

            boolean loaded = false;
            if (source != null) {
                loaded = audioPlayer.load(source);
            }

            if (loaded) {
                // Chỉ hiển thị số câu
                String roundText = controller.getCurrentRound() != null
                        ? "Câu " + controller.getCurrentRound().getRoundNumber()
                        : "Câu ?";
                audioStatusLabel.setText(roundText);
                audioPlayer.play();
                playAudioButton.setDisable(false);
            } else {
                audioStatusLabel.setText("Không thể phát âm thanh");
                playAudioButton.setDisable(false);
            }
        } catch (Exception e) {
            System.err.println("[GameView] Error loading audio: " + e.getMessage());
            audioStatusLabel.setText("Lỗi: " + e.getMessage());
            playAudioButton.setDisable(false);
        }
    }

    private void startTimer() {
        remainingSeconds = 15;
        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        timerValueLabel.setText("15s");
        timerValueLabel.setTextFill(Color.rgb(250, 204, 21));
        timerValueLabel
                .setStyle("-fx-background-color: rgba(15,23,42,0.55); -fx-background-radius: 12; -fx-padding: 8 16; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 16, 0.2, 0, 6);");

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            timerValueLabel.setText(remainingSeconds + "s");

            if (remainingSeconds <= 5) {
                timerValueLabel.setTextFill(Color.RED);
            } else {
                timerValueLabel.setTextFill(Color.rgb(250, 204, 21));
            }

            if (remainingSeconds <= 0) {
                stopTimer();
                statusLabel.setText("Hết giờ!");
                disableOptions();
            }
        }));
        timerTimeline.setCycleCount(15);
        timerTimeline.play();
    }

    private void stopTimer() {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }
    }

    private void enableOptions() {
        option1Button.setDisable(false);
        option2Button.setDisable(false);
        option3Button.setDisable(false);
        option4Button.setDisable(false);
    }

    private void disableOptions() {
        option1Button.setDisable(true);
        option2Button.setDisable(true);
        option3Button.setDisable(true);
        option4Button.setDisable(true);
    }

    private void submitAnswer(int answerIndex) {
        controller.submitAnswer(answerIndex);
        disableOptions();
        statusIndicator.setVisible(true);
        statusIndicator.setProgress(-1); // Indeterminate
        statusLabel.setText("Đã gửi câu trả lời!");
    }

    private void updateScore() {
        int myScore = controller.getMyScore();
        int opponentScore = controller.getOpponentScore();

        // Cập nhật điểm số từ server (luôn đúng cho cả hai người chơi)
        myScoreValueLabel.setText(String.valueOf(myScore));
        opponentScoreValueLabel.setText(String.valueOf(opponentScore));

        System.out.println("[GameView] Score updated - My: " + myScore + ", Opponent: " + opponentScore);
    }

    private void updateProgress() {
        int myProgress = controller.getMyProgress();
        int opponentProgress = controller.getOpponentProgress();

        // Cập nhật số câu hiện tại của người chơi
        if (myProgress > 0) {
            myCurrentRoundLabel.setText("Câu " + myProgress);
        }

        // Cập nhật số câu hiện tại của đối thủ
        opponentCurrentRoundLabel.setText("Câu " + opponentProgress);
    }

    private Label statusLabel;

    private void handleLeaveMatch() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Rời trận");
        confirm.setHeaderText("Bạn muốn rời trận hiện tại?");
        confirm.setContentText("Bạn sẽ thua trận này và quay về sảnh.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                stopTimer();
                disableOptions();
                if (audioPlayer != null) {
                    audioPlayer.stop();
                }
                controller.leaveGame();
            }
        });
    }
}
