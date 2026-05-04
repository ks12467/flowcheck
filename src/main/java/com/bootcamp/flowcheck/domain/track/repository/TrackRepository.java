package com.bootcamp.flowcheck.domain.track.repository;

import com.bootcamp.flowcheck.domain.track.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrackRepository extends JpaRepository<Track, Long> {

    @Query("SELECT t FROM Track t JOIN PmTrack pt ON pt.track = t WHERE pt.pm.id = :pmId")
    List<Track> findAllByPmId(@Param("pmId") Long pmId);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.track.id = :trackId")
    long countStudentsByTrackId(@Param("trackId") Long trackId);
}
