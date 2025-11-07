# Game Đoán Âm Thanh - Ứng dụng Client-Server

Ứng dụng game đoán âm thanh nhiều người chơi sử dụng kiến trúc Client-Server với Java Sockets.

## Yêu Cầu Hệ Thống

- Java 17 hoặc cao hơn
- Maven 3.6+
- MySQL 8.0+ (hoặc MariaDB)

## Cài Đặt và Cấu Hình

### 1. Cấu hình Database

Đảm bảo MySQL đã được cài đặt và đang chạy. Tạo database (hoặc để Spring Boot tự động tạo):

```sql
CREATE DATABASE IF NOT EXISTS game_sound_guess CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Cập nhật thông tin kết nối trong `server/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/game_sound_guess?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_password
```

### 2. Compile và Build

#### Server:
```bash
cd server
mvn clean install
```

#### Client:
```bash
cd client
mvn clean install
```

## Chạy Ứng Dụng

### 1. Khởi động Server

```bash
cd server
mvn spring-boot:run
```

Hoặc chạy trực tiếp từ IDE:
- Main class: `com.promex04.ServerApplication`
- Server sẽ chạy trên port `8888` (có thể thay đổi trong `application.properties`)

Server sẽ:
- Tự động tạo các bảng trong database (nếu chưa có)
- Khởi động Socket Server trên port 8888
- Sẵn sàng nhận kết nối từ clients

### 2. Khởi động Client

```bash
cd client
mvn javafx:run
```

Hoặc chạy từ IDE:
- Main class: `com.promex04.ClientApplication`
- Client sẽ kết nối đến `localhost:8888`

**Lưu ý**: Bạn có thể chạy nhiều instance của Client để test với nhiều người chơi.

## Tính Năng

### Đăng Nhập
- Người chơi đăng nhập bằng tên đăng nhập và mật khẩu
- Nếu tài khoản chưa tồn tại, hệ thống sẽ tự động tạo tài khoản mới

### Sảnh Chờ (Lobby)
- Hiển thị danh sách tất cả người chơi đang online
- Thông tin hiển thị: Tên, Tổng điểm, Trạng thái (rỗi/bận)
- Chatbox để nhắn tin với các người chơi khác
- Bảng xếp hạng (Ranking) hiển thị top players

### Thách Đấu
1. Chọn một người chơi trong danh sách (phải ở trạng thái "rỗi")
2. Click nút "Thách đấu"
3. Đối thủ sẽ nhận được yêu cầu thách đấu
4. Đối thủ có thể chấp nhận hoặc từ chối
5. Nếu chấp nhận, game sẽ bắt đầu

### Game Play
- Mỗi trận đấu gồm **10 ván (rounds)**
- Mỗi ván:
  - Server gửi một âm thanh và 4 lựa chọn
  - Người chơi có **15 giây** để chọn đáp án
  - Tính điểm dựa trên tốc độ:
    - 0-5 giây: **100 điểm**
    - 5-10 giây: **50 điểm**
    - 10-15 giây: **20 điểm**
    - Sai hoặc hết giờ: **0 điểm**
- Sau mỗi ván, hiển thị điểm của cả 2 người chơi
- Sau 10 ván, người có điểm cao hơn thắng

### Bảng Xếp Hạng
- Xếp hạng dựa trên:
  1. Tổng điểm (giảm dần)
  2. Số câu đúng (giảm dần)
  3. Số trận thắng (giảm dần)

## Giao Thức Giao Tiếp

Ứng dụng sử dụng text-based protocol qua Socket:

**Format**: `COMMAND:payload1:payload2:...\n`

### Commands từ Client:

- `LOGIN:username:password`
- `CHAT_LOBBY:message`
- `CHALLENGE:opponent_username`
- `CHALLENGE_RESPONSE:accept|reject`
- `GAME_SUBMIT:answer_index` (1, 2, 3, hoặc 4)
- `GET_RANKING`

### Commands từ Server:

- `LOGIN_SUCCESS:username:total_score`
- `LOGIN_FAILED`
- `LOBBY_UPDATE:user1,score1,status1;user2,score2,status2;...`
- `MSG_LOBBY:username:message`
- `CHALLENGE_REQUEST:challenger_username`
- `CHALLENGE_ACCEPTED:opponent_username`
- `CHALLENGE_REJECTED:opponent_username`
- `ROUND_START:audio_id:round_number:option1:option2:option3:option4:audio_path`
- `ROUND_RESULT:correct|wrong:points:time_ms`
- `SCORE_UPDATE:player1_score:player2_score`
- `GAME_OVER:player1_score:player2_score:winner_username|draw`
- `RANKING:rank1,username1,score1,correct1,won1;rank2,username2,...`
- `ERROR:error_message`

## Cấu Trúc Dự Án

```
LTM/
├── server/                 # Server application (Spring Boot)
│   ├── src/main/java/com/promex04/
│   │   ├── ServerApplication.java
│   │   ├── model/          # JPA Entities
│   │   │   ├── User.java
│   │   │   ├── Game.java
│   │   │   ├── GameRound.java
│   │   │   └── Audio.java
│   │   ├── repository/     # Spring Data JPA Repositories
│   │   ├── service/        # Business Logic
│   │   │   ├── UserService.java
│   │   │   ├── GameService.java
│   │   │   ├── ClientManager.java
│   │   │   ├── SocketServerService.java
│   │   │   └── ClientHandler.java
│   │   └── resources/
│   │       └── application.properties
│   └── pom.xml
│
└── client/                 # Client application (JavaFX)
    ├── src/main/java/com/promex04/
    │   ├── ClientApplication.java
    │   ├── model/          # Client-side models
    │   ├── controller/    # GameController (socket communication)
    │   │   └── GameController.java
    │   └── view/           # JavaFX Views
    │       ├── LoginView.java
    │       ├── LobbyView.java
    │       └── GameView.java
    └── pom.xml
