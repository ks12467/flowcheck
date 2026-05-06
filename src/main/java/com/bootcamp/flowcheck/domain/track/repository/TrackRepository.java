package com.bootcamp.flowcheck.domain.track.repository;

import com.bootcamp.flowcheck.domain.track.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {

    @Query("SELECT t FROM Track t JOIN PmTrack pt ON pt.track = t WHERE pt.pm.id = :pmId")
    List<Track> findAllByPmId(@Param("pmId") Long pmId);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.track.id = :trackId")
    long countStudentsByTrackId(@Param("trackId") Long trackId);

    @Query(value = "SELECT * FROM TRACK WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", nativeQuery = true)
    List<Track> findAllDeleted();

    @Query(value = "SELECT * FROM TRACK WHERE id = :id AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<Track> findDeletedById(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE TRACK SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restoreById(@Param("id") Long id);

    @Modifying
    @Query(value = "DELETE FROM TRACK WHERE id = :id", nativeQuery = true)
    void permanentDeleteById(@Param("id") Long id);
}
