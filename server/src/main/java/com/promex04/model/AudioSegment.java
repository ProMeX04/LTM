package com.promex04.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audio_segments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AudioSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_id", nullable = false)
    private Audio audio;

    @Column(name = "start_time", nullable = false)
    private Integer startTime; // Thời gian bắt đầu (giây)

    @Column(name = "end_time", nullable = false)
    private Integer endTime; // Thời gian kết thúc (giây)

    @Column(name = "file_path", nullable = false)
    private String filePath; // Đường dẫn đến file chunk đã cắt
}









