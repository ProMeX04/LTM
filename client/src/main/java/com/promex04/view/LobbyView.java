package com.promex04.view;

import com.promex04.controller.GameController;
import com.promex04.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Spinner;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.Circle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.application.Platform;
import java.util.Optional;

public class LobbyView extends BorderPane {
    private GameController controller;
    private ListView<User> userListView;
    private ListView<String> chatListView;
    private TextField chatInputField;
    private TextField searchField;
    private Button challengeButton;
    private TableView<com.promex04.model.RankingEntry> rankingTable;
    private FilteredList<User> filteredUsers;
    private SortedList<User> sortedUsers;
    private String currentChallenger; // người thách đấu mình (incoming)
    private final StringProperty pendingInvitee = new SimpleStringProperty(); // người mình đã mời (outgoing)
    private Label userLabel;

    public LobbyView(GameController controller) {
        this.controller = controller;
        initializeUI();
        setupCallbacks();
    }

    private void initializeUI() {
        setPadding(new Insets(12));

        // === THANH TRÊN CÙNG - THÔNG TIN VÀ ĐĂNG XUẤT ===
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(8, 12, 12, 12));

        userLabel = new Label("Xin chào");
        userLabel.setFont(Font.font("Inter", FontWeight.BOLD, 14));

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button logoutButton = new Button("Đăng xuất");
        logoutButton.getStyleClass().addAll("danger", "small");
        logoutButton.setOnAction(e -> handleLogout());

        topBar.getChildren().addAll(userLabel, topSpacer, logoutButton);
        setTop(topBar);

        // === CỘT TRÁI - DANH SÁCH NGƯỜI CHƠI ===
        VBox leftBox = new VBox(12);
        leftBox.setPadding(new Insets(16));
        leftBox.setPrefWidth(450);
        leftBox.getStyleClass().add("card");

        Label userListLabel = new Label("Danh sách người chơi");
        userListLabel.setFont(Font.font("Inter", FontWeight.BOLD, 16));

        // Tạo FilteredList để lọc danh sách người chơi
        filteredUsers = new FilteredList<>(controller.getLobbyUsers(), p -> true);
        // Bọc bởi SortedList để sắp xếp (ưu tiên người thách đấu lên đầu)
        sortedUsers = new SortedList<>(filteredUsers);
        sortedUsers.setComparator(getUserComparator());

        // Ô tìm kiếm
        searchField = new TextField();
        searchField.setPromptText("Tìm kiếm người chơi...");
        searchField.setPrefHeight(40);
        searchField.getStyleClass().add("search");

