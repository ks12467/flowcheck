package com.bootcamp.flowcheck.domain.student.service;

import com.bootcamp.flowcheck.domain.progress.entity.LearningProgress;
import com.bootcamp.flowcheck.domain.progress.repository.LearningProgressRepository;
import com.bootcamp.flowcheck.domain.student.dto.*;
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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private static final Map<String, Integer> RISK_ORDER = Map.of(
            "즉시면담", 1,
            "주의", 2,
            "관찰", 3,
            "정상", 4
    );

    private final StudentRepository studentRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final TrackRepository trackRepository;
    private final PmTrackRepository pmTrackRepository;

    public List<StudentResponse> getStudents(Long trackId, String riskLevel, String keyword) {
        Long pmId = getCurrentPmId();
        validateTrack(trackId);
        validateAccess(pmId, trackId);

        List<Student> students = (keyword != null && !keyword.isBlank())
                ? studentRepository.findByTrackIdAndKeyword(trackId, keyword)
                : studentRepository.findAllByTrack_Id(trackId);

        if (students.isEmpty()) {
            return List.of();
        }

        List<Long> studentIds = students.stream().map(Student::getId).toList();
        Map<Long, LearningProgress> latestProgressMap = learningProgressRepository
                .findLatestByStudentIds(studentIds).stream()
                .collect(Collectors.toMap(lp -> lp.getStudent().getId(), Function.identity()));

        return students.stream()
                .map(s -> StudentResponse.of(s, latestProgressMap.get(s.getId())))
                .filter(r -> riskLevel == null || riskLevel.equals(r.getRiskLevel()))
                .sorted(Comparator.comparingInt(r -> getRiskOrder(r.getRiskLevel())))
                .toList();
    }

    public StudentDetailResponse getStudent(Long trackId, Long studentId) {
        Long pmId = getCurrentPmId();
        validateTrack(trackId);
        validateAccess(pmId, trackId);

        Student student = studentRepository.findByIdAndTrack_Id(studentId, trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        LearningProgress latestProgress = learningProgressRepository
                .findTopByStudent_IdOrderBySubmittedAtDesc(studentId)
                .orElse(null);

        return StudentDetailResponse.of(student, latestProgress);
    }

    @Transactional
    public StudentResponse createStudent(Long trackId, StudentCreateRequest request) {
        Long pmId = getCurrentPmId();
        Track track = validateTrack(trackId);
        validateAccess(pmId, trackId);
        checkDuplicate(trackId, request.getEmail());

        Student student = Student.builder()
                .track(track)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .createdAt(LocalDateTime.now())
                .build();
        studentRepository.save(student);

        return StudentResponse.of(student, null);
    }

    @Transactional
    public BulkCreateResponse bulkCreateStudents(Long trackId, StudentBulkCreateRequest request) {
        Long pmId = getCurrentPmId();
        Track track = validateTrack(trackId);
        validateAccess(pmId, trackId);

        int successCount = 0;
        int failCount = 0;

        for (StudentCreateRequest req : request.getStudents()) {
            try {
                if (req.getEmail() != null && studentRepository.existsByTrack_IdAndEmail(trackId, req.getEmail())) {
                    failCount++;
                    continue;
                }
                studentRepository.save(Student.builder()
                        .track(track)
                        .name(req.getName())
                        .email(req.getEmail())
                        .phone(req.getPhone())
                        .createdAt(LocalDateTime.now())
                        .build());
                successCount++;
            } catch (Exception e) {
                failCount++;
            }
        }

        return new BulkCreateResponse(successCount, failCount);
    }

    @Transactional
    public StudentResponse updateStudent(Long trackId, Long studentId, StudentUpdateRequest request) {
        Long pmId = getCurrentPmId();
        validateTrack(trackId);
        validateAccess(pmId, trackId);

        Student student = studentRepository.findByIdAndTrack_Id(studentId, trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        student.update(request.getName(), request.getEmail(), request.getPhone());

        LearningProgress latestProgress = learningProgressRepository
                .findTopByStudent_IdOrderBySubmittedAtDesc(studentId)
                .orElse(null);

        return StudentResponse.of(student, latestProgress);
    }

    @Transactional
    public void deleteStudent(Long trackId, Long studentId) {
        Long pmId = getCurrentPmId();
        validateTrack(trackId);
        validateAccess(pmId, trackId);

        Student student = studentRepository.findByIdAndTrack_Id(studentId, trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        student.softDelete();
    }

    private Long getCurrentPmId() {
        return Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private Track validateTrack(Long trackId) {
        return trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
    }

    private void validateAccess(Long pmId, Long trackId) {
        if (!pmTrackRepository.existsByPm_IdAndTrack_Id(pmId, trackId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void checkDuplicate(Long trackId, String email) {
        if (email != null && studentRepository.existsByTrack_IdAndEmail(trackId, email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_STUDENT);
        }
    }

    private int getRiskOrder(String riskLevel) {
        if (riskLevel == null) return 5;
        return RISK_ORDER.getOrDefault(riskLevel, 5);
    }
}
