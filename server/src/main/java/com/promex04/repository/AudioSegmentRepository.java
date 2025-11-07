package com.promex04.repository;

import com.promex04.model.AudioSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudioSegmentRepository extends JpaRepository<AudioSegment, Long> {
    List<AudioSegment> findByAudioId(Long audioId);
    
    @EntityGraph(attributePaths = {"audio", "audio.artist", "audio.genre"})
    @Query("SELECT s FROM AudioSegment s")
    List<AudioSegment> findAllWithAudio();
}

