package com.promex04.model;

import lombok.Getter;

@Getter
public class GameRound {
    private Long audioId;
    private int roundNumber;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private String audioFilePath;
    private long startTime;

    public GameRound(Long audioId, int roundNumber, String option1, String option2,
                     String option3, String option4, String audioFilePath) {
        this.audioId = audioId;
        this.roundNumber = roundNumber;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.audioFilePath = audioFilePath;
        this.startTime = System.currentTimeMillis();
    }

    public long getElapsedTimeMs() {
        return System.currentTimeMillis() - startTime;
    }
}