```

## Lưu Ý Quan Trọng

1. **Audio Files**: Hiện tại ứng dụng chưa có tích hợp phát audio thực tế. Bạn cần thêm MediaPlayer để phát file audio từ `audio_path` trong database.

2. **Database**: Ứng dụng sẽ tự động tạo các bảng khi chạy lần đầu. Để thêm audio samples, bạn cần insert vào bảng `audios`:
   ```sql
   INSERT INTO audios (name, file_path, option1, option2, option3, option4, correct_answer)
   VALUES ('Audio Sample 1', '/path/to/audio.mp3', 'Lựa chọn 1', 'Lựa chọn 2', 'Lựa chọn 3', 'Lựa chọn 4', 1);
   ```

3. **Testing**: Để test ứng dụng với nhiều người chơi, bạn có thể:
   - Chạy nhiều instance của ClientApplication
   - Hoặc chạy trên các máy khác nhau trong cùng mạng (thay đổi `localhost` trong `connect()`)

## Troubleshooting

### Server không khởi động được
- Kiểm tra MySQL đã chạy chưa
- Kiểm tra thông tin kết nối database trong `application.properties`
- Kiểm tra port 8888 có bị chiếm dụng không

### Client không kết nối được
- Đảm bảo Server đã chạy
- Kiểm tra firewall không chặn port 8888
- Kiểm tra địa chỉ server trong `GameController.connect()`

### Lỗi JavaFX
- Đảm bảo JavaFX SDK đã được cấu hình đúng trong pom.xml
- Với Java 11+, cần module path riêng cho JavaFX

## Phát Triển Thêm

Một số tính năng có thể thêm vào:
- Phát audio thực tế bằng MediaPlayer
- Thêm nhiều loại âm thanh vào database
- Thống kê chi tiết cho từng người chơi
- Replay game
- Tournament mode
- Sound effects và background music

## License

Dự án này được tạo cho mục đích học tập và tham khảo.

