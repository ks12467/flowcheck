package com.bootcamp.flowcheck.web;

import com.bootcamp.flowcheck.domain.auth.entity.Pm;
import com.bootcamp.flowcheck.domain.auth.repository.PmRepository;
import com.bootcamp.flowcheck.domain.course.service.CourseService;
import com.bootcamp.flowcheck.domain.progress.entity.LearningProgress;
import com.bootcamp.flowcheck.domain.progress.repository.LearningProgressRepository;
import com.bootcamp.flowcheck.domain.student.dto.StudentBulkCreateRequest;
import com.bootcamp.flowcheck.domain.student.dto.StudentCreateRequest;
import com.bootcamp.flowcheck.domain.student.dto.StudentUpdateRequest;
import com.bootcamp.flowcheck.domain.student.entity.Student;
import com.bootcamp.flowcheck.domain.student.repository.StudentRepository;
import com.bootcamp.flowcheck.domain.track.dto.TrackCreateRequest;
import com.bootcamp.flowcheck.domain.track.dto.TrackResponse;
import com.bootcamp.flowcheck.domain.track.dto.TrackUpdateRequest;
import com.bootcamp.flowcheck.domain.track.entity.PmTrack;
import com.bootcamp.flowcheck.domain.track.entity.Track;
import com.bootcamp.flowcheck.domain.track.repository.PmTrackRepository;
import com.bootcamp.flowcheck.domain.track.repository.TrackRepository;
import com.bootcamp.flowcheck.global.exception.BusinessException;
import com.bootcamp.flowcheck.global.exception.ErrorCode;
import com.bootcamp.flowcheck.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PmRepository pmRepository;
    private final TrackRepository trackRepository;
    private final PmTrackRepository pmTrackRepository;
    private final StudentRepository studentRepository;
    private final LearningProgressRepository progressRepository;
    private final CourseService courseService;

    // ── 어드민 트랙 관리 ─────────────────────────────────────────────────────

    @GetMapping("/tracks")
    public ResponseEntity<ApiResponse<List<TrackResponse>>> getAllTracks() {
        List<TrackResponse> tracks = trackRepository.findAll().stream()
                .map(t -> TrackResponse.of(t, trackRepository.countStudentsByTrackId(t.getId())))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(tracks, "트랙 목록을 조회했습니다."));
    }

    @PostMapping("/tracks")
    public ResponseEntity<ApiResponse<TrackResponse>> createTrack(
            @RequestBody @Valid TrackCreateRequest req) {
        Track track = Track.builder()
                .name(req.buildName())
                .courseType(req.getCourseType())
                .generation(req.getGeneration())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .createdAt(LocalDateTime.now())
                .build();
        trackRepository.save(track);
        return ResponseEntity.ok(ApiResponse.success(TrackResponse.of(track, 0), "트랙이 생성되었습니다."));
    }

    @PutMapping("/tracks/{trackId}")
    public ResponseEntity<ApiResponse<TrackResponse>> updateTrack(
            @PathVariable Long trackId,
            @RequestBody @Valid TrackUpdateRequest req) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        track.update(req.buildName(), req.getCourseType(), req.getGeneration(),
                req.getStartDate(), req.getEndDate());
        trackRepository.save(track);
        long studentCount = trackRepository.countStudentsByTrackId(trackId);
        return ResponseEntity.ok(ApiResponse.success(TrackResponse.of(track, studentCount), "트랙이 수정되었습니다."));
    }

    @DeleteMapping("/tracks/{trackId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteTrack(@PathVariable Long trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        track.softDelete();
        return ResponseEntity.ok(ApiResponse.success(null, "트랙이 삭제되었습니다."));
    }

    // ── 삭제된 트랙 조회 / 복구 / 영구 삭제 ────────────────────────────────────

    @GetMapping("/tracks/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDeletedTracks() {
        List<Map<String, Object>> result = trackRepository.findAllDeleted().stream().map(t -> {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("trackId", t.getId());
            v.put("name", t.getName());
            v.put("courseType", t.getCourseType());
            v.put("generation", t.getGeneration());
            v.put("startDate", t.getStartDate());
            v.put("endDate", t.getEndDate());
            v.put("deletedAt", t.getDeletedAt());
            return v;
        }).toList();
        return ResponseEntity.ok(ApiResponse.success(result, "삭제된 트랙 목록입니다."));
    }

    @PostMapping("/tracks/{trackId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> restoreTrack(@PathVariable Long trackId) {
        trackRepository.findDeletedById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        trackRepository.restoreById(trackId);
        return ResponseEntity.ok(ApiResponse.success(null, "트랙이 복구되었습니다."));
    }

    @DeleteMapping("/tracks/{trackId}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> permanentDeleteTrack(@PathVariable Long trackId) {
        trackRepository.findDeletedById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        trackRepository.permanentDeleteById(trackId);
        return ResponseEntity.ok(ApiResponse.success(null, "트랙이 영구 삭제되었습니다."));
    }

    // ── PM 트랙 배정 ─────────────────────────────────────────────────────────

    @PostMapping("/pms/{pmId}/tracks/{trackId}")
    public ResponseEntity<ApiResponse<Void>> assignTrack(@PathVariable Long pmId,
                                                         @PathVariable Long trackId) {
        Pm pm = pmRepository.findById(pmId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        if (pmTrackRepository.existsByPm_IdAndTrack_Id(pmId, trackId)) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }

        pmTrackRepository.save(PmTrack.builder().pm(pm).track(track).role("MANAGER").build());
        return ResponseEntity.ok(ApiResponse.success(null, "트랙이 배정되었습니다."));
    }

    @DeleteMapping("/pms/{pmId}/tracks/{trackId}")
    public ResponseEntity<ApiResponse<Void>> removeTrack(@PathVariable Long pmId,
                                                         @PathVariable Long trackId) {
        pmTrackRepository.deleteByPm_IdAndTrack_Id(pmId, trackId);
        return ResponseEntity.ok(ApiResponse.success(null, "트랙 배정이 해제되었습니다."));
    }

    // ── 어드민 수강생 관리 ────────────────────────────────────────────────────

    @GetMapping("/tracks/{trackId}/students")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStudents(
            @PathVariable Long trackId) {
        trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        List<Student> students = studentRepository.findAllByTrack_Id(trackId);

        Map<Long, LearningProgress> latestMap = students.isEmpty() ? Map.of()
                : progressRepository.findLatestByStudentIds(
                        students.stream().map(Student::getId).toList()).stream()
                .collect(Collectors.toMap(lp -> lp.getStudent().getId(), Function.identity()));

        List<Map<String, Object>> result = students.stream().map(s -> {
            LearningProgress lp = latestMap.get(s.getId());
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("studentId", s.getId());
            v.put("name", s.getName());
            v.put("email", s.getEmail());
            v.put("phone", s.getPhone());
            v.put("riskLevel", lp != null ? lp.getRiskLevel() : null);
            v.put("submittedAt", lp != null ? lp.getSubmittedAt() : null);
            return v;
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(result, "수강생 목록을 조회했습니다."));
    }

    @PostMapping("/tracks/{trackId}/students")
    public ResponseEntity<ApiResponse<Void>> createStudent(@PathVariable Long trackId,
                                                           @RequestBody @Valid StudentCreateRequest req) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        if (req.getEmail() != null && studentRepository.existsByTrack_IdAndEmail(trackId, req.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_STUDENT);
        }

        studentRepository.save(Student.builder()
                .track(track).name(req.getName()).email(req.getEmail())
                .phone(req.getPhone()).createdAt(LocalDateTime.now()).build());

        return ResponseEntity.ok(ApiResponse.success(null, "수강생이 등록되었습니다."));
    }

    @PostMapping("/tracks/{trackId}/students/bulk")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkCreateStudents(
            @PathVariable Long trackId,
            @RequestBody @Valid StudentBulkCreateRequest req) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        int success = 0, fail = 0;
        for (var r : req.getStudents()) {
            try {
                if (r.getEmail() != null && studentRepository.existsByTrack_IdAndEmail(trackId, r.getEmail())) {
                    fail++; continue;
                }
                studentRepository.save(Student.builder()
                        .track(track).name(r.getName()).email(r.getEmail())
                        .phone(r.getPhone()).createdAt(LocalDateTime.now()).build());
                success++;
            } catch (Exception e) { fail++; }
        }

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("successCount", success, "failCount", fail), "일괄 등록이 완료되었습니다."));
    }

    @PutMapping("/tracks/{trackId}/students/{studentId}")
    public ResponseEntity<ApiResponse<Void>> updateStudent(@PathVariable Long trackId,
                                                           @PathVariable Long studentId,
                                                           @RequestBody @Valid StudentUpdateRequest req) {
        Student student = studentRepository.findByIdAndTrack_Id(studentId, trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
        student.update(req.getName(), req.getEmail(), req.getPhone());
        studentRepository.save(student);
        return ResponseEntity.ok(ApiResponse.success(null, "수강생 정보가 수정되었습니다."));
    }

    @DeleteMapping("/tracks/{trackId}/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long trackId,
                                                           @PathVariable Long studentId) {
        Student student = studentRepository.findByIdAndTrack_Id(studentId, trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
        student.softDelete();
        return ResponseEntity.ok(ApiResponse.success(null, "수강생이 삭제되었습니다."));
    }

    // ── 삭제된 수강생 조회 / 복구 / 영구 삭제 ──────────────────────────────────

    @GetMapping("/students/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDeletedStudents() {
        List<Map<String, Object>> result = studentRepository.findAllDeleted().stream().map(s -> {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("studentId", s.getId());
            v.put("name", s.getName());
            v.put("email", s.getEmail());
            v.put("phone", s.getPhone());
            try {
                Track t = s.getTrack();
                v.put("trackId", t != null ? t.getId() : null);
                v.put("trackName", t != null ? t.getName() : "-");
            } catch (Exception e) {
                v.put("trackId", null);
                v.put("trackName", "-");
            }
            v.put("deletedAt", s.getDeletedAt());
            return v;
        }).toList();
        return ResponseEntity.ok(ApiResponse.success(result, "삭제된 수강생 목록입니다."));
    }

    @PostMapping("/students/{studentId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> restoreStudent(@PathVariable Long studentId) {
        studentRepository.findDeletedById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
        studentRepository.restoreById(studentId);
        return ResponseEntity.ok(ApiResponse.success(null, "수강생이 복구되었습니다."));
    }

    @DeleteMapping("/students/{studentId}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> permanentDeleteStudent(@PathVariable Long studentId) {
        studentRepository.findDeletedById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
        studentRepository.permanentDeleteById(studentId);
        return ResponseEntity.ok(ApiResponse.success(null, "수강생이 영구 삭제되었습니다."));
    }

    // ── 강의 영구 삭제 ────────────────────────────────────────────────────────

    @DeleteMapping("/courses/{courseId}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> permanentDeleteCourse(@PathVariable Long courseId) {
        courseService.permanentDeleteCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success(null, "강의가 영구 삭제되었습니다."));
    }
}
