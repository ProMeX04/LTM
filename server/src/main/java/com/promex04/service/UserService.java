package com.promex04.service;

import com.promex04.model.User;
import com.promex04.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> authenticate(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user;
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createUser(String username, String password) {
        User user = new User(username, password);
        return userRepository.save(user);
    }

    public List<User> getRanking() {
        return userRepository.findAllOrderByRanking();
    }

    @Transactional
    public void updateUserStats(User user, boolean won, int score, int correctAnswers) {
        user.setTotalScore(user.getTotalScore() + score);
        user.setCorrectAnswers(user.getCorrectAnswers() + correctAnswers);
        user.setGamesPlayed(user.getGamesPlayed() + 1);
        if (won) {
            user.setGamesWon(user.getGamesWon() + 1);
        }
        userRepository.save(user);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}

