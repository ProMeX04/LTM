package com.promex04.view;

import com.promex04.controller.GameController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginView extends VBox {
    private TextField usernameField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button registerButton;
    private Label errorLabel;
    private GameController controller;

    public LoginView(GameController controller) {
        this.controller = controller;
        initializeUI();
    }

    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(12);
        setPadding(new Insets(24));

        // Title
        Label titleLabel = new Label("Game Đoán Âm Thanh");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.getStyleClass().add("title");

        // Username field
        Label usernameLabel = new Label("Tên đăng nhập:");
        usernameLabel.setFont(Font.font("Arial", 14));

        usernameField = new TextField();
        usernameField.setPromptText("Nhập tên đăng nhập");
        usernameField.setPrefWidth(240);
        usernameField.setPrefHeight(34);

        // Password field
        Label passwordLabel = new Label("Mật khẩu:");
        passwordLabel.setFont(Font.font("Arial", 14));

        passwordField = new PasswordField();
        passwordField.setPromptText("Nhập mật khẩu");
        passwordField.setPrefWidth(240);
        passwordField.setPrefHeight(34);

        // Error label
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f85149;");
        errorLabel.setFont(Font.font("Arial", 11));
        errorLabel.setVisible(false);

        // Login button
        loginButton = new Button("Đăng nhập");
        loginButton.setPrefWidth(110);
        loginButton.setPrefHeight(36);
        loginButton.getStyleClass().add("primary");
        loginButton.setOnAction(e -> handleLogin());

        // Register button
        registerButton = new Button("Đăng ký");
        registerButton.setPrefWidth(110);
        registerButton.setPrefHeight(36);
        registerButton.getStyleClass().add("secondary");

        registerButton.setOnAction(e -> handleRegister());

        // Button container
        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(loginButton, registerButton);

        // Enter key support - chỉ đăng nhập khi nhấn Enter
        passwordField.setOnAction(e -> handleLogin());

        VBox formBox = new VBox(8);
        formBox.setAlignment(Pos.CENTER);
        formBox.setFillWidth(false);
        formBox.setMaxWidth(320);
        formBox.getChildren().addAll(titleLabel);

        formBox.getChildren().addAll(
                new Region() {
                    {
                        setPrefHeight(10);
                    }
                },
                usernameLabel,
                usernameField,
                passwordLabel,
                passwordField,
                errorLabel,
                buttonBox);

        getChildren().addAll(formBox);

        // Setup callbacks
        controller.setOnLoginSuccess(() -> {
            errorLabel.setVisible(false);
            // Navigation sẽ được xử lý bởi MainApp
        });

        controller.setOnLoginFailed(() -> {
            String errorMsg = controller.getErrorMessage();
            errorLabel.setText(errorMsg != null && !errorMsg.isEmpty() ? errorMsg : "Đăng nhập thất bại!");
            errorLabel.setVisible(true);
            loginButton.setDisable(false);
            registerButton.setDisable(false);
            loginButton.setText("Đăng nhập");
            registerButton.setText("Đăng ký");
        });

        controller.setOnError(() -> {
            errorLabel.setText(controller.getErrorMessage());
            errorLabel.setVisible(true);
            loginButton.setDisable(false);
            registerButton.setDisable(false);
            loginButton.setText("Đăng nhập");
            registerButton.setText("Đăng ký");
        });

        controller.setOnRegisterSuccess(() -> {
            errorLabel.setVisible(false);
            // Navigation sẽ được xử lý bởi MainApp
        });

        controller.setOnRegisterFailed(() -> {
            errorLabel.setText("Đăng ký thất bại: " + controller.getErrorMessage());
            errorLabel.setVisible(true);
            loginButton.setDisable(false);
            registerButton.setDisable(false);
            loginButton.setText("Đăng nhập");
            registerButton.setText("Đăng ký");
        });
    }

    private void handleLogin() {
        // Kiểm tra nếu đang xử lý request khác
        if (loginButton.isDisable() || registerButton.isDisable()) {
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Validation cơ bản
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            errorLabel.setVisible(true);
            return;
        }

        if (username.length() < 3 || username.length() > 20) {
            errorLabel.setText("Tên đăng nhập phải từ 3-20 ký tự!");
            errorLabel.setVisible(true);
            return;
        }

        if (password.length() < 3 || password.length() > 50) {
            errorLabel.setText("Mật khẩu phải từ 3-50 ký tự!");
            errorLabel.setVisible(true);
            return;
        }

        // Disable buttons ngay lập tức để tránh click nhiều lần
        loginButton.setDisable(true);
        registerButton.setDisable(true);
        loginButton.setText("Đang đăng nhập...");
        registerButton.setText("Đang đăng nhập...");
        errorLabel.setVisible(false);

        try {
            // Đảm bảo đã kết nối với server trước khi đăng nhập
            String host = com.promex04.ClientConfig.getDefaultHost();
            int port = com.promex04.ClientConfig.getDefaultPort();
            if (!controller.isConnected()) {
                controller.connect(host, port);
            }
            controller.login(username, password);
        } catch (Exception e) {
            errorLabel.setText("Không thể kết nối đến server: " + e.getMessage());
            errorLabel.setVisible(true);
            loginButton.setDisable(false);
            registerButton.setDisable(false);
            loginButton.setText("Đăng nhập");
            registerButton.setText("Đăng ký");
        }
    }

    private void handleRegister() {
        // Kiểm tra nếu đang xử lý request khác
        if (loginButton.isDisable() || registerButton.isDisable()) {
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            errorLabel.setVisible(true);
            return;
        }

        if (username.length() < 3 || username.length() > 20) {
            errorLabel.setText("Tên đăng nhập phải từ 3-20 ký tự!");
            errorLabel.setVisible(true);
            return;
        }

        if (password.length() < 3 || password.length() > 50) {
            errorLabel.setText("Mật khẩu phải từ 3-50 ký tự!");
            errorLabel.setVisible(true);
            return;
        }

        // Kiểm tra ký tự hợp lệ cho username (chỉ chữ cái, số, dấu gạch dưới)
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            errorLabel.setText("Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới!");
            errorLabel.setVisible(true);
            return;
        }

        // Disable buttons ngay lập tức để tránh click nhiều lần
        loginButton.setDisable(true);
        registerButton.setDisable(true);
        loginButton.setText("Đang đăng ký...");
        registerButton.setText("Đang đăng ký...");
        errorLabel.setVisible(false);

        try {
            // Đảm bảo đã kết nối với server trước khi đăng ký
            String host = com.promex04.ClientConfig.getDefaultHost();
            int port = com.promex04.ClientConfig.getDefaultPort();
            if (!controller.isConnected()) {
                controller.connect(host, port);
            }
            controller.register(username, password);
        } catch (Exception e) {
            errorLabel.setText("Không thể kết nối đến server: " + e.getMessage());
            errorLabel.setVisible(true);
            loginButton.setDisable(false);
            registerButton.setDisable(false);
            loginButton.setText("Đăng nhập");
            registerButton.setText("Đăng ký");
        }
    }

    public void reset() {
        usernameField.clear();
        passwordField.clear();
        errorLabel.setVisible(false);
        loginButton.setDisable(false);
        registerButton.setDisable(false);
        loginButton.setText("Đăng nhập");
        registerButton.setText("Đăng ký");
    }
}
