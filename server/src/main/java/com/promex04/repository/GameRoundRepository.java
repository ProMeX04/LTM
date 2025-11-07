package com.promex04.repository;

import com.promex04.model.GameRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRoundRepository extends JpaRepository<GameRound, Long> {
    List<GameRound> findByGameId(Long gameId);
    
    Optional<GameRound> findByGameIdAndRoundNumber(Long gameId, Integer roundNumber);
}

