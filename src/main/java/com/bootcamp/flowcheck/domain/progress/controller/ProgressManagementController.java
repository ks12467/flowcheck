package com.bootcamp.flowcheck.domain.progress.controller;

import com.bootcamp.flowcheck.domain.progress.dto.LearningProgressHistoryResponse;
import com.bootcamp.flowcheck.domain.progress.dto.LearningProgressResponse;
import com.bootcamp.flowcheck.domain.progress.service.ProgressService;
import com.bootcamp.flowcheck.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * PM용 진척도 관리 컨트롤러 (JWT 인증 필요)
 */
@RestController
@RequestMapping("/api/v1/tracks/{trackId}")
@RequiredArgsConstructor
public class ProgressManagementController {

    private final ProgressService progressService;

    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<List<LearningProgressResponse>>> getProgressList(
            @PathVariable Long trackId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String riskLevel) {
        return ResponseEntity.ok(ApiResponse.success(
                progressService.getProgressList(trackId, date, riskLevel), "학습 진척도 목록을 조회했습니다."));
    }

    @GetMapping("/students/{studentId}/progress")
    public ResponseEntity<ApiResponse<List<LearningProgressHistoryResponse>>> getStudentProgressHistory(
            @PathVariable Long trackId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(
                progressService.getStudentProgressHistory(trackId, studentId), "수강생 진척도 히스토리를 조회했습니다."));
    }
}