        // Lắng nghe thay đổi text để lọc danh sách
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsers(newValue);
        });

        userListView = new ListView<>(sortedUsers);
        userListView.setStyle("-fx-background-insets: 0; -fx-padding: 4;");
        userListView.setCellFactory(listView -> new UserListCell());
        Label placeholderLabel = new Label("Không tìm thấy người chơi");
        placeholderLabel.getStyleClass().add("placeholder");
        userListView.setPlaceholder(placeholderLabel);
        VBox.setVgrow(userListView, Priority.ALWAYS);

        challengeButton = new Button("Thách đấu");
        challengeButton.setPrefWidth(Double.MAX_VALUE);
        challengeButton.setPrefHeight(40);
        challengeButton.getStyleClass().add("primary");
        challengeButton.setOnAction(e -> handleChallenge());
        challengeButton.disableProperty().bind(
                Bindings.createBooleanBinding(() -> {
                    User sel = userListView.getSelectionModel().getSelectedItem();
                    if (sel == null)
                        return true;
                    String invitee = pendingInvitee.get();
                    if (invitee != null && sel.getUsername().equals(invitee))
                        return true; // đang chờ đối thủ
                    return false;
                },
                        userListView.getSelectionModel().selectedItemProperty(), pendingInvitee));

        leftBox.getChildren().addAll(userListLabel, searchField, userListView, challengeButton);
        setLeft(leftBox);
        BorderPane.setMargin(leftBox, new Insets(0, 12, 0, 0)); // Thêm khoảng cách bên phải cột trái

        // === CỘT GIỮA - CHAT ===
        VBox centerBox = new VBox(12);
        centerBox.setPadding(new Insets(16));
        centerBox.getStyleClass().add("card");

        Label chatLabel = new Label("Trò chuyện");
        chatLabel.setFont(Font.font("Inter", FontWeight.BOLD, 16));

        chatListView = new ListView<>(controller.getChatMessages());
        Label chatPlaceholder = new Label("Chưa có tin nhắn");
        chatPlaceholder.getStyleClass().add("placeholder");
        chatListView.setPlaceholder(chatPlaceholder);
        chatListView.setStyle("-fx-background-insets: 0;");
        chatListView.setCellFactory(listView -> new ListCell<>() {
            private final HBox container = new HBox();
            private final Label bubble = new Label();
            private final Region spacer = new Region();
            {
                container.setSpacing(8);
                container.setAlignment(Pos.CENTER_LEFT);
                bubble.setWrapText(true);
                bubble.setMaxWidth(400);
                bubble.getStyleClass().add("chat-bubble");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                container.getChildren().addAll(spacer, bubble);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: #0d1117;");
                } else {
                    setStyle("-fx-background-color: #0d1117;");
                    // Phân tích tin nhắn: format là "username: message"
                    String currentUsername = controller.getCurrentUsername();
                    boolean isMyMessage = false;
                    String displayText = item;

                    if (item.contains(": ")) {
                        int colonIndex = item.indexOf(": ");
                        String username = item.substring(0, colonIndex);
                        String message = item.substring(colonIndex + 2);

                        isMyMessage = currentUsername != null && username.equals(currentUsername);

                        if (isMyMessage) {
                            // Tin nhắn của mình: hiển thị bên phải, màu xanh GitHub
                            displayText = message;
                            bubble.getStyleClass().setAll("chat-bubble", "my-message");
                            container.setAlignment(Pos.CENTER_RIGHT);
                            container.getChildren().clear();
                            container.getChildren().addAll(bubble, spacer);
                        } else {
                            // Tin nhắn của người khác: hiển thị bên trái, màu xám GitHub
                            displayText = username + ": " + message;
                            bubble.getStyleClass().setAll("chat-bubble", "other-message");
                            container.setAlignment(Pos.CENTER_LEFT);
                            container.getChildren().clear();
                            container.getChildren().addAll(bubble, spacer);
                        }
                    } else {
                        // Tin nhắn hệ thống: giữ nguyên
                        bubble.getStyleClass().setAll("chat-bubble", "system");
                        container.setAlignment(Pos.CENTER_LEFT);
                        container.getChildren().clear();
                        container.getChildren().addAll(bubble, spacer);
                    }

                    bubble.setText(displayText);
                    setGraphic(container);
                }
            }
        });
        VBox.setVgrow(chatListView, Priority.ALWAYS); // <-- QUAN TRỌNG: Cho phép co giãn dọc

        HBox chatInputBox = new HBox(12);
        chatInputBox.setAlignment(Pos.CENTER_RIGHT);
        chatInputField = new TextField();
        chatInputField.setPromptText("Nhập tin nhắn...");
        chatInputField.setPrefHeight(45);
        chatInputField.getStyleClass().add("chat-input");
        chatInputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 200) {
                chatInputField.setText(oldVal);
            }
        });
        HBox.setHgrow(chatInputField, Priority.ALWAYS);

        Button sendButton = new Button("Send");
        sendButton.setPrefWidth(50);
        sendButton.setPrefHeight(45);
        sendButton.getStyleClass().addAll("primary", "large");
        sendButton.setOnAction(e -> handleSendMessage());
        sendButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> chatInputField.getText() == null || chatInputField.getText().trim().isEmpty(),
                        chatInputField.textProperty()));

        chatInputField.setOnAction(e -> {
            if (!chatInputField.getText().trim().isEmpty()) {
                handleSendMessage();
            }
        });
        chatInputBox.getChildren().addAll(chatInputField, sendButton);

        centerBox.getChildren().addAll(chatLabel, chatListView, chatInputBox);
        setCenter(centerBox); // <-- QUAN TRỌNG: Đặt chat vào GIỮA

        // === CỘT PHẢI - BẢNG XẾP HẠNG ===
        VBox rightBox = new VBox(12);
        rightBox.setPadding(new Insets(16));
        rightBox.setPrefWidth(380);
        rightBox.getStyleClass().add("card");

        Label rankingLabel = new Label("Bảng xếp hạng");
        rankingLabel.setFont(Font.font("Inter", FontWeight.BOLD, 16));
        rankingLabel.getStyleClass().add("title");

        // TableView cho bảng xếp hạng
        rankingTable = new TableView<>();
        rankingTable.setItems(controller.getRankingList());
        rankingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        Label rankingPlaceholder = new Label("Chưa có dữ liệu xếp hạng");
        rankingPlaceholder.getStyleClass().add("placeholder");
        rankingTable.setPlaceholder(rankingPlaceholder);
        VBox.setVgrow(rankingTable, Priority.ALWAYS);

        TableColumn<com.promex04.model.RankingEntry, Number> rankCol = new TableColumn<>("Hạng");
        rankCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getRank()));
        rankCol.setMaxWidth(150);
        rankCol.setMinWidth(100);
        rankCol.setCellFactory(column -> new javafx.scene.control.TableCell<com.promex04.model.RankingEntry, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #0d1117;");
                } else {
                    setText(item.toString());
                    setTextFill(Color.WHITE);
                    updateRowStyle();
                }
            }

            private void updateRowStyle() {
                javafx.scene.control.TableRow<com.promex04.model.RankingEntry> row = getTableRow();
                if (row != null) {
                    if (row.isSelected()) {
                        setStyle("-fx-background-color: #1f6feb; -fx-text-fill: white;");
                        setTextFill(Color.WHITE);
                    } else {
                        setStyle("-fx-background-color: #0d1117; -fx-text-fill: white;");
                        setTextFill(Color.WHITE);
                    }
                } else {
                    setStyle("-fx-background-color: #0d1117; -fx-text-fill: white;");
                    setTextFill(Color.WHITE);
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                updateRowStyle();
            }
        });

        TableColumn<com.promex04.model.RankingEntry, Number> winCol = new TableColumn<>("Số trận thắng");
        winCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getGamesWon()));
        winCol.setCellFactory(column -> new javafx.scene.control.TableCell<com.promex04.model.RankingEntry, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #0d1117;");
                } else {
                    setText(item.toString());
                    setTextFill(Color.WHITE);
                    updateRowStyle();
                }
            }

            private void updateRowStyle() {
                javafx.scene.control.TableRow<com.promex04.model.RankingEntry> row = getTableRow();
                if (row != null) {
                    if (row.isSelected()) {
                        setStyle("-fx-background-color: #1f6feb; -fx-text-fill: white;");
                        setTextFill(Color.WHITE);
                    } else {
                        setStyle("-fx-background-color: #0d1117; -fx-text-fill: white;");
                        setTextFill(Color.WHITE);
                    }
                } else {
                    setStyle("-fx-background-color: #0d1117; -fx-text-fill: white;");
                    setTextFill(Color.WHITE);
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                updateRowStyle();
            }
        });

        // Row factory được handle bởi CSS

        rankingTable.getColumns().addAll(rankCol, winCol);

        // Function to style column headers
        Runnable styleHeaders = () -> {
            rankingTable.lookupAll(".column-header").forEach(node -> {
                node.setStyle("-fx-background-color: #161b22; -fx-text-fill: white;");
                // Style all text nodes inside column header
                javafx.scene.Node label = node.lookup(".label");
                if (label != null) {
                    label.setStyle("-fx-text-fill: white;");
                    if (label instanceof javafx.scene.control.Label) {
                        ((javafx.scene.control.Label) label).setTextFill(Color.WHITE);
                    }
                }
                // Also try to find text nodes directly
                node.lookupAll(".text").forEach(textNode -> {
                    textNode.setStyle("-fx-fill: white;");
                });
            });
        };

        // Apply style immediately and also after scene is shown
        Platform.runLater(() -> {
            styleHeaders.run();
            // Also add listener for when TableView is shown
            rankingTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    Platform.runLater(() -> {
                        styleHeaders.run();
                    });
                }
            });
        });

        rightBox.getChildren().addAll(rankingLabel, rankingTable);
        setRight(rightBox);
        BorderPane.setMargin(rightBox, new Insets(0, 0, 0, 12)); // Thêm khoảng cách bên trái cột phải
    }

    // ... (Toàn bộ phần code còn lại: setupCallbacks, handleSendMessage,
    // handleChallenge, showAlert, UserListCell)
    // ... (KHÔNG THAY ĐỔI)
    private void setupCallbacks() {
        controller.setOnLobbyUpdate(() -> {
            // ListView sẽ tự động cập nhật vì nó bind với ObservableList
            String name = controller.getCurrentUsername();
            if (name != null && !name.isEmpty()) {
                userLabel.setText("Xin chào, " + name);
            }

            // Nếu đã vào game, xóa trạng thái mời/được mời để tránh hiển thị sai khi quay
            // lại lobby
            if (controller.isInGame()) {
                currentChallenger = null;
                pendingInvitee.set(null);
                sortedUsers.setComparator(getUserComparator());
                userListView.refresh();
            } else {
                // Không ở trong game: đồng bộ trạng thái 'Đã mời' theo controller
                String controllerInvitee = controller.getChallengeToUsername();
                if ((controllerInvitee == null && pendingInvitee.get() != null)
                        || (controllerInvitee != null && !controllerInvitee.equals(pendingInvitee.get()))) {
                    pendingInvitee.set(controllerInvitee);
                    userListView.refresh();
                }
            }
        });

        controller.setOnChatUpdate(() -> {
            // Tự động cuộn xuống cuối khi có tin nhắn mới
            int size = chatListView.getItems() != null ? chatListView.getItems().size() : 0;
            if (size > 0) {
                chatListView.scrollTo(size - 1);
            }
        });

        controller.setOnRankingUpdate(() -> {
            // TableView đã bind items; chỉ cần refresh nếu cần
            rankingTable.refresh();
            // Style column headers again after update
            Platform.runLater(() -> {
                rankingTable.lookupAll(".column-header").forEach(node -> {
                    node.setStyle("-fx-background-color: #161b22; -fx-text-fill: white;");
                    javafx.scene.Node label = node.lookup(".label");
                    if (label != null) {
                        label.setStyle("-fx-text-fill: white;");
                        if (label instanceof javafx.scene.control.Label) {
                            ((javafx.scene.control.Label) label).setTextFill(Color.WHITE);
                        }
                    }
                    node.lookupAll(".text").forEach(textNode -> {
                        textNode.setStyle("-fx-fill: white;");
                    });
                });
            });
        });

        controller.requestRanking();
        controller.requestAudioTags();

        controller.setOnLeftMatch(() -> {
            currentChallenger = null;
            pendingInvitee.set(null);
            sortedUsers.setComparator(getUserComparator());
            userListView.refresh();
        });

        controller.setOnChallengeReceived(() -> {
            // Lưu người thách đấu và hiển thị nút Chấp nhận/Từ chối ngay trên item tương
            // ứng
            currentChallenger = controller.getChallengeFromUsername();
            // Cập nhật sắp xếp để đưa người thách đấu lên đầu danh sách
            sortedUsers.setComparator(getUserComparator());
            userListView.refresh();
            // Cuộn lên đầu để người dùng dễ thấy
            if (!sortedUsers.isEmpty()) {
                userListView.scrollTo(0);
            }
        });

        controller.setOnChallengeSent(() -> {
            pendingInvitee.set(controller.getChallengeToUsername());
            userListView.refresh();
        });

        controller.setOnChallengeRejected(() -> {
            // Nếu người từ chối là người mình đã mời thì clear trạng thái để có thể mời lại
            String rejectedBy = controller.getChallengeRejectedBy();
            String invitee = pendingInvitee.get();
            if (rejectedBy != null && rejectedBy.equals(invitee)) {
                pendingInvitee.set(null);
                userListView.refresh();
            }
        });
    }

    private void handleSendMessage() {
        String message = chatInputField.getText().trim();
        if (!message.isEmpty()) {
            controller.sendChatMessage(message);
            chatInputField.clear();
        }
    }

    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText("Bạn có chắc muốn đăng xuất?");
        confirm.setContentText("Bạn sẽ quay lại màn hình đăng nhập.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                controller.logout();
            }
        });
    }

    private void handleChallenge() {
        User selectedUser = userListView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Chọn người chơi", "Vui lòng chọn một người chơi để thách đấu!");
            return;
        }

        if ("bận".equals(selectedUser.getStatus())) {
            showAlert("Không thể thách đấu", "Người chơi này đang trong game!");
            return;
        }

        if (selectedUser.getUsername().equals(controller.getCurrentUsername())) {
            showAlert("Lỗi", "Bạn không thể thách đấu chính mình!");
            return;
        }

        ChallengePreference preference = promptChallengePreference();
        if (preference == null) {
            return;
        }

        controller.challenge(selectedUser.getUsername(), preference.artist(), preference.genre(),
                preference.totalRounds());
        // Không hiển thị dialog; CHALLENGE_SENT từ server sẽ cập nhật UI "Đã mời"
    }

    private ChallengePreference promptChallengePreference() {
        Dialog<ChallengePreference> dialog = new Dialog<>();
        dialog.setTitle("Chọn chủ đề âm thanh");

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/github-dark.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("compact-dialog");

        ButtonType sendButtonType = new ButtonType("Gửi lời mời", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);

        Button sendButton = (Button) dialog.getDialogPane().lookupButton(sendButtonType);
        if (sendButton != null) {
            sendButton.getStyleClass().addAll("primary", "small");
            sendButton.setDefaultButton(true);
        }

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.getStyleClass().addAll("secondary", "small");
            cancelButton.setCancelButton(true);
        }

        ComboBox<String> artistBox = new ComboBox<>(controller.getAvailableArtists());
        artistBox.setEditable(true);
        artistBox.setPromptText("Ca sĩ (để trống = bất kỳ)");
        prepareInputControl(artistBox);

        ComboBox<String> genreBox = new ComboBox<>(controller.getAvailableGenres());
        genreBox.setEditable(true);
        genreBox.setPromptText("Thể loại (để trống = bất kỳ)");
        prepareInputControl(genreBox);

        Spinner<Integer> roundsSpinner = new Spinner<>(5, 50, 15, 5);
        roundsSpinner.setEditable(true);
        prepareInputControl(roundsSpinner);

        roundsSpinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                if (!newVal.isEmpty()) {
                    int value = Integer.parseInt(newVal);
                    if (value < 5) {
                        roundsSpinner.getEditor().setText("5");
                    } else if (value > 50) {
                        roundsSpinner.getEditor().setText("50");
                    }
                }
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        });

        VBox content = new VBox(12, artistBox, genreBox, roundsSpinner);
        content.getStyleClass().add("compact-dialog-body");
        content.setPrefWidth(280);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(320);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == sendButtonType) {
                int totalRounds = roundsSpinner.getValue() != null ? roundsSpinner.getValue() : 15;
                return new ChallengePreference(
                        trimToNull(artistBox.getEditor().getText()),
                        trimToNull(genreBox.getEditor().getText()),
                        totalRounds);
            }
            return null;
        });

        Optional<ChallengePreference> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void prepareInputControl(ComboBox<String> comboBox) {
        comboBox.getStyleClass().add("audio-dialog-input");
        comboBox.setMaxWidth(Double.MAX_VALUE);
    }

    private void prepareInputControl(Spinner<Integer> spinner) {
        spinner.getStyleClass().add("audio-dialog-input");
        spinner.setMaxWidth(Double.MAX_VALUE);
        spinner.getEditor().getStyleClass().add("audio-dialog-input-editor");
    }

    private String displayValue(String value) {
        return value == null || value.isBlank() ? "Bất kỳ" : value;
    }

    private String formatPreferenceText(String artist, String genre) {
        return "🎤 Ca sĩ: " + displayValue(artist) + "   •   🎧 Thể loại: " + displayValue(genre);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Lọc danh sách người chơi dựa trên từ khóa tìm kiếm (tìm kiếm gần đúng)
     */
    private void filterUsers(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredUsers.setPredicate(user -> {
                if (user == null)
                    return false;
                // Luôn hiển thị người thách đấu hoặc người đã mời nếu có
                if (currentChallenger != null && currentChallenger.equals(user.getUsername()))
                    return true;
                String invitee = pendingInvitee.get();
                if (invitee != null && invitee.equals(user.getUsername()))
                    return true;
                return true;
            });
        } else {
            String lowerSearchText = searchText.toLowerCase().trim();
            filteredUsers.setPredicate(user -> {
                if (user == null)
                    return false;

                // Luôn hiển thị người thách đấu nếu có
                if (currentChallenger != null && currentChallenger.equals(user.getUsername()))
                    return true;
                String invitee = pendingInvitee.get();
                if (invitee != null && invitee.equals(user.getUsername()))
                    return true;

                String username = user.getUsername().toLowerCase();
                String score = String.valueOf(user.getTotalScore());

                // Tìm kiếm gần đúng: kiểm tra username có chứa từ khóa
                // hoặc tìm theo từng từ trong từ khóa
                String[] searchWords = lowerSearchText.split("\\s+");

                // Kiểm tra username có chứa tất cả các từ trong từ khóa
                boolean matchesUsername = true;
                for (String word : searchWords) {
                    if (!username.contains(word)) {
                        matchesUsername = false;
                        break;
                    }
                }

                // Hoặc kiểm tra điểm số có chứa từ khóa
                boolean matchesScore = score.contains(lowerSearchText);

                return matchesUsername || matchesScore;
            });
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record ChallengePreference(String artist, String genre, int totalRounds) {
    }

    // Comparator ưu tiên người thách đấu lên đầu, còn lại sắp theo tên
    private java.util.Comparator<User> getUserComparator() {
        return (u1, u2) -> {
            boolean u1IsChallenger = currentChallenger != null && u1 != null
                    && currentChallenger.equals(u1.getUsername());
            boolean u2IsChallenger = currentChallenger != null && u2 != null
                    && currentChallenger.equals(u2.getUsername());
            if (u1IsChallenger && !u2IsChallenger)
                return -1;
            if (!u1IsChallenger && u2IsChallenger)
                return 1;
            // Nếu cùng mức ưu tiên, sắp xếp theo tên cho ổn định
            String n1 = u1 != null ? u1.getUsername() : "";
            String n2 = u2 != null ? u2.getUsername() : "";
            return n1.compareToIgnoreCase(n2);
        };
    }

    // Xóa trạng thái thách đấu để ẩn nút chấp nhận/từ chối
    private void clearCurrentChallenger() {
        currentChallenger = null;
        sortedUsers.setComparator(getUserComparator());
        userListView.refresh();
    }

    // Custom ListCell cho User
    private class UserListCell extends ListCell<User> {
        private final HBox row = new HBox(10);
        private final Circle avatar = new Circle(16);
        private final VBox nameScoreBox = new VBox(2);
        private final Label nameLabel = new Label();
        private final Label scoreLabel = new Label();
        private final Label statusPill = new Label();
        private final Label preferenceLabel = new Label();
        private final HBox actionBox = new HBox(6);
        private final Button acceptButton = new Button("Chấp nhận");
        private final Button rejectButton = new Button("Từ chối");

        {
            row.setAlignment(Pos.CENTER_LEFT);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #c9d1d9;");
            scoreLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");
            preferenceLabel.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 11px;");
            preferenceLabel.setVisible(false);
            preferenceLabel.setManaged(false);
            statusPill.setStyle(
                    "-fx-background-radius: 6; -fx-padding: 2 8 2 8; -fx-text-fill: white; -fx-font-size: 11px;");
            nameScoreBox.getChildren().addAll(nameLabel, scoreLabel, preferenceLabel);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            // Nút hành động khi nhận thách đấu
            acceptButton.setStyle(
                    "-fx-background-color: #238636; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold;");
            rejectButton.setStyle(
                    "-fx-background-color: #da3633; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold;");
            acceptButton.setOnAction(e -> {
                controller.respondToChallenge(true);
                clearCurrentChallenger();
            });
            rejectButton.setOnAction(e -> {
                controller.respondToChallenge(false);
                clearCurrentChallenger();
            });
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            actionBox.getChildren().addAll(acceptButton, rejectButton);

            row.getChildren().addAll(avatar, nameScoreBox, spacer, statusPill, actionBox);
        }

        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
                setStyle("-fx-background-color: #0d1117;");
            } else {
                String status = user.getStatus();
                String statusColor = "rỗi".equals(status) ? "#238636" : "#d29922";
                String statusText = "rỗi".equals(status) ? "Rảnh" : "Bận";

                // Avatar màu dựa vào hash username
                int hash = Math.abs(user.getUsername().hashCode());
                Color color = Color.hsb((hash % 360), 0.55, 0.85);
                avatar.setFill(color);

                nameLabel.setText(user.getUsername());
                scoreLabel.setText(user.getTotalScore() + " điểm");
                // Nếu là người mình đã mời, hiển thị pill "Đã mời"
                boolean isInvitee = pendingInvitee.get() != null && pendingInvitee.get().equals(user.getUsername());
                statusPill.setText(isInvitee ? "Đã mời" : statusText);
                statusPill.setStyle(
                        "-fx-background-radius: 6; -fx-padding: 2 8 2 8; -fx-text-fill: white; -fx-font-size: 11px;" +
                                "-fx-background-color: " + (isInvitee ? "#1f6feb" : statusColor) + ";");
                // Hiển thị nút chấp nhận/từ chối nếu đây là người thách đấu
                boolean isChallenger = currentChallenger != null && currentChallenger.equals(user.getUsername());
                actionBox.setVisible(isChallenger);
                actionBox.setManaged(isChallenger);
                // Khi là người thách đấu (incoming) thì hiển thị nút; ẩn pill
                statusPill.setVisible(!isChallenger);
                statusPill.setManaged(!isChallenger);
                if (isChallenger) {
                    preferenceLabel.setText(formatPreferenceText(
                            controller.getChallengeArtist(), controller.getChallengeGenre()));
                    preferenceLabel.setVisible(true);
                    preferenceLabel.setManaged(true);
                } else {
                    preferenceLabel.setVisible(false);
                    preferenceLabel.setManaged(false);
                }

                setGraphic(row);
                updateCellStyle();
            }
        }

        private void updateCellStyle() {
            if (isSelected()) {
                setStyle("-fx-background-color: #1f6feb; -fx-background-radius: 4; -fx-padding: 6 8 6 8;");
            } else {
                setStyle("-fx-background-color: #0d1117; -fx-padding: 6 8 6 8;");
            }
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            if (getItem() != null) {
                updateCellStyle();
            }
        }
    }
}
