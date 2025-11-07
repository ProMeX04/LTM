package com.promex04.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankingEntry {
    private int rank;
    private String username;
    private int totalScore;
    private int correctAnswers;
    private int gamesWon;
}
