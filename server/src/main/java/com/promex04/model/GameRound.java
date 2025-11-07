package com.promex04.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_rounds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne
    @JoinColumn(name = "audio_id", nullable = false)
    private Audio audio;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "player1_answer")
    private Integer player1Answer;

    @Column(name = "player2_answer")
    private Integer player2Answer;

    @Column(name = "player1_time_ms")
    private Long player1TimeMs;

    @Column(name = "player2_time_ms")
    private Long player2TimeMs;

    @Column(name = "player1_points", nullable = false)
    private Integer player1Points = 0;

    @Column(name = "player2_points", nullable = false)
    private Integer player2Points = 0;

    // Lưu phương án lựa chọn cho round này (được sinh ngẫu nhiên từ DB)
    @Column(name = "option1")
    private String option1;

    @Column(name = "option2")
    private String option2;

    @Column(name = "option3")
    private String option3;

    @Column(name = "option4")
    private String option4;

    // Chỉ số đáp án đúng tương ứng 1..4
    @Column(name = "correct_answer")
    private Integer correctAnswer;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
