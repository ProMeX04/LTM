package com.promex04.service;

import com.promex04.model.*;
import com.promex04.repository.ArtistRepository;
import com.promex04.repository.AudioRepository;
import com.promex04.repository.AudioSegmentRepository;
import com.promex04.repository.GameRepository;
import com.promex04.repository.GameRoundRepository;
import com.promex04.repository.GenreRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final GameRoundRepository gameRoundRepository;
    private final AudioRepository audioRepository;
    private final AudioSegmentRepository audioSegmentRepository;
    private final ArtistRepository artistRepository;
    private final GenreRepository genreRepository;
    private final UserService userService;

    @Autowired
    public GameService(GameRepository gameRepository, GameRoundRepository gameRoundRepository,
            AudioRepository audioRepository, AudioSegmentRepository audioSegmentRepository,
            ArtistRepository artistRepository, GenreRepository genreRepository, UserService userService) {
        this.gameRepository = gameRepository;
        this.gameRoundRepository = gameRoundRepository;
        this.audioRepository = audioRepository;
        this.audioSegmentRepository = audioSegmentRepository;
        this.artistRepository = artistRepository;
        this.genreRepository = genreRepository;
        this.userService = userService;
    }

    public Game createGame(User player1, User player2) {
        return createGame(player1, player2, null, null);
    }

    public Game createGame(User player1, User player2, String preferredArtist, String preferredGenre) {
        Game game = new Game();
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(Game.GameStatus.IN_PROGRESS);
        game.setPreferredArtist(preferredArtist);
        game.setPreferredGenre(preferredGenre);
        return gameRepository.save(game);
    }

    @Transactional(readOnly = true)
    public AudioSegment getRandomAudioSegment() {
        List<AudioSegment> allSegments = audioSegmentRepository.findAllWithAudio();
        if (allSegments.isEmpty()) {
            // Tạo audio mẫu nếu chưa có
            createSampleAudio();
            allSegments = audioSegmentRepository.findAllWithAudio();
        }
        Collections.shuffle(allSegments);
        return allSegments.get(0);
    }

    @Transactional(readOnly = true)
    public List<AudioSegment> getRandomAudioSegments(int n) {
        return getRandomAudioSegments(n, null, null);
    }

    @Transactional(readOnly = true)
    public List<AudioSegment> getRandomAudioSegments(int n, String preferredArtist, String preferredGenre) {
        List<AudioSegment> allSegments = audioSegmentRepository.findAllWithAudio();
        if (allSegments.isEmpty()) {
            // Đảm bảo có ít nhất 1 segment mẫu để không lỗi
            createSampleAudio();
            allSegments = audioSegmentRepository.findAllWithAudio();
        }

        // Force initialize tất cả các lazy proxy trong transaction để tránh LazyInitializationException
        // Đảm bảo audio, artist và genre được load đầy đủ trước khi rời khỏi transaction
        List<AudioSegment> initializedSegments = new ArrayList<>();
        for (AudioSegment segment : allSegments) {
            // Force load audio nếu chưa được load bằng Hibernate.initialize()
            Audio audio = segment.getAudio();
            if (audio != null) {
                // Force initialize audio proxy
                Hibernate.initialize(audio);
                // Force initialize artist và genre proxy
                if (audio.getArtist() != null) {
                    Hibernate.initialize(audio.getArtist());
                    // Truy cập name để đảm bảo được load
                    audio.getArtist().getName();
                }
                if (audio.getGenre() != null) {
                    Hibernate.initialize(audio.getGenre());
                    // Truy cập name để đảm bảo được load
                    audio.getGenre().getName();
                }
            }
            initializedSegments.add(segment);
        }

        // Sử dụng danh sách đã được initialize để filter
        List<AudioSegment> filtered = initializedSegments.stream()
                .filter(segment -> {
                    Audio audio = segment.getAudio();
                    if (audio == null) return false;
                    String artistName = audio.getArtist() != null ? audio.getArtist().getName() : null;
                    return matchesPreference(artistName, preferredArtist);
                })
                .filter(segment -> {
                    Audio audio = segment.getAudio();
                    if (audio == null) return false;
                    String genreName = audio.getGenre() != null ? audio.getGenre().getName() : null;
                    return matchesPreference(genreName, preferredGenre);
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            filtered = new ArrayList<>(initializedSegments);
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
        // Tìm hoặc tạo artist và genre mẫu
        Artist artist = artistRepository.findByName("Unknown Artist")
                .orElseGet(() -> {
                    Artist a = new Artist();
                    a.setName("Unknown Artist");
                    return artistRepository.save(a);
                });
        
        Genre genre = genreRepository.findByName("Sample")
                .orElseGet(() -> {
                    Genre g = new Genre();
                    g.setName("Sample");
                    return genreRepository.save(g);
                });
        
        Audio audio = new Audio();
        audio.setName("Sample Audio");
        audio.setFilePath("/sample/audio.mp3");
        audio.setArtist(artist);
        audio.setGenre(genre);
        audio = audioRepository.save(audio);
        
        // Tạo segment mẫu
        AudioSegment segment = new AudioSegment();
        segment.setAudio(audio);
        segment.setStartTime(0);
        segment.setEndTime(30);
        segment.setFilePath("/sample/audio.mp3");
        audioSegmentRepository.save(segment);
        
        return audio;
    }

    public List<String> getDistinctArtists() {
        return artistRepository.findAll().stream()
                .map(Artist::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public List<String> getDistinctGenres() {
        return genreRepository.findAll().stream()
                .map(Genre::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> searchArtists(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getDistinctArtists();
        }
        return artistRepository.searchByName(keyword.trim()).stream()
                .map(Artist::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> searchGenres(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getDistinctGenres();
        }
        return genreRepository.searchByName(keyword.trim()).stream()
                .map(Genre::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public GameRound createRound(Game game, int roundNumber) {
        AudioSegment segment = getRandomAudioSegment();
        return createRoundWithSegment(game, roundNumber, segment);
    }

    public GameRound createRoundWithSegment(Game game, int roundNumber, AudioSegment segment) {
        GameRound round = new GameRound();
        round.setGame(game);
        round.setAudioSegment(segment);
        round.setRoundNumber(roundNumber);
        round.setStartedAt(LocalDateTime.now());

        // Lấy artist và genre đã chọn từ Game (nếu có)
        String preferredArtist = game.getPreferredArtist();
        String preferredGenre = game.getPreferredGenre();

        // Lấy tất cả segments
        List<AudioSegment> allSegments = audioSegmentRepository.findAllWithAudio();
        
        // Force initialize tất cả để tránh LazyInitializationException
        for (AudioSegment s : allSegments) {
            Audio audio = s.getAudio();
            if (audio != null) {
                Hibernate.initialize(audio);
                if (audio.getArtist() != null) {
                    Hibernate.initialize(audio.getArtist());
                }
                if (audio.getGenre() != null) {
                    Hibernate.initialize(audio.getGenre());
                }
            }
        }
        
        // Filter các segments để lấy đáp án nhiễu
        List<AudioSegment> filteredSegments;
        
        // Kiểm tra xem có chọn artist hoặc genre không
        boolean hasArtistFilter = preferredArtist != null && !preferredArtist.isBlank();
        boolean hasGenreFilter = preferredGenre != null && !preferredGenre.isBlank();
        
        // Nếu có chọn artist/genre khi thách đấu, filter theo đó
        if (hasArtistFilter || hasGenreFilter) {
            filteredSegments = allSegments.stream()
                    .filter(s -> {
                        // Loại bỏ segment đúng
                        if (s.getId().equals(segment.getId())) {
                            return false;
                        }
                        
                        Audio audio = s.getAudio();
                        if (audio == null) return false;
                        
                        // Kiểm tra artist (nếu có chọn artist)
                        if (hasArtistFilter && preferredArtist != null) {
                            String audioArtistName = audio.getArtist() != null ? audio.getArtist().getName() : null;
                            if (audioArtistName == null || !preferredArtist.equalsIgnoreCase(audioArtistName)) {
                                return false;
                            }
                        }
                        
                        // Kiểm tra genre (nếu có chọn genre)
                        if (hasGenreFilter && preferredGenre != null) {
                            String audioGenreName = audio.getGenre() != null ? audio.getGenre().getName() : null;
                            if (audioGenreName == null || !preferredGenre.equalsIgnoreCase(audioGenreName)) {
                                return false;
                            }
                        }
                        
                        return true;
                    })
                    .collect(Collectors.toList());
        } else {
            // Nếu không chọn artist/genre, lấy ngẫu nhiên từ tất cả
            filteredSegments = allSegments.stream()
                    .filter(s -> !s.getId().equals(segment.getId()))
                    .collect(Collectors.toList());
        }
        
        // Nếu không có đủ segments, lấy từ tất cả (fallback)
        if (filteredSegments.size() < 3) {
            filteredSegments = allSegments.stream()
                    .filter(s -> !s.getId().equals(segment.getId()))
                    .collect(Collectors.toList());
        }
        
        Collections.shuffle(filteredSegments);

        String correct = segment.getAudio().getName();
        String o2 = filteredSegments.size() >= 1 ? filteredSegments.get(0).getAudio().getName() : "Lựa chọn 2";
        String o3 = filteredSegments.size() >= 2 ? filteredSegments.get(1).getAudio().getName() : "Lựa chọn 3";
        String o4 = filteredSegments.size() >= 3 ? filteredSegments.get(2).getAudio().getName() : "Lựa chọn 4";

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
    public GameRound findOrCreateRoundWithSegment(Game game, int roundNumber, AudioSegment segment) {
        // Kiểm tra xem đã có round cho roundNumber này chưa
        var existingRound = gameRoundRepository.findByGameIdAndRoundNumber(game.getId(), roundNumber);
        if (existingRound.isPresent()) {
            // Đã có round, kiểm tra xem segment có khớp không
            GameRound round = existingRound.get();
            if (round.getAudioSegment().getId().equals(segment.getId())) {
                return round; // Dùng lại round đã có
            }
            // Nếu segment không khớp, tạo mới (trường hợp hiếm - có thể do lỗi logic)
        }
        // Chưa có round, tạo mới
        return createRoundWithSegment(game, roundNumber, segment);
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
