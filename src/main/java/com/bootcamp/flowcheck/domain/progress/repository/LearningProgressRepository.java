package com.bootcamp.flowcheck.domain.progress.repository;

import com.bootcamp.flowcheck.domain.progress.entity.LearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LearningProgressRepository extends JpaRepository<LearningProgress, Long> {

    Optional<LearningProgress> findTopByStudent_IdOrderBySubmittedAtDesc(Long studentId);

    List<LearningProgress> findTop10ByStudent_IdOrderBySubmittedAtDesc(Long studentId);

    @Query("""
            SELECT lp FROM LearningProgress lp
            WHERE lp.student.id IN :studentIds
            AND lp.submittedAt = (
                SELECT MAX(lp2.submittedAt) FROM LearningProgress lp2
                WHERE lp2.student.id = lp.student.id
            )
            """)
    List<LearningProgress> findLatestByStudentIds(@Param("studentIds") List<Long> studentIds);

    @Query("""
            SELECT lp FROM LearningProgress lp
            JOIN FETCH lp.student s
            JOIN FETCH lp.courseWeek cw
            JOIN FETCH cw.course c
            WHERE s.track.id = :trackId
            ORDER BY lp.submittedAt DESC
            """)
    List<LearningProgress> findAllByStudentTrackId(@Param("trackId") Long trackId);

    @Query("""
            SELECT lp FROM LearningProgress lp
            JOIN FETCH lp.courseWeek cw
            JOIN FETCH cw.course c
            WHERE lp.student.id = :studentId
            ORDER BY lp.submittedAt DESC
            """)
    List<LearningProgress> findAllByStudentIdOrderBySubmittedAtDesc(@Param("studentId") Long studentId);

    @Query("""
            SELECT lp FROM LearningProgress lp
            JOIN FETCH lp.student s
            JOIN FETCH s.track t
            WHERE t.id IN :trackIds
            AND lp.riskLevel = :riskLevel
            AND lp.submittedAt = (
                SELECT MAX(lp2.submittedAt) FROM LearningProgress lp2
                WHERE lp2.student.id = lp.student.id
            )
            """)
    List<LearningProgress> findLatestWithRiskLevelInTracks(
            @Param("trackIds") List<Long> trackIds,
            @Param("riskLevel") String riskLevel);

    @Query("""
            SELECT COUNT(DISTINCT lp.student.id) FROM LearningProgress lp
            WHERE lp.student.track.id = :trackId
            AND lp.submittedAt >= :startOfDay
            """)
    long countTodaySubmissionsByTrackId(
            @Param("trackId") Long trackId,
            @Param("startOfDay") LocalDateTime startOfDay);

    @Query("""
            SELECT lp.conditionScore, COUNT(lp) FROM LearningProgress lp
            WHERE lp.student.track.id IN :trackIds
            AND lp.submittedAt = (
                SELECT MAX(lp2.submittedAt) FROM LearningProgress lp2
                WHERE lp2.student.id = lp.student.id
            )
            GROUP BY lp.conditionScore
            ORDER BY lp.conditionScore
            """)
    List<Object[]> countByConditionScoreInTracks(@Param("trackIds") List<Long> trackIds);
}
