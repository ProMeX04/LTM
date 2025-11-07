package com.promex04.service;

import com.promex04.model.*;
import com.promex04.repository.AudioRepository;
import com.promex04.repository.GameRepository;
import com.promex04.repository.GameRoundRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final GameRoundRepository gameRoundRepository;
    private final AudioRepository audioRepository;
    private final UserService userService;

    @Autowired
    public GameService(GameRepository gameRepository, GameRoundRepository gameRoundRepository,
            AudioRepository audioRepository, UserService userService) {
        this.gameRepository = gameRepository;
        this.gameRoundRepository = gameRoundRepository;
        this.audioRepository = audioRepository;
        this.userService = userService;
    }

    public Game createGame(User player1, User player2) {
        Game game = new Game();
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(Game.GameStatus.IN_PROGRESS);
        return gameRepository.save(game);
    }

    public Audio getRandomAudio() {
        List<Audio> allAudios = audioRepository.findAll();
        if (allAudios.isEmpty()) {
            // Tạo audio mẫu nếu chưa có
            return createSampleAudio();
        }
        Collections.shuffle(allAudios);
        return allAudios.get(0);
    }

    public List<Audio> getRandomAudios(int n) {
        return getRandomAudios(n, null, null);
    }

    public List<Audio> getRandomAudios(int n, String preferredArtist, String preferredGenre) {
        List<Audio> allAudios = new ArrayList<>(audioRepository.findAll());
        if (allAudios.isEmpty()) {
            // Đảm bảo có ít nhất 1 audio mẫu để không lỗi
            allAudios.add(createSampleAudio());
        }

        List<Audio> filtered = allAudios.stream()
                .filter(audio -> matchesPreference(audio.getArtist(), preferredArtist))
                .filter(audio -> matchesPreference(audio.getGenre(), preferredGenre))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            filtered = new ArrayList<>(allAudios);
        }

        Collections.shuffle(filtered);
        if (n <= 0 || n >= filtered.size()) {
            return filtered;
        }
        return filtered.subList(0, n);
    }

    private boolean matchesPreference(String value, String preferred) {
        if (preferred == null || preferred.isBlank()) {
            return true;
        }
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.trim().equalsIgnoreCase(preferred.trim());
    }

    private Audio createSampleAudio() {
        Audio audio = new Audio();
        audio.setName("Sample Audio");
        audio.setFilePath("/sample/audio.mp3");
        audio.setArtist("Unknown Artist");
        audio.setGenre("Sample");
        audio.setAlbum("Demo Collection");
        audio.setReleaseYear(LocalDateTime.now().getYear());
        audio.setDurationSeconds(30);
        return audioRepository.save(audio);
    }

    public List<String> getDistinctArtists() {
        Set<String> artists = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        audioRepository.findAll().forEach(audio -> {
            if (audio.getArtist() != null && !audio.getArtist().isBlank()) {
                artists.add(audio.getArtist().trim());
            }
        });
        return new ArrayList<>(artists);
    }

    public List<String> getDistinctGenres() {
        Set<String> genres = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        audioRepository.findAll().forEach(audio -> {
            if (audio.getGenre() != null && !audio.getGenre().isBlank()) {
                genres.add(audio.getGenre().trim());
            }
        });
        return new ArrayList<>(genres);
    }

    public GameRound createRound(Game game, int roundNumber) {
        Audio audio = getRandomAudio();
        return createRoundWithAudio(game, roundNumber, audio);
    }

    public GameRound createRoundWithAudio(Game game, int roundNumber, Audio audio) {
        GameRound round = new GameRound();
        round.setGame(game);
        round.setAudio(audio);
        round.setRoundNumber(roundNumber);
        round.setStartedAt(LocalDateTime.now());

        // Sinh 3 đáp án nhiễu + 1 đúng từ DB
        List<Audio> all = audioRepository.findAll();
        // loại bỏ bản đúng
        all.removeIf(a -> a.getId().equals(audio.getId()));
        Collections.shuffle(all);

        String correct = audio.getName();
        String o2 = all.size() >= 1 ? all.get(0).getName() : "Lựa chọn 2";
        String o3 = all.size() >= 2 ? all.get(1).getName() : "Lựa chọn 3";
        String o4 = all.size() >= 3 ? all.get(2).getName() : "Lựa chọn 4";

        // Trộn thứ tự và xác định index đúng
        java.util.List<String> options = new java.util.ArrayList<>();
        options.add(correct);
        options.add(o2);
        options.add(o3);
        options.add(o4);
        Collections.shuffle(options);
        int correctIndex = options.indexOf(correct) + 1; // 1..4

        round.setOption1(options.get(0));
        round.setOption2(options.get(1));
        round.setOption3(options.get(2));
        round.setOption4(options.get(3));
        round.setCorrectAnswer(correctIndex);

        return gameRoundRepository.save(round);
    }

    /**
     * Tìm hoặc tạo round cho game và roundNumber.
     * Đảm bảo cả hai người chơi dùng chung một round với cùng các option.
     * 
     * @Transactional để đảm bảo tính nhất quán khi kiểm tra và tạo round.
     */
    @Transactional
    public GameRound findOrCreateRoundWithAudio(Game game, int roundNumber, Audio audio) {
        // Kiểm tra xem đã có round cho roundNumber này chưa
        var existingRound = gameRoundRepository.findByGameIdAndRoundNumber(game.getId(), roundNumber);
        if (existingRound.isPresent()) {
            // Đã có round, kiểm tra xem audio có khớp không
            GameRound round = existingRound.get();
            if (round.getAudio().getId().equals(audio.getId())) {
                return round; // Dùng lại round đã có
            }
            // Nếu audio không khớp, tạo mới (trường hợp hiếm - có thể do lỗi logic)
        }
        // Chưa có round, tạo mới
        return createRoundWithAudio(game, roundNumber, audio);
    }

    @Transactional
    public RoundResult processRoundAnswer(GameRound roundRef, String playerUsername, int answerIndex, long timeMs) {
        if (roundRef == null || roundRef.getId() == null) {
            throw new IllegalArgumentException("Round reference is invalid");
        }

        GameRound round = gameRoundRepository.findById(roundRef.getId())
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundRef.getId()));

        // Đọc lại game từ database để đảm bảo có điểm mới nhất (tránh ghi đè điểm của
        // người chơi khác)
        Game game = gameRepository.findById(round.getGame().getId())
                .orElseThrow(() -> new IllegalStateException("Game not found: " + round.getGame().getId()));

        boolean isPlayer1 = game.getPlayer1().getUsername().equals(playerUsername);

        // Nếu người chơi đã trả lời round này rồi thì không cộng điểm thêm (tránh
        // double submit)
        if (isPlayer1 && round.getPlayer1Answer() != null) {
            return new RoundResult(isCorrect(round, round.getPlayer1Answer()), round.getPlayer1Points() != null
                    ? round.getPlayer1Points()
                    : 0,
                    round.getPlayer1TimeMs() != null ? round.getPlayer1TimeMs() : timeMs);
        }
        if (!isPlayer1 && round.getPlayer2Answer() != null) {
            return new RoundResult(isCorrect(round, round.getPlayer2Answer()), round.getPlayer2Points() != null
                    ? round.getPlayer2Points()
                    : 0,
                    round.getPlayer2TimeMs() != null ? round.getPlayer2TimeMs() : timeMs);
        }

        boolean isCorrect = isCorrect(round, answerIndex);
        int points = calculatePoints(isCorrect, timeMs);

        if (isPlayer1) {
            round.setPlayer1Answer(answerIndex);
            round.setPlayer1TimeMs(timeMs);
            round.setPlayer1Points(points);
            // Cộng điểm vào điểm hiện tại từ database (đảm bảo không ghi đè điểm của
            // player2)
            game.setPlayer1Score(safeScore(game.getPlayer1Score()) + points);
        } else {
            round.setPlayer2Answer(answerIndex);
            round.setPlayer2TimeMs(timeMs);
            round.setPlayer2Points(points);
            // Cộng điểm vào điểm hiện tại từ database (đảm bảo không ghi đè điểm của
            // player1)
            game.setPlayer2Score(safeScore(game.getPlayer2Score()) + points);
        }

        gameRoundRepository.save(round);
        gameRepository.save(game);

        return new RoundResult(isCorrect, points, timeMs);
    }

    private boolean isCorrect(GameRound round, Integer answerIndex) {
        if (round == null || round.getCorrectAnswer() == null || answerIndex == null) {
            return false;
        }
        return round.getCorrectAnswer().equals(answerIndex);
    }

    private int calculatePoints(boolean isCorrect, long timeMs) {
        if (!isCorrect) {
            return 0;
        }
        if (timeMs <= 5000) {
            return 100;
        } else if (timeMs <= 10000) {
            return 50;
        } else if (timeMs <= 15000) {
            return 20;
        }
        return 0;
    }

    private int safeScore(Integer score) {
        return score == null ? 0 : score;
    }

    public boolean isRoundComplete(GameRound round) {
        return round.getPlayer1Answer() != null && round.getPlayer2Answer() != null;
    }

    public Game updateGame(Game game) {
        return gameRepository.save(game);
    }

    @Transactional
    public void finishRound(GameRound round) {
        round.setFinishedAt(LocalDateTime.now());
        gameRoundRepository.save(round);
    }

    @Transactional
    public Game finishGame(Game game) {
        game.setStatus(Game.GameStatus.FINISHED);
        game.setFinishedAt(LocalDateTime.now());

        boolean player1Won = game.getPlayer1Score() > game.getPlayer2Score();
        boolean player2Won = game.getPlayer2Score() > game.getPlayer1Score();

        if (player1Won) {
            game.setWinner(game.getPlayer1());
        } else if (player2Won) {
            game.setWinner(game.getPlayer2());
        }

        // Cập nhật thống kê người chơi
        int player1Correct = (int) gameRoundRepository.findByGameId(game.getId()).stream()
                .filter(r -> r.getPlayer1Answer() != null &&
                        r.getCorrectAnswer() != null &&
                        r.getPlayer1Answer().equals(r.getCorrectAnswer()))
                .count();

        int player2Correct = (int) gameRoundRepository.findByGameId(game.getId()).stream()
                .filter(r -> r.getPlayer2Answer() != null &&
                        r.getCorrectAnswer() != null &&
                        r.getPlayer2Answer().equals(r.getCorrectAnswer()))
                .count();

        userService.updateUserStats(game.getPlayer1(), player1Won, game.getPlayer1Score(), player1Correct);
        userService.updateUserStats(game.getPlayer2(), player2Won, game.getPlayer2Score(), player2Correct);

        return gameRepository.save(game);
    }

    /**
     * Kết thúc game do một người chơi rời trận (forfeit).
     * Người còn lại sẽ được tính thắng, điểm giữ nguyên như hiện tại.
     */
    @Transactional
    public Game finishGameByForfeit(Game game, User leaver) {
        game.setStatus(Game.GameStatus.FINISHED);
        game.setFinishedAt(LocalDateTime.now());

        boolean leaverIsP1 = game.getPlayer1().getId().equals(leaver.getId());
        User winner = leaverIsP1 ? game.getPlayer2() : game.getPlayer1();
        game.setWinner(winner);

        // Tính số câu đúng dựa trên các round đã có dữ liệu
        int player1Correct = (int) gameRoundRepository.findByGameId(game.getId()).stream()
                .filter(r -> r.getPlayer1Answer() != null &&
                        r.getCorrectAnswer() != null &&
                        r.getPlayer1Answer().equals(r.getCorrectAnswer()))
                .count();

        int player2Correct = (int) gameRoundRepository.findByGameId(game.getId()).stream()
                .filter(r -> r.getPlayer2Answer() != null &&
                        r.getCorrectAnswer() != null &&
                        r.getPlayer2Answer().equals(r.getCorrectAnswer()))
                .count();

        // Cập nhật thống kê: người rời trận thua, người còn lại thắng
        userService.updateUserStats(game.getPlayer1(), !leaverIsP1, game.getPlayer1Score(), player1Correct);
        userService.updateUserStats(game.getPlayer2(), leaverIsP1, game.getPlayer2Score(), player2Correct);

        return gameRepository.save(game);
    }

    public static class RoundResult {
        private final boolean correct;
        private final int points;
        private final long timeMs;

        public RoundResult(boolean correct, int points, long timeMs) {
            this.correct = correct;
            this.points = points;
            this.timeMs = timeMs;
        }

        public boolean isCorrect() {
            return correct;
        }

        public int getPoints() {
            return points;
        }

        public long getTimeMs() {
            return timeMs;
        }
    }
}
