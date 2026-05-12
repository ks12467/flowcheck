package com.bootcamp.flowcheck.domain.satisfaction.entity;

import com.bootcamp.flowcheck.domain.track.entity.Track;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "SATISFACTION_RESULT")
@Getter
@NoArgsConstructor
public class SatisfactionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "upload_name", nullable = false)
    private String uploadName;

    @Column(name = "respondent_name")
    private String respondentName;

    // ── 커리큘럼 ──────────────────────────────────────────
    @Column(name = "curriculum_satisfaction")
    private Integer curriculumSatisfaction;

    @Column(name = "curriculum_difficulty")
    private Integer curriculumDifficulty;

    // ── 강의 ──────────────────────────────────────────────
    @Column(name = "lecture_delivery")
    private Integer lectureDelivery;

    @Column(name = "lecture_material")
    private Integer lectureMaterial;

    @Column(name = "lecture_quality")
    private Integer lectureQuality;

    @Column(name = "lecture_difficulty")
    private Integer lectureDifficulty;

    @Column(name = "lecture_nps")
    private Integer lectureNps;

    @Column(name = "assignment_satisfaction")
    private Integer assignmentSatisfaction;

    // ── 운영 ──────────────────────────────────────────────
    @Column(name = "scrum_score")
    private Integer scrumScore;

    @Column(name = "tutor_help_score")
    private Integer tutorHelpScore;

    @Column(name = "tutor_meet_score")
    private Integer tutorMeetScore;

    @Column(name = "manager_score")
    private Integer managerScore;

    @Column(name = "feedback_speed_score")
    private Integer feedbackSpeedScore;

    @Column(name = "brief_score")
    private Integer briefScore;

    // ── 의견 ──────────────────────────────────────────────
    @Column(name = "free_comment", length = 2000)
    private String freeComment;

    @Column(name = "operation_comment", length = 2000)
    private String operationComment;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @Builder
    public SatisfactionResult(Track track, String uploadName, String respondentName,
                               Integer curriculumSatisfaction, Integer curriculumDifficulty,
                               Integer lectureDelivery, Integer lectureMaterial,
                               Integer lectureQuality, Integer lectureDifficulty,
                               Integer lectureNps, Integer assignmentSatisfaction,
                               Integer scrumScore, Integer tutorHelpScore,
                               Integer tutorMeetScore, Integer managerScore,
                               Integer feedbackSpeedScore, Integer briefScore,
                               String freeComment, String operationComment,
                               LocalDateTime uploadedAt) {
        this.track = track;
        this.uploadName = uploadName;
        this.respondentName = respondentName;
        this.curriculumSatisfaction = curriculumSatisfaction;
        this.curriculumDifficulty = curriculumDifficulty;
        this.lectureDelivery = lectureDelivery;
        this.lectureMaterial = lectureMaterial;
        this.lectureQuality = lectureQuality;
        this.lectureDifficulty = lectureDifficulty;
        this.lectureNps = lectureNps;
        this.assignmentSatisfaction = assignmentSatisfaction;
        this.scrumScore = scrumScore;
        this.tutorHelpScore = tutorHelpScore;
        this.tutorMeetScore = tutorMeetScore;
        this.managerScore = managerScore;
        this.feedbackSpeedScore = feedbackSpeedScore;
        this.briefScore = briefScore;
        this.freeComment = freeComment;
        this.operationComment = operationComment;
        this.uploadedAt = uploadedAt;
    }
}
