package com.bootcamp.flowcheck.domain.survey.entity;

import com.bootcamp.flowcheck.domain.track.entity.Track;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "SURVEY_QUESTION")
@Getter
@NoArgsConstructor
public class SurveyQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "question_text", nullable = false, length = 500)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 10)
    private QuestionType questionType;

    @Column(name = "min_score")
    private Integer minScore;

    @Column(name = "max_score")
    private Integer maxScore;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum QuestionType { SCORE, TEXT }

    @Builder
    public SurveyQuestion(Track track, Integer questionOrder, String questionText,
                          QuestionType questionType, Integer minScore, Integer maxScore,
                          LocalDateTime createdAt) {
        this.track = track;
        this.questionOrder = questionOrder;
        this.questionText = questionText;
        this.questionType = questionType;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.createdAt = createdAt;
    }

    public void updateText(String questionText) {
        this.questionText = questionText;
    }
}
