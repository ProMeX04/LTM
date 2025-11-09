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
import javafx.animation.PauseTransition;
import javafx.util.Duration;
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
    private String pendingSelectionUsername; // Lưu username được chọn để restore sau khi refresh

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
        logoutButton.setOnAction(e -> handleLogout());

        topBar.getChildren().addAll(userLabel, topSpacer, logoutButton);
        setTop(topBar);

        // === CỘT TRÁI - DANH SÁCH NGƯỜI CHƠI ===
        VBox leftBox = new VBox(12);
        leftBox.setPadding(new Insets(16));
        leftBox.setPrefWidth(450);

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

        // Lắng nghe thay đổi text để lọc danh sách
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsers(newValue);
        });

        userListView = new ListView<>(sortedUsers);
        userListView.setStyle("-fx-background-insets: 0; -fx-padding: 4;");
        userListView.setCellFactory(listView -> new UserListCell());
        Label placeholderLabel = new Label("Không tìm thấy người chơi");
        userListView.setPlaceholder(placeholderLabel);
        VBox.setVgrow(userListView, Priority.ALWAYS);

        challengeButton = new Button("Thách đấu");
        challengeButton.setPrefWidth(Double.MAX_VALUE);
        challengeButton.setPrefHeight(40);
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
                HBox.setHgrow(spacer, Priority.ALWAYS);
                container.getChildren().addAll(spacer, bubble);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
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
                            // Tin nhắn của mình: hiển thị bên phải
                            displayText = message;
                            container.setAlignment(Pos.CENTER_RIGHT);
                            container.getChildren().clear();
                            container.getChildren().addAll(bubble, spacer);
                        } else {
                            // Tin nhắn của người khác: hiển thị bên trái
                            displayText = username + ": " + message;
                            container.setAlignment(Pos.CENTER_LEFT);
                            container.getChildren().clear();
                            container.getChildren().addAll(bubble, spacer);
                        }
                    } else {
                        // Tin nhắn hệ thống: giữ nguyên
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

        Label rankingLabel = new Label("Bảng xếp hạng");
        rankingLabel.setFont(Font.font("Inter", FontWeight.BOLD, 16));

        // TableView cho bảng xếp hạng
        rankingTable = new TableView<>();
        rankingTable.setItems(controller.getRankingList());
        rankingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        Label rankingPlaceholder = new Label("Chưa có dữ liệu xếp hạng");
        rankingTable.setPlaceholder(rankingPlaceholder);
        VBox.setVgrow(rankingTable, Priority.ALWAYS);

        TableColumn<com.promex04.model.RankingEntry, Number> rankCol = new TableColumn<>("Hạng");
        rankCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getRank()));
        rankCol.setMaxWidth(150);
        rankCol.setMinWidth(100);

        TableColumn<com.promex04.model.RankingEntry, Number> winCol = new TableColumn<>("Số trận thắng");
        winCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getGamesWon()));

        rankingTable.getColumns().addAll(rankCol, winCol);

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
                // Restore selection sau khi refresh
                if (pendingSelectionUsername != null) {
                    restoreSelection(pendingSelectionUsername);
                }
            } else {
                // Không ở trong game: đồng bộ trạng thái 'Đã mời' theo controller
                String controllerInvitee = controller.getChallengeToUsername();
                if ((controllerInvitee == null && pendingInvitee.get() != null)
                        || (controllerInvitee != null && !controllerInvitee.equals(pendingInvitee.get()))) {
                    pendingInvitee.set(controllerInvitee);
                    userListView.refresh();
                    // Restore selection sau khi refresh
                    if (pendingSelectionUsername != null) {
                        restoreSelection(pendingSelectionUsername);
                    }
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
            // Restore selection sau khi refresh
            if (pendingSelectionUsername != null) {
                restoreSelection(pendingSelectionUsername);
            }
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

        // Lưu lại username để restore selection sau khi dialog đóng và sau các lần refresh
        String selectedUsername = selectedUser.getUsername();
        pendingSelectionUsername = selectedUsername;

        ChallengePreference preference = promptChallengePreference();
        if (preference == null) {
            // Nếu người dùng cancel dialog, restore lại selection và clear pending
            restoreSelection(selectedUsername);
            pendingSelectionUsername = null;
            return;
        }

        controller.challenge(selectedUsername, preference.artist(), preference.genre(),
                preference.totalRounds());
        // Không restore ngay ở đây vì CHALLENGE_SENT callback sẽ refresh và restore
        // pendingSelectionUsername sẽ được dùng trong callback để restore
    }

    private void restoreSelection(String username) {
        if (username == null) return;
        // Tìm lại user trong danh sách và restore selection
        // Sử dụng Platform.runLater với delay nhỏ để đảm bảo refresh đã hoàn tất
        Platform.runLater(() -> {
            // Delay nhỏ để đảm bảo refresh đã hoàn tất
            PauseTransition pause = new PauseTransition(Duration.millis(50));
            pause.setOnFinished(e -> {
                // Tìm user trong danh sách
                User userToSelect = null;
                for (User user : sortedUsers) {
                    if (user != null && username.equals(user.getUsername())) {
                        userToSelect = user;
                        break;
                    }
                }
                
                if (userToSelect != null) {
                    userListView.getSelectionModel().select(userToSelect);
                    // Scroll đến item được chọn để đảm bảo nó hiển thị
                    userListView.scrollTo(userToSelect);
                }
            });
            pause.play();
        });
    }

    private ChallengePreference promptChallengePreference() {
        Dialog<ChallengePreference> dialog = new Dialog<>();
        dialog.setTitle("Chọn chủ đề âm thanh");

        ButtonType sendButtonType = new ButtonType("Gửi lời mời", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);

        Button sendButton = (Button) dialog.getDialogPane().lookupButton(sendButtonType);
        if (sendButton != null) {
            sendButton.setDefaultButton(true);
        }

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.setCancelButton(true);
        }

        // Load danh sách khi mở dialog
        controller.requestAudioTags();
        
        // === CHỌN CA SĨ ===
        Label artistLabel = new Label("Ca sĩ:");
        artistLabel.setStyle("-fx-font-size: 13px;");
        
        TextField artistSearchField = new TextField();
        artistSearchField.setPromptText("Tìm kiếm ca sĩ...");
        artistSearchField.setPrefHeight(32);
        
        FilteredList<String> filteredArtists = new FilteredList<>(controller.getAvailableArtists(), p -> true);
        ListView<String> artistListView = new ListView<>(filteredArtists);
        artistListView.setPrefHeight(120);
        artistListView.setStyle("-fx-background-insets: 0; -fx-padding: 4;");
        
        // Filter danh sách dựa trên ô search
        artistSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filterText = newVal != null ? newVal.toLowerCase() : "";
            filteredArtists.setPredicate(item -> {
                if (filterText.isEmpty()) {
                    return true;
                }
                return item != null && item.toLowerCase().contains(filterText);
            });
        });
        
        // Label hiển thị giá trị đã chọn
        Label selectedArtistLabel = new Label("Chưa chọn");
        selectedArtistLabel.setStyle("-fx-font-size: 12px; -fx-padding: 4 0;");
        selectedArtistLabel.setWrapText(true);
        
        // Cập nhật label khi chọn
        artistListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedArtistLabel.setText("Đã chọn: " + newVal);
            } else {
                selectedArtistLabel.setText("Chưa chọn");
            }
        });
        
        VBox artistBox = new VBox(6, artistLabel, artistSearchField, artistListView, selectedArtistLabel);
        
        // === CHỌN THỂ LOẠI ===
        Label genreLabel = new Label("Thể loại:");
        genreLabel.setStyle("-fx-font-size: 13px;");
        
        TextField genreSearchField = new TextField();
        genreSearchField.setPromptText("Tìm kiếm thể loại...");
        genreSearchField.setPrefHeight(32);
        
        FilteredList<String> filteredGenres = new FilteredList<>(controller.getAvailableGenres(), p -> true);
        ListView<String> genreListView = new ListView<>(filteredGenres);
        genreListView.setPrefHeight(120);
        genreListView.setStyle("-fx-background-insets: 0; -fx-padding: 4;");
        
        // Filter danh sách dựa trên ô search
        genreSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filterText = newVal != null ? newVal.toLowerCase() : "";
            filteredGenres.setPredicate(item -> {
                if (filterText.isEmpty()) {
                    return true;
                }
                return item != null && item.toLowerCase().contains(filterText);
            });
        });
        
        // Label hiển thị giá trị đã chọn
        Label selectedGenreLabel = new Label("Chưa chọn");
        selectedGenreLabel.setStyle("-fx-font-size: 12px; -fx-padding: 4 0;");
        selectedGenreLabel.setWrapText(true);
        
        // Cập nhật label khi chọn
        genreListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedGenreLabel.setText("Đã chọn: " + newVal);
            } else {
                selectedGenreLabel.setText("Chưa chọn");
            }
        });
        
        VBox genreBox = new VBox(6, genreLabel, genreSearchField, genreListView, selectedGenreLabel);

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
        content.setPrefWidth(350);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);
        dialog.getDialogPane().setPrefHeight(600);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == sendButtonType) {
                int totalRounds = roundsSpinner.getValue() != null ? roundsSpinner.getValue() : 15;
                // Lấy giá trị từ ListView
                String selectedArtist = artistListView.getSelectionModel().getSelectedItem();
                String selectedGenre = genreListView.getSelectionModel().getSelectedItem();
                return new ChallengePreference(
                        trimToNull(selectedArtist),
                        trimToNull(selectedGenre),
                        totalRounds);
            }
            return null;
        });

        Optional<ChallengePreference> result = dialog.showAndWait();
        return result.orElse(null);
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
            nameLabel.setStyle("-fx-font-weight: bold;");
            scoreLabel.setStyle("-fx-font-size: 11px;");
            preferenceLabel.setStyle("-fx-font-size: 11px;");
            preferenceLabel.setVisible(false);
            preferenceLabel.setManaged(false);
            statusPill.setStyle(
                    "-fx-background-radius: 6; -fx-padding: 2 8 2 8; -fx-font-size: 11px;");
            nameScoreBox.getChildren().addAll(nameLabel, scoreLabel, preferenceLabel);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            // Nút hành động khi nhận thách đấu
            acceptButton.setStyle(
                    "-fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold;");
            rejectButton.setStyle(
                    "-fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold;");
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
            } else {
                String status = user.getStatus();
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
                        "-fx-background-radius: 6; -fx-padding: 2 8 2 8; -fx-font-size: 11px;");
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
                setStyle("-fx-background-radius: 4; -fx-padding: 6 8 6 8;");
            } else {
                setStyle("-fx-padding: 6 8 6 8;");
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
