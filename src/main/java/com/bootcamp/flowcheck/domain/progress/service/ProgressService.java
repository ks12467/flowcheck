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
import java.util.stream.Collectors;

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
                request.getConditionScore(), request.getComment(), student.getId(), today);

        LearningProgress progress = LearningProgress.builder()
                .student(student)
                .courseWeek(courseWeek)
                .lectureNumber(request.getLectureNumber())
                .status(request.getStatus())
                .assignmentProgress(request.getAssignmentProgress())
                .tilWritten(request.getTilWritten())
                .conditionScore(request.getConditionScore())
                .riskLevel(riskLevel)
                .comment(request.getComment())
                .submittedAt(LocalDateTime.now())
                .build();
        progressRepository.save(progress);

        int similarPercent = calculateSimilarStudentPercent(trackId, request.getConditionScore());
        return new LearningProgressSubmitResponse(similarPercent);
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

    private String calculateRiskLevel(Integer conditionScore, String comment, Long studentId, LocalDate today) {
        if (conditionScore == null) return "정상";

        boolean hasComment = comment != null && !comment.isBlank();

        if (conditionScore >= 4 && hasComment) return "즉시면담";
        if (conditionScore >= 4 && hasConsecutive3Days(studentId, 4, today)) return "즉시면담";
        if (conditionScore == 3 && hasComment && hasConsecutive3Days(studentId, 3, today)) return "즉시면담";
        if (conditionScore >= 4) return "주의";
        if (conditionScore == 3 && hasComment) return "관찰";
        return "정상";
    }

    /**
     * 해당 학생이 minScore 이상의 conditionScore로 3일 이상 연속 제출했는지 확인.
     * today(오늘 제출 날짜)를 포함해서 계산한다.
     */
    private boolean hasConsecutive3Days(Long studentId, int minScore, LocalDate today) {
        List<LearningProgress> recent = progressRepository.findTop10ByStudent_IdOrderBySubmittedAtDesc(studentId);

        TreeSet<LocalDate> relevantDates = recent.stream()
                .filter(p -> p.getConditionScore() != null && p.getConditionScore() >= minScore)
                .map(p -> p.getSubmittedAt().toLocalDate())
                .collect(Collectors.toCollection(TreeSet::new));

        relevantDates.add(today); // 현재 제출 포함

        List<LocalDate> sorted = new ArrayList<>(relevantDates);

        int consecutive = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                if (++consecutive >= 3) return true;
            } else {
                consecutive = 1;
            }
        }
        return false;
    }

    // ── 유사 수강생 비율 ──────────────────────────────────────────────────────

    private int calculateSimilarStudentPercent(Long trackId, Integer conditionScore) {
        List<Student> trackStudents = studentRepository.findAllByTrack_Id(trackId);
        if (trackStudents.isEmpty()) return 0;

        List<Long> studentIds = trackStudents.stream().map(Student::getId).toList();
        List<LearningProgress> latestProgressList = progressRepository.findLatestByStudentIds(studentIds);

        if (latestProgressList.isEmpty()) return 0;

        long sameScore = latestProgressList.stream()
                .filter(lp -> Objects.equals(lp.getConditionScore(), conditionScore))
                .count();

        return (int) Math.round(sameScore * 100.0 / latestProgressList.size());
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
