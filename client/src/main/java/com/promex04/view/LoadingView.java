package com.promex04.view;

import com.promex04.controller.GameController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoadingView extends VBox {
    private final GameController controller;
    private final Label title;
    private final Label status;
    private final Label currentFile;
    private final ProgressBar overallProgress;
    private final ProgressBar fileProgress;
    private final TextArea debugArea;
    private final Button cancelButton;

    public LoadingView(GameController controller) {
        this.controller = controller;
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(32));
        setStyle("-fx-background-color: linear-gradient(to bottom, #1e293b, #0f172a);");

        title = new Label("Đang chuẩn bị trận đấu");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);

        status = new Label("Đang tải âm thanh...");
        status.setFont(Font.font("Arial", 14));
        status.setTextFill(Color.LIGHTGRAY);

        currentFile = new Label("");
        currentFile.setFont(Font.font("Arial", 12));
        currentFile.setTextFill(Color.LIGHTGRAY);

        overallProgress = new ProgressBar();
        overallProgress.setPrefWidth(500);
        overallProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        fileProgress = new ProgressBar();
        fileProgress.setPrefWidth(500);
        fileProgress.setProgress(0);

        cancelButton = new Button("Hủy và quay lại sảnh");
        cancelButton.setOnAction(e -> controller.leaveGame());
        cancelButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");

        debugArea = new TextArea();
        debugArea.setEditable(false);
        debugArea.setPrefRowCount(8);
        debugArea.setWrapText(true);
        debugArea.setStyle("-fx-control-inner-background:#0b1220; -fx-text-fill:#9ca3af; -fx-font-size:12px;");

        getChildren().addAll(title, status, currentFile, overallProgress, fileProgress, debugArea, cancelButton);

        hookCallbacks();
    }

    private void hookCallbacks() {
        controller.setOnPrefetchStart(() -> {
            status.setText("Đang tải âm thanh...");
            currentFile.setText("");
            overallProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            fileProgress.setProgress(0);
            debugArea.clear();
        });

        controller.setOnPrefetchProgress(() -> {
            int total = Math.max(1, controller.getPrefetchTotal());
            int done = controller.getPrefetchCompleted();
            String path = controller.getPrefetchCurrentPath();
            String name = (path == null) ? "" : extractFileName(path);
            long cur = controller.getPrefetchCurrentBytes();
            long all = controller.getPrefetchCurrentTotalBytes();
            int percent = controller.getPrefetchCurrentPercent();
            double overall = (done + Math.min(99, percent) / 100.0) / (double) total;
            status.setText(String.format("Đang tải %d/%d tệp...", done, total));
            currentFile.setText(all > 0 ?
                    String.format("Tệp hiện tại: %s — %d%% (%,d/%,d bytes)", name, percent, cur, all) :
                    String.format("Tệp hiện tại: %s — %,d bytes", name, cur));
            overallProgress.setProgress(overall);
            fileProgress.setProgress(all > 0 ? percent / 100.0 : ProgressBar.INDETERMINATE_PROGRESS);

            // Cập nhật debug lines
            StringBuilder sb = new StringBuilder();
            for (String line : controller.getPrefetchLogs()) {
                sb.append(line).append('\n');
            }
            debugArea.setText(sb.toString());
            debugArea.setScrollTop(Double.MAX_VALUE);
        });

        controller.setOnPrefetchDone(() -> {
            status.setText("Tải xong âm thanh. Đang chờ đối thủ...");
            overallProgress.setProgress(1.0);
            fileProgress.setProgress(1.0);
        });
    }

    private String extractFileName(String serverPath) {
        if (serverPath == null) return "(unknown)";
        int idx = serverPath.lastIndexOf('/') + 1;
        if (idx <= 0 || idx >= serverPath.length()) return serverPath;
        return serverPath.substring(idx);
    }
}
