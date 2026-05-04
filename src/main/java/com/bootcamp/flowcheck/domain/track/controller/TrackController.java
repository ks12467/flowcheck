package com.bootcamp.flowcheck.domain.track.controller;

import com.bootcamp.flowcheck.domain.track.dto.*;
import com.bootcamp.flowcheck.domain.track.service.TrackService;
import com.bootcamp.flowcheck.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tracks")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TrackResponse>>> getMyTracks() {
        return ResponseEntity.ok(ApiResponse.success(trackService.getMyTracks(), "트랙 목록을 조회했습니다."));
    }

    @GetMapping("/{trackId}")
    public ResponseEntity<ApiResponse<TrackDetailResponse>> getTrackDetail(@PathVariable Long trackId) {
        return ResponseEntity.ok(ApiResponse.success(trackService.getTrackDetail(trackId), "트랙을 조회했습니다."));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TrackResponse>> createTrack(@RequestBody @Valid TrackCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(trackService.createTrack(request), "트랙을 생성했습니다."));
    }

    @PutMapping("/{trackId}")
    public ResponseEntity<ApiResponse<TrackResponse>> updateTrack(
            @PathVariable Long trackId,
            @RequestBody @Valid TrackUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(trackService.updateTrack(trackId, request), "트랙을 수정했습니다."));
    }

    @PostMapping("/{trackId}/join")
    public ResponseEntity<ApiResponse<Void>> joinTrack(@PathVariable Long trackId) {
        trackService.joinTrack(trackId);
        return ResponseEntity.ok(ApiResponse.success(null, "트랙에 참여했습니다."));
    }

    @DeleteMapping("/{trackId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveTrack(@PathVariable Long trackId) {
        trackService.leaveTrack(trackId);
        return ResponseEntity.ok(ApiResponse.success(null, "트랙에서 탈퇴했습니다."));
    }
}
