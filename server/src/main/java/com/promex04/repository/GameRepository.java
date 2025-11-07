package com.promex04.repository;

import com.promex04.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByPlayer1UsernameOrPlayer2Username(String player1, String player2);
}
