package com.bootcamp.flowcheck.domain.progress.controller;

import com.bootcamp.flowcheck.domain.progress.dto.LearningProgressSubmitRequest;
import com.bootcamp.flowcheck.domain.progress.dto.LearningProgressSubmitResponse;
import com.bootcamp.flowcheck.domain.progress.dto.ProgressFormResponse;
import com.bootcamp.flowcheck.domain.progress.service.ProgressService;
import com.bootcamp.flowcheck.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 수강생용 진척도 컨트롤러 (인증 불필요 - SecurityConfig에서 /progress/** permitAll)
 */
@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/{trackId}/form")
    public ResponseEntity<ApiResponse<ProgressFormResponse>> getProgressForm(@PathVariable Long trackId) {
        return ResponseEntity.ok(ApiResponse.success(
                progressService.getProgressForm(trackId), "제출 폼 정보를 조회했습니다."));
    }

    @PostMapping("/{trackId}")
    public ResponseEntity<ApiResponse<LearningProgressSubmitResponse>> submitProgress(
            @PathVariable Long trackId,
            @RequestBody @Valid LearningProgressSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                progressService.submitProgress(trackId, request), "학습 진척도를 제출했습니다."));
    }
}
