package com.bootcamp.flowcheck.domain.nps.controller;

import com.bootcamp.flowcheck.domain.nps.dto.NpsAnalysisResponse;
import com.bootcamp.flowcheck.domain.nps.service.NpsService;
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
@RequestMapping("/api/v1/admin/nps")
@RequiredArgsConstructor
public class NpsController {

    private final NpsService npsService;

    /** GET /api/v1/admin/nps/uploads?trackId=1 — 업로드 이름 목록 */
    @GetMapping("/uploads")
    public ResponseEntity<ApiResponse<List<String>>> uploads(@RequestParam Long trackId) {
        return ResponseEntity.ok(ApiResponse.success(
                npsService.getUploadNames(trackId),
                "업로드 목록을 조회했습니다."));
    }

    /** POST /api/v1/admin/nps/upload — 엑셀 파일 업로드 */
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

        int count = npsService.upload(trackId, uploadName, file);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("count", count, "trackId", trackId, "uploadName", uploadName),
                count + "건의 NPS 데이터가 업로드되었습니다."));
    }

    /** PATCH /api/v1/admin/nps/upload-name — 업로드 이름 변경 */
    @PatchMapping("/upload-name")
    public ResponseEntity<ApiResponse<Void>> renameUpload(
            @RequestParam Long trackId,
            @RequestParam String oldName,
            @RequestParam String newName) {

        if (newName == null || newName.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("새 이름을 입력해주세요."));
        }
        npsService.renameUpload(trackId, oldName, newName);
        return ResponseEntity.ok(ApiResponse.success(null, "업로드 이름이 변경되었습니다."));
    }

    /** DELETE /api/v1/admin/nps/upload — 업로드 데이터 삭제 */
    @DeleteMapping("/upload")
    public ResponseEntity<ApiResponse<Void>> deleteUpload(
            @RequestParam Long trackId,
            @RequestParam String uploadName) {

        npsService.deleteUpload(trackId, uploadName);
        return ResponseEntity.ok(ApiResponse.success(null, "업로드 데이터가 삭제되었습니다."));
    }

    /** GET /api/v1/admin/nps/analysis?trackId=1&uploadName=1차 — 분석 결과 조회 */
    @GetMapping("/analysis")
    public ResponseEntity<ApiResponse<NpsAnalysisResponse>> analysis(
            @RequestParam Long trackId,
            @RequestParam String uploadName) {
        return ResponseEntity.ok(ApiResponse.success(
                npsService.analyze(trackId, uploadName),
                "NPS 분석 결과를 조회했습니다."));
    }
}
