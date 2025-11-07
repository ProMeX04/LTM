package com.promex04.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Audio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column
    private String artist;

    @Column
    private String genre;

    @Column
    private String album;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;
}
