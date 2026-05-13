package com.bootcamp.flowcheck.domain.satisfaction.repository;

import com.bootcamp.flowcheck.domain.satisfaction.entity.SatisfactionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SatisfactionResultRepository extends JpaRepository<SatisfactionResult, Long> {

    List<SatisfactionResult> findAllByTrack_IdAndUploadName(Long trackId, String uploadName);

    @Query("SELECT DISTINCT s.uploadName FROM SatisfactionResult s " +
           "WHERE s.track.id = :trackId AND s.uploadName IS NOT NULL AND s.uploadName <> '' " +
           "ORDER BY s.uploadName ASC")
    List<String> findDistinctUploadNamesByTrackId(@Param("trackId") Long trackId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SatisfactionResult s WHERE s.track.id = :trackId AND s.uploadName = :uploadName")
    void deleteAllByTrackIdAndUploadName(@Param("trackId") Long trackId, @Param("uploadName") String uploadName);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SatisfactionResult s SET s.uploadName = :newName " +
           "WHERE s.track.id = :trackId AND s.uploadName = :oldName")
    int renameUploadName(@Param("trackId") Long trackId,
                         @Param("oldName") String oldName,
                         @Param("newName") String newName);
}
