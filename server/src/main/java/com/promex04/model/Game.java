package com.promex04.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "games")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player1_id", nullable = false)
    private User player1;

    @ManyToOne
    @JoinColumn(name = "player2_id", nullable = false)
    private User player2;

    @Column(name = "player1_score", nullable = false)
    private Integer player1Score = 0;

    @Column(name = "player2_score", nullable = false)
    private Integer player2Score = 0;

    @Column(name = "current_round", nullable = false)
    private Integer currentRound = 1;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private GameStatus status = GameStatus.WAITING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    @Column(name = "preferred_artist")
    private String preferredArtist; // Ca sĩ đã chọn khi thách đấu (null = bất kỳ)

    @Column(name = "preferred_genre")
    private String preferredGenre; // Thể loại đã chọn khi thách đấu (null = bất kỳ)

    public enum GameStatus {
        WAITING, // Đang chờ cả 2 người chơi sẵn sàng
        IN_PROGRESS, // Đang chơi
        FINISHED // Đã kết thúc
    }
}
