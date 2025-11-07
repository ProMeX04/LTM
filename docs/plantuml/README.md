# Tài liệu Sơ đồ UML - Game Đoán Âm Thanh

Thư mục này chứa các sơ đồ UML được tạo bằng PlantUML để mô tả kiến trúc và luồng hoạt động của ứng dụng Game Đoán Âm Thanh.

## Cấu trúc File

### Use Case Diagrams

1. **usecase_server.puml** - Sơ đồ use case cho module Server

    - Authentication Module: Đăng nhập, đăng ký, xác thực
    - User Management Module: Quản lý người dùng, cập nhật thống kê, bảng xếp hạng
    - Game Management Module: Tạo game, xử lý thách đấu, tạo round, tính điểm
    - Connection Management Module: Quản lý kết nối, broadcast, chat, gửi file

2. **usecase_client.puml** - Sơ đồ use case cho module Client
    - Authentication Module: Đăng nhập, đăng ký, đăng xuất
    - Lobby Module: Xem danh sách người chơi, chat, thách đấu, xem ranking
    - Game Play Module: Chơi game, nghe audio, chọn đáp án, xem kết quả

### Class Diagrams

3. **class_server.puml** - Sơ đồ class cho Server

    - **Model**: User, Game, GameRound, Audio
    - **Repository**: UserRepository, GameRepository, GameRoundRepository, AudioRepository
    - **Service**: UserService, GameService, ClientHandler, ClientManager, SocketServerService, BinaryTransferService

4. **class_client.puml** - Sơ đồ class cho Client
    - **Model**: User, GameRound, RankingEntry
    - **Controller**: GameController, BinaryTransferClient
    - **View**: ClientApplication, LoginView, LobbyView, GameView, GameOverView, LoadingView
    - **Audio**: AudioPlayer

### Sequence Diagrams

5. **sequence_login.puml** - Luồng đăng nhập

    - Người chơi nhập thông tin → LoginView → GameController → Server
    - Server xác thực → UserService → UserRepository
    - Kết quả trả về và cập nhật UI

6. **sequence_challenge.puml** - Luồng thách đấu và bắt đầu game

    - Người chơi 1 gửi thách đấu với chủ đề
    - Người chơi 2 nhận và chấp nhận/từ chối
    - Tạo game, prefetch audio, bắt đầu round đầu tiên

7. **sequence_gameplay.puml** - Luồng chơi game (submit answer)

    - Người chơi chọn đáp án
    - Server xử lý và tính điểm
    - Cập nhật kết quả và chuyển round tiếp theo hoặc kết thúc game

8. **sequence_chat.puml** - Luồng chat trong lobby

    - Người chơi gửi tin nhắn
    - Server broadcast đến tất cả clients trong lobby
    - Cập nhật UI cho tất cả người chơi

9. **sequence_ranking.puml** - Luồng lấy bảng xếp hạng
    - Client yêu cầu hoặc server tự động broadcast
    - Server lấy dữ liệu từ database
    - Trả về và hiển thị bảng xếp hạng

### Component Diagram

10. **component_diagram.puml** - Sơ đồ component kiến trúc tổng thể
    -   Mô tả các component chính: Client UI, Controller, Server, Business Logic, Data Access
    -   Hiển thị cách các component tương tác với nhau
    -   Mô tả giao thức giao tiếp giữa Client và Server

### Deployment Diagram

11. **deployment_diagram.puml** - Sơ đồ triển khai hệ thống
    -   Mô tả cách triển khai ứng dụng trên các máy khác nhau
    -   Client machines với JavaFX Runtime
    -   Server machine với Spring Boot và MySQL Database
    -   Kết nối mạng giữa các thành phần

### State Diagrams

12. **state_game.puml** - State machine cho Game entity

    -   Các trạng thái: WAITING, IN_PROGRESS, FINISHED
    -   Chuyển đổi trạng thái giữa các round
    -   Kết thúc game và cập nhật thống kê

13. **state_client.puml** - State machine cho Client Application
    -   Các trạng thái: DISCONNECTED, CONNECTING, LOGGED_OUT, IN_LOBBY, IN_GAME, GAME_OVER
    -   Luồng chuyển đổi giữa các màn hình
    -   Quản lý trạng thái kết nối và game

