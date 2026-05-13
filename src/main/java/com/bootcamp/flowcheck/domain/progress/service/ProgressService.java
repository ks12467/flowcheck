package com.bootcamp.flowcheck.domain.progress.service;

import com.bootcamp.flowcheck.domain.course.dto.CourseResponse;
import com.bootcamp.flowcheck.domain.course.entity.CourseWeek;
import com.bootcamp.flowcheck.domain.course.repository.CourseRepository;
import com.bootcamp.flowcheck.domain.course.repository.CourseWeekRepository;
import com.bootcamp.flowcheck.domain.progress.dto.*;
import com.bootcamp.flowcheck.domain.progress.entity.LearningProgress;
import com.bootcamp.flowcheck.domain.progress.repository.LearningProgressRepository;
import com.bootcamp.flowcheck.domain.student.entity.Student;
import com.bootcamp.flowcheck.domain.student.repository.StudentRepository;
import com.bootcamp.flowcheck.domain.track.entity.Track;
import com.bootcamp.flowcheck.domain.track.repository.PmTrackRepository;
import com.bootcamp.flowcheck.domain.track.repository.TrackRepository;
import com.bootcamp.flowcheck.global.exception.BusinessException;
import com.bootcamp.flowcheck.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressService {

    private static final Map<String, Integer> RISK_ORDER = Map.of(
            "즉시면담", 1, "주의", 2, "관찰", 3, "정상", 4
    );

    private final LearningProgressRepository progressRepository;
    private final StudentRepository studentRepository;
    private final TrackRepository trackRepository;
    private final CourseRepository courseRepository;
    private final CourseWeekRepository courseWeekRepository;
    private final PmTrackRepository pmTrackRepository;

    // ── 공개 API ─────────────────────────────────────────────────────────────

    public ProgressFormResponse getProgressForm(Long trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        List<CourseResponse> courses = courseRepository.findAllByTrackIdWithWeeks(trackId).stream()
                .map(CourseResponse::of)
                .toList();

        return new ProgressFormResponse(track.getName(), courses);
    }

    @Transactional
    public LearningProgressSubmitResponse submitProgress(Long trackId, LearningProgressSubmitRequest request) {
        trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        List<Student> students = studentRepository.findAllByTrack_IdAndName(trackId, request.getStudentName());
        if (students.isEmpty()) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND);
        }
        Student student = students.get(0);

        CourseWeek courseWeek = courseWeekRepository.findById(request.getCourseWeekId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_WEEK_NOT_FOUND));

        LocalDate today = LocalDate.now();
        String riskLevel = calculateRiskLevel(
                request.getConditionScore(), request.getUnderstandingScore(),
                request.getImmersionScore(), request.getComment(), student.getId(), today);

        LearningProgress progress = LearningProgress.builder()
                .student(student)
                .courseWeek(courseWeek)
                .lectureNumber(request.getLectureNumber())
                .status(request.getStatus())
                .assignmentProgress(request.getAssignmentProgress())
                .tilWritten(request.getTilWritten())
                .conditionScore(request.getConditionScore())
                .understandingScore(request.getUnderstandingScore())
                .immersionScore(request.getImmersionScore())
                .riskLevel(riskLevel)
                .comment(request.getComment())
                .submittedAt(LocalDateTime.now())
                .build();
        progressRepository.save(progress);

        int myScore = request.getConditionScore();
        Map<Integer, Long> scoreMap = fetchRecentScoreMap(trackId, myScore);

        long total = scoreMap.values().stream().mapToLong(Long::longValue).sum();
        int similarPercent = total == 0 ? 0
                : (int) Math.round(scoreMap.getOrDefault(myScore, 0L) * 100.0 / total);

        List<LearningProgressSubmitResponse.CaseStats> sorted = scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .map(e -> buildCaseStats(e.getKey(), e.getValue()))
                .toList();

        List<LearningProgressSubmitResponse.CaseStats> topCases = new ArrayList<>(
                sorted.stream().limit(2).toList());
        boolean myIncluded = topCases.stream().anyMatch(c -> c.getConditionScore() == myScore);
        if (!myIncluded) {
            sorted.stream().filter(c -> c.getConditionScore() == myScore).findFirst().ifPresent(topCases::add);
        } else if (sorted.size() > 2) {
            topCases.add(sorted.get(2));
        }

        LearningProgressSubmitResponse.CaseStats myCase =
                topCases.stream().filter(c -> c.getConditionScore() == myScore)
                        .findFirst().orElse(buildCaseStats(myScore, 1L));

        return new LearningProgressSubmitResponse(similarPercent, topCases, myCase);
    }

    // ── PM 인증 API ───────────────────────────────────────────────────────────

    public List<LearningProgressResponse> getProgressList(Long trackId, LocalDate date, String riskLevel) {
        Long pmId = getCurrentPmId();
        trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        validateAccess(pmId, trackId);

        List<LearningProgress> progressList = progressRepository.findAllByStudentTrackId(trackId);

        return progressList.stream()
                .filter(lp -> date == null || lp.getSubmittedAt().toLocalDate().equals(date))
                .filter(lp -> riskLevel == null || riskLevel.equals(lp.getRiskLevel()))
                .map(LearningProgressResponse::of)
                .sorted(Comparator.comparingInt(r -> getRiskOrder(r.getRiskLevel())))
                .toList();
    }

    public List<LearningProgressHistoryResponse> getStudentProgressHistory(Long trackId, Long studentId) {
        Long pmId = getCurrentPmId();
        trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        validateAccess(pmId, trackId);

        studentRepository.findByIdAndTrack_Id(studentId, trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        return progressRepository.findAllByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                .map(LearningProgressHistoryResponse::of)
                .toList();
    }

    // ── 위험군 자동 분류 ──────────────────────────────────────────────────────

    private String calculateRiskLevel(Integer conditionScore, Integer understandingScore,
                                      Integer immersionScore, String comment,
                                      Long studentId, LocalDate today) {
        if (conditionScore == null) return "정상";

        boolean hasComment = comment != null && !comment.isBlank();

        List<Integer> scores = new ArrayList<>();
        scores.add(conditionScore);
        if (understandingScore != null) scores.add(understandingScore);
        if (immersionScore != null) scores.add(immersionScore);

        long countFive = scores.stream().filter(s -> s == 5).count();
        long countFour = scores.stream().filter(s -> s == 4).count();

        // 즉시면담 조건
        if (countFive >= 1) return "즉시면담";
        if (countFour >= 2) return "즉시면담";
        if (countFour >= 1 && hasComment) return "즉시면담";

        // 주의/관찰/정상
        if (countFour >= 1) return "주의";
        if (conditionScore == 3 && hasComment) return "관찰";
        return "정상";
    }

    // ── 유사 수강생 비율 + 난이도 케이스 통계 ────────────────────────────────

    private static final Map<Integer, String> SCORE_LABELS = Map.of(
            1, "매우 쉬움", 2, "쉬움", 3, "보통", 4, "어려움", 5, "매우 어려움"
    );

    /**
     * 최근 3일 내 수강생별 최신 제출 1건 기준으로 난이도별 count 맵을 반환.
     * 방금 제출한 내 점수는 merge로 보장.
     */
    private Map<Integer, Long> fetchRecentScoreMap(Long trackId, int myScore) {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        List<Object[]> rows = progressRepository.countByConditionScoreRecent3Days(trackId, threeDaysAgo);

        Map<Integer, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            int score = ((Number) row[0]).intValue();
            long cnt  = ((Number) row[1]).longValue();
            map.put(score, cnt);
        }
        // 방금 제출한 내 케이스 반드시 포함 (쿼리 결과에 없을 수 있음)
        map.merge(myScore, 1L, Long::max);
        return map;
    }

    private LearningProgressSubmitResponse.CaseStats buildCaseStats(int score, long count) {
        return new LearningProgressSubmitResponse.CaseStats(
                score, count, SCORE_LABELS.getOrDefault(score, score + "점"));
    }

    // ── 공통 유틸 ─────────────────────────────────────────────────────────────

    private Long getCurrentPmId() {
        return Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private void validateAccess(Long pmId, Long trackId) {
        if (!pmTrackRepository.existsByPm_IdAndTrack_Id(pmId, trackId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private int getRiskOrder(String riskLevel) {
        if (riskLevel == null) return 5;
        return RISK_ORDER.getOrDefault(riskLevel, 5);
    }
}
