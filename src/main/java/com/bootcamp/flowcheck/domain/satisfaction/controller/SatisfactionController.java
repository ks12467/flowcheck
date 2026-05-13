package com.bootcamp.flowcheck.domain.satisfaction.controller;

import com.bootcamp.flowcheck.domain.satisfaction.dto.SatisfactionAiReportResponse;
import com.bootcamp.flowcheck.domain.satisfaction.dto.SatisfactionAnalysisResponse;
import com.bootcamp.flowcheck.domain.satisfaction.service.SatisfactionService;
import com.bootcamp.flowcheck.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/satisfaction")
@RequiredArgsConstructor
public class SatisfactionController {

    private final SatisfactionService satisfactionService;

    /** GET /api/v1/admin/satisfaction/uploads?trackId=1 — 업로드 이름 목록 */
    @GetMapping("/uploads")
    public ResponseEntity<ApiResponse<List<String>>> uploads(@RequestParam Long trackId) {
        return ResponseEntity.ok(ApiResponse.success(
                satisfactionService.getUploadNames(trackId),
                "업로드 목록을 조회했습니다."));
    }

    /** POST /api/v1/admin/satisfaction/upload — 엑셀 파일 업로드 */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @RequestParam("trackId") Long trackId,
            @RequestParam("uploadName") String uploadName,
            @RequestParam("file") MultipartFile file) {

        if (uploadName == null || uploadName.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("업로드 이름을 입력해주세요."));
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("파일이 비어있습니다."));
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(".xlsx 파일만 업로드 가능합니다."));
        }

        int count = satisfactionService.upload(trackId, uploadName, file);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("count", count, "trackId", trackId, "uploadName", uploadName),
                count + "건의 만족도 데이터가 업로드되었습니다."));
    }

    /** PATCH /api/v1/admin/satisfaction/upload-name — 업로드 이름 변경 */
    @PatchMapping("/upload-name")
    public ResponseEntity<ApiResponse<Void>> renameUpload(
            @RequestParam Long trackId,
            @RequestParam String oldName,
            @RequestParam String newName) {

        if (newName == null || newName.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("새 이름을 입력해주세요."));
        }
        satisfactionService.renameUpload(trackId, oldName, newName);
        return ResponseEntity.ok(ApiResponse.success(null, "업로드 이름이 변경되었습니다."));
    }

    /** DELETE /api/v1/admin/satisfaction/upload — 업로드 데이터 삭제 */
    @DeleteMapping("/upload")
    public ResponseEntity<ApiResponse<Void>> deleteUpload(
            @RequestParam Long trackId,
            @RequestParam String uploadName) {

        satisfactionService.deleteUpload(trackId, uploadName);
        return ResponseEntity.ok(ApiResponse.success(null, "업로드 데이터가 삭제되었습니다."));
    }

    /** GET /api/v1/admin/satisfaction/analysis?trackId=1&uploadName=3주차 — 분석 결과 조회 */
    @GetMapping("/analysis")
    public ResponseEntity<ApiResponse<SatisfactionAnalysisResponse>> analysis(
            @RequestParam Long trackId,
            @RequestParam String uploadName) {
        return ResponseEntity.ok(ApiResponse.success(
                satisfactionService.analyze(trackId, uploadName),
                "만족도 분석 결과를 조회했습니다."));
    }

    /** POST /api/v1/admin/satisfaction/ai-report?trackId=1&uploadName=3주차 — Gemini AI 분석 리포트 */
    @PostMapping("/ai-report")
    public ResponseEntity<ApiResponse<SatisfactionAiReportResponse>> aiReport(
            @RequestParam Long trackId,
            @RequestParam String uploadName) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    satisfactionService.buildAiReport(trackId, uploadName),
                    "AI 분석 리포트가 생성되었습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (RuntimeException e) {
            log.error("[Satisfaction AI] 리포트 생성 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("AI 분석 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
