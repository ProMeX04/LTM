# BÁO CÁO BÀI TẬP LỚN

**ĐỀ TÀI:** Ứng dụng Game Đoán Âm Thanh Nhiều Người Chơi

**MODULE 2:** Lobby & Social Features (Chat + Challenge System)

---

## TRANG BÌA

**Họ và Tên:** [Họ tên sinh viên]

**MSSV:** [Mã số sinh viên]

**Nhóm Lớp:** [N5/N6/N9/N10/N11/N12]

**Số Nhóm BTL:** [Số nhóm]

**Thành viên nhóm:**

1. [Họ tên] - [MSSV] - Module 1: Authentication & User Management
2. [Họ tên] - [MSSV] - Module 2: Lobby & Social Features (Chat + Challenge)
3. [Họ tên] - [MSSV] - Module 3: Game Play & Scoring
4. [Họ tên] - [MSSV] - Module 4: Audio Management & Transfer

**Tên đầy đủ đề tài:** Xây dựng ứng dụng Game Đoán Âm Thanh Nhiều Người Chơi sử dụng kiến trúc Client-Server với Java Sockets và JavaFX

---

## MỤC LỤC

1. [MỞ ĐẦU](#1-mở-đầu)
   1.1. [Giới thiệu ứng dụng](#11-giới-thiệu-ứng-dụng)
   1.2. [Phân tích yêu cầu ứng dụng/hệ thống](#12-phân-tích-yêu-cầu-ứng-dụnghệ-thống)
   1.3. [Phân công chức năng](#13-phân-công-chức-năng)

2. [PHÂN TÍCH THIẾT KẾ TỔNG QUAN](#2-phân-tích-thiết-kế-tổng-quan)
   2.1. [Kiến trúc tổng quan](#21-kiến-trúc-tổng-quan)
   2.2. [Sơ đồ khối các chức năng của Client và Server](#22-sơ-đồ-khối-các-chức-năng-của-client-và-server)
   2.3. [Biểu đồ Usecase tổng quan của hệ thống](#23-biểu-đồ-usecase-tổng-quan-của-hệ-thống)

3. [PHÂN TÍCH THIẾT KẾ CHI TIẾT MODULE 2](#3-phân-tích-thiết-kế-chi-tiết-module-2)
   3.1. [Giới thiệu Module 2](#31-giới-thiệu-module-2)
   3.2. [Usecase chi tiết Module 2](#32-usecase-chi-tiết-module-2)
   3.3. [Biểu đồ lớp Module 2](#33-biểu-đồ-lớp-module-2)
   3.4. [Biểu đồ tuần tự](#34-biểu-đồ-tuần-tự)
   3.5. [Sơ đồ thực thể quan hệ (ER)](#35-sơ-đồ-thực-thể-quan-hệ-er)

4. [KẾT QUẢ VÀ TRIỂN KHAI](#4-kết-quả-và-triển-khai)
   4.1. [Kiến trúc ứng dụng](#41-kiến-trúc-ứng-dụng)
   4.2. [Cài đặt và triển khai ứng dụng](#42-cài-đặt-và-triển-khai-ứng-dụng)
   4.3. [Các kết quả cá nhân thực hiện được](#43-các-kết-quả-cá-nhân-thực-hiện-được)
   4.4. [Kết quả thử nghiệm/triển khai](#44-kết-quả-thử-nghiệmtriển-khai)

5. [TÀI LIỆU THAM KHẢO](#5-tài-liệu-tham-khảo)

---

## DANH SÁCH TỪ VIẾT TẮT

| Từ viết tắt | Ý nghĩa                               |
| ----------- | ------------------------------------- |
| BTL         | Bài Tập Lớn                           |
| UI          | User Interface (Giao diện người dùng) |
| MVC         | Model-View-Controller                 |
| ERD         | Entity Relationship Diagram           |
| JPA         | Java Persistence API                  |
| JSON        | JavaScript Object Notation            |
| TCP         | Transmission Control Protocol         |
| HTTP        | Hypertext Transfer Protocol           |
| REST        | Representational State Transfer       |
| API         | Application Programming Interface     |

---

## DANH SÁCH HÌNH VẼ

| Hình     | Tên hình                           | Trang |
| -------- | ---------------------------------- | ----- |
| Hình 2.1 | Kiến trúc tổng quan hệ thống       | 8     |
| Hình 2.2 | Sơ đồ khối chức năng Client        | 9     |
| Hình 2.3 | Sơ đồ khối chức năng Server        | 10    |
| Hình 2.4 | Biểu đồ Usecase tổng quan          | 11    |
| Hình 3.1 | Usecase chi tiết Module 2          | 13    |
| Hình 3.2 | Biểu đồ lớp Module 2 (Server)      | 15    |
| Hình 3.3 | Biểu đồ lớp Module 2 (Client)      | 16    |
| Hình 3.4 | Biểu đồ tuần tự - Chat trong Lobby | 18    |
| Hình 3.5 | Biểu đồ tuần tự - Challenge System | 19    |
| Hình 3.6 | Sơ đồ ERD Module 2                 | 21    |
| Hình 4.1 | Kiến trúc triển khai ứng dụng      | 23    |
| Hình 4.2 | Giao diện Lobby với Chat           | 25    |
| Hình 4.3 | Giao diện Challenge Dialog         | 26    |

---

## DANH SÁCH BẢNG BIỂU

| Bảng     | Tên bảng                            | Trang |
| -------- | ----------------------------------- | ----- |
| Bảng 1.1 | Phân công chức năng theo module     | 5     |
| Bảng 3.1 | Các command liên quan đến Chat      | 14    |
| Bảng 3.2 | Các command liên quan đến Challenge | 14    |
| Bảng 4.1 | Kết quả test các tính năng Module 2 | 27    |

---

# 1. MỞ ĐẦU

## 1.1. Giới thiệu ứng dụng

Ứng dụng Game Đoán Âm Thanh Nhiều Người Chơi là hệ thống game trực tuyến cho phép người chơi thi đấu bằng cách đoán tên bài hát từ các đoạn âm thanh. Ứng dụng sử dụng kiến trúc Client-Server với Java Sockets, JavaFX (UI), và Spring Boot (Server).

**Tính năng chính:** Đăng nhập/Đăng ký, Sảnh chờ (Lobby) với chat và bảng xếp hạng, Thách đấu với tùy chọn ca sĩ/thể loại, Chơi game với nhiều ván, Quản lý audio files.

## 1.2. Phân tích yêu cầu ứng dụng/hệ thống

### 1.2.1. Yêu cầu chức năng

**Yêu cầu chung:** Hỗ trợ nhiều người chơi đồng thời, giao tiếp real-time, lưu trữ thông tin người chơi và lịch sử game, quản lý file audio hiệu quả.

**Yêu cầu Module 2 - Lobby & Social Features:**

-   Hiển thị danh sách người chơi online với trạng thái (rỗi/bận)
-   Chat trong lobby, tìm kiếm và lọc danh sách người chơi
-   Thách đấu người chơi khác với chủ đề tùy chọn (ca sĩ, thể loại, số lượng câu hỏi)
-   Chấp nhận/từ chối thách đấu
-   Cập nhật trạng thái lobby real-time

### 1.2.2. Yêu cầu phi chức năng

-   **Hiệu năng:** Độ trễ thấp (< 100ms), hỗ trợ 100+ người chơi đồng thời
-   **Độ tin cậy:** Đảm bảo tin nhắn chat và thách đấu được gửi/nhận đúng
-   **Bảo mật:** Xác thực người chơi, ngăn chặn spam chat
-   **Giao diện:** Thân thiện, dễ sử dụng, hỗ trợ dark mode

## 1.3. Phân công chức năng

**Bảng 1.1: Phân công chức năng theo module**

| Module                                     | Thành viên   | Chức năng chính                                                                                                 | Công nghệ                             |
| ------------------------------------------ | ------------ | --------------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| Module 1: Authentication & User Management | [Tên SV]     | - Đăng nhập/Đăng ký<br>- Quản lý thông tin người dùng<br>- Bảng xếp hạng                                        | Spring Boot, JPA, MySQL               |
| **Module 2: Lobby & Social Features**      | **[Tên SV]** | **- Hiển thị danh sách người chơi<br>- Chat trong lobby<br>- Hệ thống thách đấu<br>- Quản lý trạng thái lobby** | **JavaFX, Java Sockets, Spring Boot** |
| Module 3: Game Play & Scoring              | [Tên SV]     | - Logic game<br>- Tính điểm<br>- Quản lý rounds                                                                 | Spring Boot, JPA                      |
| Module 4: Audio Management & Transfer      | [Tên SV]     | - Quản lý audio files<br>- Binary transfer<br>- Audio player                                                    | JavaFX Media, Binary Sockets          |

**Nội dung cá nhân đảm nhận (Module 2):**

1. **Lobby Management:** Hiển thị danh sách người chơi online (tên, điểm, trạng thái), tìm kiếm/lọc, cập nhật real-time
2. **Chat System:** Gửi/nhận tin nhắn chat với bubble UI, tự động cuộn, giới hạn 200 ký tự
3. **Challenge System:** Gửi/nhận/chấp nhận/từ chối thách đấu với tùy chọn ca sĩ, thể loại, số lượng câu hỏi
4. **State Management:** Quản lý và đồng bộ trạng thái kết nối, xử lý người chơi rời đột ngột/mất kết nối

---

# 2. PHÂN TÍCH THIẾT KẾ TỔNG QUAN

## 2.1. Kiến trúc tổng quan

Hệ thống sử dụng kiến trúc Client-Server: **Server Side** (Spring Boot với Socket Server, MySQL Database, Binary Transfer Service), **Client Side** (JavaFX Application, Socket Client, Audio Player).

![Kiến trúc tổng quan hệ thống](images/hinh_2_1_kien_truc_tong_quan.png)

**Hình 2.1: Kiến trúc tổng quan hệ thống**

## 2.2. Sơ đồ khối các chức năng của Client và Server

### 2.2.1. Sơ đồ khối chức năng Client

![Sơ đồ khối chức năng Client](images/hinh_2_2_so_do_khoi_client.png)

**Hình 2.2: Sơ đồ khối chức năng Client**

### 2.2.2. Sơ đồ khối chức năng Server

![Sơ đồ khối chức năng Server](images/hinh_2_3_so_do_khoi_server.png)

**Hình 2.3: Sơ đồ khối chức năng Server**

## 2.3. Biểu đồ Usecase tổng quan của hệ thống

![Biểu đồ Usecase tổng quan của hệ thống](images/hinh_2_4_usecase_tong_quan.png)

**Hình 2.4: Biểu đồ Usecase tổng quan của hệ thống**

---

# 3. PHÂN TÍCH THIẾT KẾ CHI TIẾT MODULE 2

## 3.1. Giới thiệu Module 2

Module 2 - Lobby & Social Features quản lý sảnh chờ và các tính năng xã hội: **Lobby Management** (danh sách người chơi online, cập nhật real-time), **Chat System** (giao tiếp trong lobby), **Challenge System** (thách đấu người chơi khác). Module này đóng vai trò trung tâm trong việc kết nối người chơi.

## 3.2. Usecase chi tiết Module 2

![Usecase chi tiết Module 2](images/hinh_3_1_usecase_module2.png)

**Hình 3.1: Usecase chi tiết Module 2**

### Mô tả các Usecase:

**Lobby Management:**

-   UC2.1: Hiển thị danh sách người chơi online (tên, điểm, trạng thái), tự động cập nhật real-time
-   UC2.2: Tìm kiếm người chơi theo tên hoặc điểm số
-   UC2.3: Lọc danh sách theo trạng thái và điểm số
-   UC2.4: Cập nhật trạng thái lobby khi có người vào/ra hoặc thay đổi trạng thái

**Chat System:**

-   UC2.5: Gửi tin nhắn chat đến tất cả người chơi (giới hạn 200 ký tự)
-   UC2.6: Nhận và hiển thị tin nhắn với bubble UI, phân biệt tin nhắn của mình/người khác
-   UC2.7: Hiển thị lịch sử chat trong phiên lobby hiện tại

**Challenge System:**

-   UC2.8: Gửi thách đấu với chủ đề (ca sĩ, thể loại, số lượng câu hỏi) đến người chơi rỗi
-   UC2.9: Nhận và hiển thị yêu cầu thách đấu với đầy đủ thông tin
-   UC2.10: Chấp nhận thách đấu → tạo game, cả hai chuyển sang trạng thái "bận"
-   UC2.11: Từ chối thách đấu → thông báo đến người gửi
-   UC2.12: Chọn chủ đề thách đấu (ca sĩ, thể loại, số lượng câu hỏi 5-50)

## 3.3. Biểu đồ lớp Module 2

### 3.3.1. Server Side

![Biểu đồ lớp Module 2 (Server)](images/hinh_3_2_class_diagram_server.png)

**Hình 3.2: Biểu đồ lớp Module 2 (Server)**

### 3.3.2. Client Side

![Biểu đồ lớp Module 2 (Client)](images/hinh_3_3_class_diagram_client.png)

**Hình 3.3: Biểu đồ lớp Module 2 (Client)**

### 3.3.3. Protocol Commands

**Bảng 3.1: Các command liên quan đến Chat**

| Command    | Hướng           | Format                       | Mô tả              |
| ---------- | --------------- | ---------------------------- | ------------------ |
| CHAT_LOBBY | Client → Server | `CHAT_LOBBY:message`         | Gửi tin nhắn chat  |
| MSG_LOBBY  | Server → Client | `MSG_LOBBY:username:message` | Nhận tin nhắn chat |

**Bảng 3.2: Các command liên quan đến Challenge**

| Command            | Hướng           | Format                                                       | Mô tả                     |
| ------------------ | --------------- | ------------------------------------------------------------ | ------------------------- | ----- | --- | ------------ | ------------- |
| CHALLENGE          | Client → Server | `CHALLENGE:opponentUsername:artist                           |                           | genre |     | totalRounds` | Gửi thách đấu |
| CHALLENGE_REQUEST  | Server → Client | `CHALLENGE_REQUEST:challengerUsername:payload`               | Nhận yêu cầu thách đấu    |
| CHALLENGE_RESPONSE | Client → Server | `CHALLENGE_RESPONSE:accept\|reject`                          | Phản hồi thách đấu        |
| CHALLENGE_ACCEPTED | Server → Client | `CHALLENGE_ACCEPTED:opponentUsername`                        | Thách đấu được chấp nhận  |
| CHALLENGE_REJECTED | Server → Client | `CHALLENGE_REJECTED:opponentUsername`                        | Thách đấu bị từ chối      |
| CHALLENGE_SENT     | Server → Client | `CHALLENGE_SENT:opponentUsername:payload`                    | Xác nhận đã gửi thách đấu |
| CHALLENGE_FAILED   | Server → Client | `CHALLENGE_FAILED:errorMessage`                              | Lỗi khi gửi thách đấu     |
| LOBBY_UPDATE       | Server → Client | `LOBBY_UPDATE:user1,score1,status1;user2,score2,status2;...` | Cập nhật danh sách lobby  |

## 3.4. Biểu đồ tuần tự

### 3.4.1. Biểu đồ tuần tự - Chat trong Lobby

![Biểu đồ tuần tự - Chat trong Lobby](images/hinh_3_4_sequence_chat.png)

**Hình 3.4: Biểu đồ tuần tự - Chat trong Lobby**

### 3.4.2. Biểu đồ tuần tự - Challenge System

![Biểu đồ tuần tự - Challenge System](images/hinh_3_5_sequence_challenge.png)

**Hình 3.5: Biểu đồ tuần tự - Challenge System**

## 3.5. Sơ đồ thực thể quan hệ (ER)

Module 2 chủ yếu làm việc với entity `User` và không tạo thêm bảng mới. Tuy nhiên, Module 2 sử dụng thông tin từ các bảng khác:

![Sơ đồ ERD Module 2](images/hinh_3_6_erd_module2.png)

**Hình 3.6: Sơ đồ ERD Module 2**

---

# 4. KẾT QUẢ VÀ TRIỂN KHAI

## 4.1. Kiến trúc ứng dụng

**Thành phần Client:**

-   LobbyView UI: Giao diện lobby với danh sách người chơi, chat box và các nút điều khiển
-   GameController: Logic nghiệp vụ, xử lý sự kiện UI và giao tiếp với server
-   Socket Client: Kết nối TCP Socket với server để gửi/nhận messages

**Thành phần Server:**

-   Socket Server: Lắng nghe kết nối từ client trên port 8888
-   ClientHandler: Xử lý giao tiếp với từng client, parse và xử lý commands
-   ClientManager: Quản lý tất cả clients, thực hiện broadcast messages
-   UserService và GameService: Xử lý logic nghiệp vụ người dùng và game

**Database:** MySQL lưu trữ thông tin người dùng (Users table) và lịch sử game (Games table)

**Luồng hoạt động:** Client kết nối → Server tạo ClientHandler → ClientManager quản lý → Broadcast khi có thay đổi → Client cập nhật UI

Kiến trúc triển khai chi tiết:

![Kiến trúc triển khai ứng dụng](images/hinh_4_1_kien_truc_trien_khai.png)

**Hình 4.1: Kiến trúc triển khai ứng dụng**

## 4.2. Cài đặt và triển khai ứng dụng

**Yêu cầu hệ thống:** JDK 17+, Maven 3.6+, MySQL 8.0+, Windows/Linux/macOS

**Cài đặt Server:**

1. Cấu hình database trong `server/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/game_sound_guess
spring.datasource.username=root
spring.datasource.password=your_password
server.socket.port=8888
binary.transfer.port=7777
```

2. Build và chạy: `cd server && mvn clean install && mvn spring-boot:run`

**Cài đặt Client:**

1. Build: `cd client && mvn clean install`
2. Chạy: `mvn javafx:run` hoặc từ IDE với main class `com.promex04.ClientApplication`

**Khởi động hệ thống:**

1. Khởi động MySQL và tạo database `game_sound_guess`
2. Khởi động Server Application
3. Khởi động Client Application (có thể nhiều instance để test)

**Kiểm tra:** Server log "Socket Server started on port 8888", client kết nối sau khi đăng nhập, lobby update khi có client mới vào.

## 4.3. Các kết quả cá nhân thực hiện được

**Lobby Management:**

-   Hiển thị danh sách người chơi online (tên, điểm, trạng thái) với UI dark theme
-   Tìm kiếm và lọc người chơi theo tên/điểm số
-   Cập nhật real-time khi có người vào/ra hoặc thay đổi trạng thái
-   Avatar màu sắc dựa trên username, phân biệt trạng thái "rỗi"/"bận"

**Chat System:**

-   Gửi/nhận tin nhắn chat trong lobby (giới hạn 200 ký tự)
-   Bubble UI phân biệt tin nhắn của mình (xanh, phải) và người khác (xám, trái)
-   Tự động cuộn xuống tin nhắn mới, hỗ trợ gửi bằng Enter hoặc nút Send

**Challenge System:**

-   Gửi thách đấu với chủ đề (ca sĩ, thể loại, số lượng câu hỏi 5-50)
-   Nhận/chấp nhận/từ chối thách đấu với dialog hiển thị đầy đủ thông tin
-   Xử lý lỗi khi người chơi không sẵn sàng hoặc đã rời game
-   Cập nhật trạng thái lobby khi challenge được chấp nhận

**State Management:**

-   Quản lý và đồng bộ trạng thái kết nối giữa các client
-   Xử lý người chơi rời đột ngột và mất kết nối với heartbeat mechanism

**Code Structure:**

-   Server: `ClientManager.java` (quản lý clients, broadcast), `ClientHandler.java` (xử lý chat/challenge)
-   Client: `LobbyView.java` (UI), `GameController.java` (logic giao tiếp server)
-   Protocol: Text-based qua TCP Socket, format `COMMAND:payload`, delimiter `||` cho challenge payload

## 4.4. Kết quả thử nghiệm/triển khai

**Bảng 4.1: Kết quả test các tính năng Module 2**

| STT | Test Case                        | Kết quả | Ghi chú                                      |
| --- | -------------------------------- | ------- | -------------------------------------------- |
| 1   | Gửi/nhận tin nhắn chat           | PASS    | Tin nhắn hiển thị đúng cho tất cả người chơi |
| 2   | Tự động cuộn chat                | PASS    | Chat tự động cuộn xuống tin nhắn mới         |
| 3   | Giới hạn độ dài tin nhắn         | PASS    | Không thể nhập quá 200 ký tự                 |
| 4   | Gửi/nhận thách đấu               | PASS    | Thách đấu được gửi/nhận đúng người chơi      |
| 5   | Chấp nhận/từ chối thách đấu      | PASS    | Game được tạo hoặc thông báo từ chối đúng    |
| 6   | Tìm kiếm người chơi              | PASS    | Tìm kiếm hoạt động đúng với tên và điểm số   |
| 7   | Cập nhật lobby real-time         | PASS    | Lobby cập nhật ngay khi có thay đổi          |
| 8   | Xử lý người chơi rời/mất kết nối | PASS    | Heartbeat phát hiện và xử lý đúng            |

**Performance Testing:**

-   Latency: < 50ms cho chat message
-   Throughput: 100+ messages/giây
-   Concurrent Users: Đã test với 50 người chơi đồng thời, hoạt động ổn định

**Issues và Solutions:**

-   Tin nhắn chứa ký tự `:` → Sử dụng `split(":", 3)` và ghép phần còn lại
-   Challenge payload chứa ký tự đặc biệt → Sử dụng delimiter `||` thay vì `:`
-   Lobby update không đồng bộ → Broadcast đến tất cả clients mỗi khi có thay đổi

### 4.4.2. Minh họa giao diện ứng dụng

**Hình 4.2: Giao diện Lobby với Chat**

![Giao diện Lobby với Chat](images/hinh_4_2_lobby_chat.png)

Giao diện Lobby hiển thị:

-   Danh sách người chơi online bên trái với avatar màu sắc, điểm số và trạng thái (rỗi/bận)
-   Chat box ở giữa với bubble UI phân biệt tin nhắn của mình (xanh, phải) và người khác (xám, trái)
-   Ô tìm kiếm người chơi ở đầu danh sách
-   Nút "Thách đấu" để gửi thách đấu đến người chơi khác

**Hình 4.3: Giao diện Challenge Dialog**

![Giao diện Challenge Dialog](images/hinh_4_3_challenge_dialog.png)

Dialog thách đấu cho phép:

-   Chọn ca sĩ từ dropdown với tìm kiếm
-   Chọn thể loại nhạc (pop, rock, ballad, v.v.)
-   Chọn số lượng câu hỏi từ 5 đến 50
-   Hiển thị thông tin chi tiết khi nhận được yêu cầu thách đấu với nút "Chấp nhận" và "Từ chối"

---

# 5. TÀI LIỆU THAM KHẢO

1. Oracle Corporation. (2023). _Java Platform, Standard Edition Documentation_. https://docs.oracle.com/javase/

2. Oracle Corporation. (2023). _JavaFX Documentation_. https://openjfx.io/

3. Spring Framework. (2023). _Spring Boot Reference Documentation_. https://spring.io/projects/spring-boot

4. MySQL. (2023). _MySQL 8.0 Reference Manual_. https://dev.mysql.com/doc/

5. PlantUML. (2023). _PlantUML Language Reference Guide_. https://plantuml.com/

6. Fowler, M. (2002). _Patterns of Enterprise Application Architecture_. Addison-Wesley Professional.

7. Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). _Design Patterns: Elements of Reusable Object-Oriented Software_. Addison-Wesley Professional.

8. Oracle Corporation. (2023). _Java Socket Programming_. https://docs.oracle.com/javase/tutorial/networking/sockets/

9. GitHub. (2023). _GitHub Dark Theme CSS_. https://github.com/primer/github-syntax-theme-generator

10. Maven. (2023). _Apache Maven Documentation_. https://maven.apache.org/guides/

---

**HẾT BÁO CÁO**
