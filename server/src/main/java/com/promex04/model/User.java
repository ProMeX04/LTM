package com.promex04.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;

    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers = 0;

    @Column(name = "games_won", nullable = false)
    private Integer gamesWon = 0;

    @Column(name = "games_played", nullable = false)
    private Integer gamesPlayed = 0;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.totalScore = 0;
        this.correctAnswers = 0;
        this.gamesWon = 0;
        this.gamesPlayed = 0;
    }
}

