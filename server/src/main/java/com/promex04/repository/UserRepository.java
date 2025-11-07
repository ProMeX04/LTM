package com.promex04.repository;

import com.promex04.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    @Query("SELECT u FROM User u ORDER BY u.totalScore DESC, u.correctAnswers DESC, u.gamesWon DESC")
    List<User> findAllOrderByRanking();
}