### Activity Diagram

14. **activity_gameplay.puml** - Luồng hoạt động chi tiết của gameplay
    -   Quy trình từ khi chấp nhận thách đấu đến kết thúc game
    -   Prefetch audio files
    -   Vòng lặp các round với tính điểm
    -   Xác định người thắng và cập nhật thống kê

### Package Diagram

15. **package_diagram.puml** - Sơ đồ package và dependencies
    -   Cấu trúc package của Client và Server
    -   Dependencies giữa các package
    -   Mối quan hệ giữa các module

### Database Diagrams

16. **database_diagram.puml** - Sơ đồ database schema chi tiết

    -   Mô tả đầy đủ các bảng: users, games, game_rounds, audios
    -   Hiển thị tất cả các cột với kiểu dữ liệu và constraints
    -   Foreign keys và relationships giữa các bảng
    -   Notes mô tả từng bảng

17. **database_erd.puml** - Entity Relationship Diagram
    -   ERD chuẩn với các entity và relationships
    -   Cardinality (1-to-many, many-to-one)
    -   Primary keys và Foreign keys
    -   Indexes và constraints quan trọng

## Cách Sử Dụng

### Xem sơ đồ trực tuyến

1. Truy cập [PlantUML Online Server](http://www.plantuml.com/plantuml/uml/)
2. Copy nội dung file `.puml` vào editor
3. Xem sơ đồ được render tự động

### Render thành hình ảnh

#### Sử dụng PlantUML CLI

```bash
# Cài đặt PlantUML (cần Java)
# macOS
brew install plantuml

# Hoặc download từ http://plantuml.com/download

# Render tất cả file
cd docs/plantuml
plantuml *.puml

# Render file cụ thể
plantuml usecase_server.puml
```

#### Sử dụng VS Code Extension

1. Cài đặt extension "PlantUML" trong VS Code
2. Mở file `.puml`
3. Nhấn `Alt+D` (Windows/Linux) hoặc `Option+D` (Mac) để preview
4. Export thành PNG/SVG bằng command palette

#### Sử dụng IntelliJ IDEA

1. Cài đặt plugin "PlantUML integration"
2. Mở file `.puml`
3. Nhấn `Ctrl+Alt+U` (Windows/Linux) hoặc `Cmd+Option+U` (Mac) để preview
4. Export bằng menu chuột phải

### Tích hợp vào tài liệu

Các file `.puml` có thể được tích hợp vào:

-   Markdown (với extension hỗ trợ PlantUML)
-   Confluence (với PlantUML macro)
-   GitLab/GitHub (với plugin hỗ trợ)
-   Documentation sites (MkDocs, Sphinx, etc.)

## Tổng Quan Các Loại Sơ Đồ

### Structural Diagrams (Sơ đồ cấu trúc)

-   **Use Case Diagrams**: Mô tả các chức năng từ góc nhìn người dùng
-   **Class Diagrams**: Mô tả cấu trúc các class và mối quan hệ
-   **Component Diagram**: Mô tả kiến trúc component và cách chúng tương tác
-   **Package Diagram**: Mô tả cấu trúc package và dependencies
-   **Deployment Diagram**: Mô tả cách triển khai hệ thống

### Behavioral Diagrams (Sơ đồ hành vi)

-   **Sequence Diagrams**: Mô tả luồng tương tác theo thời gian
-   **State Diagrams**: Mô tả các trạng thái và chuyển đổi trạng thái
-   **Activity Diagram**: Mô tả luồng hoạt động chi tiết

## Lưu Ý

-   Tất cả các sơ đồ sử dụng theme `plain` với màu sắc nhất quán
-   Các relationship được mô tả rõ ràng với mũi tên và nhãn
-   Sequence diagrams bao gồm các luồng chính và luồng phụ (alt/else)
-   Class diagrams hiển thị đầy đủ các thuộc tính và phương thức quan trọng
-   State diagrams mô tả đầy đủ các trạng thái và điều kiện chuyển đổi
-   Component và Deployment diagrams giúp hiểu rõ kiến trúc hệ thống

## Cập Nhật

Khi có thay đổi trong code, vui lòng cập nhật các sơ đồ tương ứng để đảm bảo tính nhất quán giữa code và tài liệu.
