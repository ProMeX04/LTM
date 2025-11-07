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

    // Loading components
    private VBox loadingOverlay;
    private ProgressBar loadingProgressBar;
    private Label loadingStatusLabel;
    private boolean isLoading = false;

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
        setSpacing(12);
        setPadding(new Insets(16));
        setStyle("-fx-background-color: #0d1117;");

        // Header với progress bar cho round
        HBox header = createHeader();

        // Audio section (đã gộp score vào trong)
        VBox audioSection = createAudioSection();

        // Options grid
        VBox optionsSection = createOptionsSection();

        // Status với progress indicator
        HBox statusSection = createStatusSection();

        VBox mainContent = new VBox(12, audioSection, optionsSection, statusSection);
        mainContent.setAlignment(Pos.TOP_CENTER);
        mainContent.setPrefWidth(600);

        // Loading overlay
        loadingOverlay = createLoadingOverlay();

        // Sử dụng StackPane để overlay phủ lên toàn bộ nội dung
        StackPane contentStack = new StackPane();
        VBox contentBox = new VBox(12, header, mainContent);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentStack.getChildren().addAll(contentBox, loadingOverlay);

        getChildren().addAll(contentStack);
    }

    private VBox createLoadingOverlay() {
        VBox overlay = new VBox(16);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(13, 17, 23, 0.95); -fx-background-radius: 6; -fx-padding: 32;");
        overlay.setVisible(false);
        overlay.setManaged(false);
        // Đặt overlay để phủ toàn bộ StackPane
        overlay.setMaxWidth(Double.MAX_VALUE);
        overlay.setMaxHeight(Double.MAX_VALUE);
        StackPane.setAlignment(overlay, Pos.CENTER);

        loadingStatusLabel = new Label("Đang tải âm thanh...");
        loadingStatusLabel.setFont(Font.font("Poppins", FontWeight.SEMI_BOLD, 16));
        loadingStatusLabel.setTextFill(Color.web("#c9d1d9"));
        loadingStatusLabel.setAlignment(Pos.CENTER);

        loadingProgressBar = new ProgressBar();
        loadingProgressBar.setPrefWidth(400);
        loadingProgressBar.setPrefHeight(8);
        loadingProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        loadingProgressBar.setStyle("-fx-accent: #238636;");

        Button cancelButton = new Button("Hủy và quay lại sảnh");
        cancelButton.setOnAction(e -> controller.leaveGame());
        cancelButton.setStyle("-fx-background-color: #21262d; -fx-text-fill: #f85149; -fx-border-color: #30363d; -fx-border-width: 1px; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 16;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(
                "-fx-background-color: #da3633; -fx-text-fill: white; -fx-border-color: #da3633; -fx-border-width: 1px; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 16;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle(
                "-fx-background-color: #21262d; -fx-text-fill: #f85149; -fx-border-color: #30363d; -fx-border-width: 1px; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 16;"));

        overlay.getChildren().addAll(loadingStatusLabel, loadingProgressBar, cancelButton);
        return overlay;
    }

    private HBox createHeader() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Timer chỉ có label, không có progress bar
        timerValueLabel = new Label("15s");
        timerValueLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 20));
        timerValueLabel.setTextFill(Color.web("#d29922"));
        timerValueLabel
                .setStyle("-fx-background-color: #161b22; -fx-background-radius: 6; -fx-padding: 6 12; "
                        + "-fx-border-color: #30363d; -fx-border-width: 1px; -fx-border-radius: 6;");

        Button exitButton = new Button("Rời trận");
        exitButton.setStyle("-fx-background-color: #21262d; -fx-text-fill: #f85149; "
                + "-fx-border-color: #30363d; -fx-border-width: 1px; "
                + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 16;");
        exitButton.setOnMouseEntered(e -> exitButton.setStyle(
                "-fx-background-color: #da3633; -fx-text-fill: white; "
                        + "-fx-border-color: #da3633; -fx-border-width: 1px; "
                        + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 16;"));
        exitButton.setOnMouseExited(e -> exitButton.setStyle(
                "-fx-background-color: #21262d; -fx-text-fill: #f85149; "
                        + "-fx-border-color: #30363d; -fx-border-width: 1px; "
                        + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 16;"));
        exitButton.setOnAction(e -> handleLeaveMatch());

        HBox header = new HBox(20, spacer, timerValueLabel, exitButton);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }


    private VBox createAudioSection() {
        VBox audioCard = new VBox(12);
        audioCard.setAlignment(Pos.TOP_CENTER);
        audioCard.setStyle("-fx-background-color: #161b22; -fx-background-radius: 6; -fx-padding: 16; "
                + "-fx-border-color: #30363d; -fx-border-width: 1px; -fx-border-radius: 6;");

        // Audio status và button
        audioStatusLabel = new Label("Đang chờ...");
        audioStatusLabel.setFont(Font.font("Poppins", FontWeight.NORMAL, 12));
        audioStatusLabel.setTextFill(Color.web("#c9d1d9"));
        audioStatusLabel.setWrapText(true);
        audioStatusLabel.setAlignment(Pos.CENTER);

        playAudioButton = new Button("Nghe lại");
        playAudioButton
                .setStyle("-fx-background-color: #238636; -fx-text-fill: white;"
                        + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8 16;");
        playAudioButton.setOnMouseEntered(e -> playAudioButton.setStyle(
                "-fx-background-color: #2ea043; -fx-text-fill: white;"
                        + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8 16;"));
        playAudioButton.setOnMouseExited(e -> playAudioButton.setStyle(
                "-fx-background-color: #238636; -fx-text-fill: white;"
                        + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8 16;"));
        playAudioButton.setOnAction(e -> replayAudio());

        // Score section (gộp vào đây)
        VBox scoreSection = new VBox(8);
        scoreSection.setAlignment(Pos.CENTER);

        // Row 1: My player info
        HBox myRow = new HBox(12);
        myRow.setAlignment(Pos.CENTER);

        myPlayerNameLabel = new Label("Bạn");
        myPlayerNameLabel.setFont(Font.font("Poppins", FontWeight.SEMI_BOLD, 13));
        myPlayerNameLabel.setTextFill(Color.web("#c9d1d9"));
        myPlayerNameLabel.setPrefWidth(60);

        myCurrentRoundLabel = new Label("Câu 0");
        myCurrentRoundLabel.setFont(Font.font("Poppins", FontWeight.NORMAL, 12));
        myCurrentRoundLabel.setTextFill(Color.web("#8b949e"));
        myCurrentRoundLabel.setPrefWidth(50);

        myScoreValueLabel = new Label("0");
        myScoreValueLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 15));
        myScoreValueLabel.setTextFill(Color.web("#3fb950"));
        myScoreValueLabel.setPrefWidth(40);

        myRow.getChildren().addAll(myPlayerNameLabel, myCurrentRoundLabel, myScoreValueLabel);

        // Row 2: Opponent player info
        HBox opponentRow = new HBox(12);
        opponentRow.setAlignment(Pos.CENTER);

        opponentPlayerNameLabel = new Label("Đối thủ");
        opponentPlayerNameLabel.setFont(Font.font("Poppins", FontWeight.SEMI_BOLD, 13));
        opponentPlayerNameLabel.setTextFill(Color.web("#c9d1d9"));
        opponentPlayerNameLabel.setPrefWidth(60);

        opponentCurrentRoundLabel = new Label("Câu 0");
        opponentCurrentRoundLabel.setFont(Font.font("Poppins", FontWeight.NORMAL, 12));
        opponentCurrentRoundLabel.setTextFill(Color.web("#8b949e"));
        opponentCurrentRoundLabel.setPrefWidth(50);

        opponentScoreValueLabel = new Label("0");
        opponentScoreValueLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 15));
        opponentScoreValueLabel.setTextFill(Color.web("#f85149"));
        opponentScoreValueLabel.setPrefWidth(40);

        opponentRow.getChildren().addAll(opponentPlayerNameLabel, opponentCurrentRoundLabel, opponentScoreValueLabel);

        scoreSection.getChildren().addAll(myRow, opponentRow);

        audioCard.getChildren().addAll(audioStatusLabel, playAudioButton, scoreSection);

        return audioCard;
    }

    private VBox createOptionsSection() {
        GridPane optionsGrid = new GridPane();
        optionsGrid.setHgap(12);
        optionsGrid.setVgap(12);
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

        VBox optionsCard = new VBox(12, optionsGrid);
        optionsCard.setAlignment(Pos.TOP_CENTER);
        optionsCard.setStyle("-fx-background-color: #161b22; -fx-background-radius: 6; -fx-padding: 16; "
                + "-fx-border-color: #30363d; -fx-border-width: 1px; -fx-border-radius: 6;");

        return optionsCard;
    }

    private HBox createStatusSection() {
        statusIndicator = new ProgressIndicator();
        statusIndicator.setPrefSize(24, 24);
        statusIndicator.setStyle("-fx-progress-color: #3fb950;");
        statusIndicator.setVisible(false);

        statusLabel = new Label("Chờ âm thanh...");
        statusLabel.setFont(Font.font("Poppins", FontWeight.MEDIUM, 13));
        statusLabel.setTextFill(Color.web("#c9d1d9"));

        HBox statusBox = new HBox(10, statusIndicator, statusLabel);
        statusBox.setAlignment(Pos.CENTER);
        return statusBox;
    }

    private Button createOptionButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(240);
        button.setPrefHeight(70);
        button.setWrapText(true);
        button.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        button.setFont(Font.font("Poppins", FontWeight.SEMI_BOLD, 14));
        button.setStyle("-fx-background-color: #21262d; -fx-text-fill: #c9d1d9; "
                + "-fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #30363d; -fx-border-width: 1px; -fx-border-radius: 6;");

        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #30363d; -fx-text-fill: #c9d1d9; "
                + "-fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #58a6ff; -fx-border-width: 1px; -fx-border-radius: 6;"));

        button.setOnMouseExited(
                e -> button.setStyle("-fx-background-color: #21262d; -fx-text-fill: #c9d1d9; "
                        + "-fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #30363d; -fx-border-width: 1px; -fx-border-radius: 6;"));

        return button;
    }

    private void setupCallbacks() {
        // Khởi tạo điểm số khi game bắt đầu
        controller.setOnGameStart(() -> {
            updateScore();
            updateProgress();
            // Khi game bắt đầu, hiển thị loading overlay
            isLoading = true;
            showLoadingOverlay();
        });

        controller.setOnGameReady(() -> {
            // Khi game sẵn sàng (cả 2 người đã tải xong), ẩn loading overlay
            isLoading = false;
            hideLoadingOverlay();
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
                statusIndicator.setStyle("-fx-progress-color: #3fb950;");
                statusIndicator.setProgress(1.0);
                statusLabel.setText("Đúng! +" + points + " điểm");
                statusLabel.setTextFill(Color.web("#3fb950"));
            } else {
                statusIndicator.setStyle("-fx-progress-color: #f85149;");
                statusIndicator.setProgress(1.0);
                if (selectedAnswer == 0) {
                    statusLabel.setText("Hết giờ! 0 điểm");
                } else {
                    statusLabel.setText("Sai! 0 điểm");
                }
                statusLabel.setTextFill(Color.web("#f85149"));
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
            isLoading = true;
            showLoadingOverlay();
            loadingStatusLabel.setText("Đang tải âm thanh...");
            loadingProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            playAudioButton.setDisable(true);
        });

        controller.setOnPrefetchProgress(() -> {
            if (isLoading) {
                int total = Math.max(1, controller.getPrefetchTotal());
                int done = controller.getPrefetchCompleted();
                double progress = (double) done / (double) total;
                loadingStatusLabel.setText(String.format("Đang tải %d/%d tệp...", done, total));
                loadingProgressBar.setProgress(progress);
            }
        });

        controller.setOnPrefetchDone(() -> {
            isLoading = false;
            loadingStatusLabel.setText("Tải xong âm thanh. Đang chờ đối thủ...");
            loadingProgressBar.setProgress(1.0);
            // Overlay sẽ được ẩn khi game bắt đầu (onGameReady)
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
        statusLabel.setTextFill(Color.web("#c9d1d9"));
    }

    private void resetButtonStyles() {
        String defaultStyle = "-fx-background-color: #21262d; -fx-text-fill: #c9d1d9; "
                + "-fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #30363d; -fx-border-width: 1px; -fx-border-radius: 6;";
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
                    "-fx-background-color: #238636; -fx-text-fill: white; "
                            + "-fx-background-radius: 6; -fx-cursor: default; -fx-border-color: #3fb950; -fx-border-width: 1px; -fx-border-radius: 6;");
        } else {
            selectedButton.setStyle(
                    "-fx-background-color: #da3633; -fx-text-fill: white; "
                            + "-fx-background-radius: 6; -fx-cursor: default; -fx-border-color: #f85149; -fx-border-width: 1px; -fx-border-radius: 6;");
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
        timerValueLabel.setTextFill(Color.web("#d29922"));
        timerValueLabel
                .setStyle("-fx-background-color: #161b22; -fx-background-radius: 6; -fx-padding: 8 16; "
                        + "-fx-border-color: #30363d; -fx-border-width: 1px; -fx-border-radius: 6;");

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            timerValueLabel.setText(remainingSeconds + "s");

            if (remainingSeconds <= 5) {
                timerValueLabel.setTextFill(Color.web("#f85149"));
            } else {
                timerValueLabel.setTextFill(Color.web("#d29922"));
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

    private void showLoadingOverlay() {
        loadingOverlay.setVisible(true);
        loadingOverlay.setManaged(true);
        loadingOverlay.toFront();
    }

    private void hideLoadingOverlay() {
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);
    }

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
