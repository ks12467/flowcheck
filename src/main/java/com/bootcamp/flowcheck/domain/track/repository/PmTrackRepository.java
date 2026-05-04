package com.bootcamp.flowcheck.domain.track.repository;

import com.bootcamp.flowcheck.domain.track.entity.PmTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PmTrackRepository extends JpaRepository<PmTrack, Long> {

    boolean existsByPm_IdAndTrack_Id(Long pmId, Long trackId);

    @Query("SELECT pt FROM PmTrack pt JOIN FETCH pt.pm WHERE pt.track.id = :trackId")
    List<PmTrack> findAllByTrackIdWithPm(@Param("trackId") Long trackId);

    void deleteByPm_IdAndTrack_Id(Long pmId, Long trackId);

    @Query("SELECT pt FROM PmTrack pt JOIN FETCH pt.track WHERE pt.pm.id = :pmId")
    List<PmTrack> findAllByPmIdWithTrack(@Param("pmId") Long pmId);
}
