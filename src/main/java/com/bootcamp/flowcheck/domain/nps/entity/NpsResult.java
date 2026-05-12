package com.bootcamp.flowcheck.domain.nps.entity;

import com.bootcamp.flowcheck.domain.track.entity.Track;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "NPS_RESULT")
@Getter
@NoArgsConstructor
public class NpsResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(nullable = false)
    private String name;

    @Column(name = "`class`")
    private String className;

    private String specialty;

    private Integer difficulty;
    private Integer skill;
    private Integer growth;
    private Integer communication;
    private Integer immersion;

    @Column(name = "ops_satisfaction")
    private Integer opsSatisfaction;

    @Column(name = "curriculum_satisfaction")
    private Integer curriculumSatisfaction;

    private Integer nps;

    @Column(name = "nps_comment", length = 2000)
    private String npsComment;

    @Column(name = "ops_comment", length = 2000)
    private String opsComment;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "upload_name", nullable = false)
    private String uploadName;

    @Builder
    public NpsResult(Track track, String name, String className, String specialty,
                     Integer difficulty, Integer skill, Integer growth,
                     Integer communication, Integer immersion,
                     Integer opsSatisfaction, Integer curriculumSatisfaction,
                     Integer nps, String npsComment, String opsComment,
                     LocalDateTime uploadedAt, String uploadName) {
        this.track = track;
        this.name = name;
        this.className = className;
        this.specialty = specialty;
        this.difficulty = difficulty;
        this.skill = skill;
        this.growth = growth;
        this.communication = communication;
        this.immersion = immersion;
        this.opsSatisfaction = opsSatisfaction;
        this.curriculumSatisfaction = curriculumSatisfaction;
        this.nps = nps;
        this.npsComment = npsComment;
        this.opsComment = opsComment;
        this.uploadedAt = uploadedAt;
        this.uploadName = uploadName;
    }
}
