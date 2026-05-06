package com.bootcamp.flowcheck.domain.track.service;

import com.bootcamp.flowcheck.domain.auth.entity.Pm;
import com.bootcamp.flowcheck.domain.auth.repository.PmRepository;
import com.bootcamp.flowcheck.domain.track.dto.*;
import com.bootcamp.flowcheck.domain.track.entity.PmTrack;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackService {

    private final TrackRepository trackRepository;
    private final PmTrackRepository pmTrackRepository;
    private final PmRepository pmRepository;

    public List<TrackResponse> getMyTracks() {
        Long pmId = getCurrentPmId();
        return trackRepository.findAllByPmId(pmId).stream()
                .map(t -> TrackResponse.of(t, trackRepository.countStudentsByTrackId(t.getId())))
                .toList();
    }

    public TrackDetailResponse getTrackDetail(Long trackId) {
        Long pmId = getCurrentPmId();
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        validateAccess(pmId, trackId);

        long studentCount = trackRepository.countStudentsByTrackId(trackId);
        List<PmResponse> pms = pmTrackRepository.findAllByTrackIdWithPm(trackId).stream()
                .map(pt -> PmResponse.of(pt.getPm()))
                .toList();

        return TrackDetailResponse.of(track, studentCount, pms);
    }

    @Transactional
    public TrackResponse createTrack(TrackCreateRequest request) {
        Long pmId = getCurrentPmId();
        Pm pm = pmRepository.findById(pmId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        Track track = Track.builder()
                .name(request.buildName())
                .courseType(request.getCourseType())
                .generation(request.getGeneration())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdAt(LocalDateTime.now())
                .build();
        trackRepository.save(track);

        PmTrack pmTrack = PmTrack.builder()
                .pm(pm)
                .track(track)
                .role("MANAGER")
                .build();
        pmTrackRepository.save(pmTrack);

        return TrackResponse.of(track, 0);
    }

    @Transactional
    public TrackResponse updateTrack(Long trackId, TrackUpdateRequest request) {
        Long pmId = getCurrentPmId();
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        validateAccess(pmId, trackId);

        track.update(request.buildName(), request.getCourseType(), request.getGeneration(),
                request.getStartDate(), request.getEndDate());
        long studentCount = trackRepository.countStudentsByTrackId(trackId);
        return TrackResponse.of(track, studentCount);
    }

    @Transactional
    public void joinTrack(Long trackId) {
        Long pmId = getCurrentPmId();
        trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        if (pmTrackRepository.existsByPm_IdAndTrack_Id(pmId, trackId)) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }

        Pm pm = pmRepository.findById(pmId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        Track track = trackRepository.getReferenceById(trackId);

        pmTrackRepository.save(PmTrack.builder()
                .pm(pm)
                .track(track)
                .role("MANAGER")
                .build());
    }

    @Transactional
    public void deleteTrack(Long trackId) {
        Long pmId = getCurrentPmId();
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        validateAccess(pmId, trackId);
        track.softDelete();
    }

    @Transactional
    public void leaveTrack(Long trackId) {
        Long pmId = getCurrentPmId();
        trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        validateAccess(pmId, trackId);

        pmTrackRepository.deleteByPm_IdAndTrack_Id(pmId, trackId);
    }

    private Long getCurrentPmId() {
        return Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private void validateAccess(Long pmId, Long trackId) {
        if (!pmTrackRepository.existsByPm_IdAndTrack_Id(pmId, trackId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
