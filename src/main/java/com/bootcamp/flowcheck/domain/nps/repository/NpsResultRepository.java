package com.bootcamp.flowcheck.domain.nps.repository;

import com.bootcamp.flowcheck.domain.nps.entity.NpsResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NpsResultRepository extends JpaRepository<NpsResult, Long> {

    List<NpsResult> findAllByTrack_IdAndUploadNameOrderByNameAsc(Long trackId, String uploadName);

    boolean existsByTrack_Id(Long trackId);

    @Query("SELECT DISTINCT n.uploadName FROM NpsResult n WHERE n.track.id = :trackId AND n.uploadName IS NOT NULL AND n.uploadName <> '' ORDER BY n.uploadName ASC")
    List<String> findDistinctUploadNamesByTrackId(@Param("trackId") Long trackId);

    @Modifying
    @Query("DELETE FROM NpsResult n WHERE n.track.id = :trackId AND n.uploadName = :uploadName")
    void deleteAllByTrackIdAndUploadName(@Param("trackId") Long trackId, @Param("uploadName") String uploadName);

    @Modifying
    @Query("UPDATE NpsResult n SET n.uploadName = :newName WHERE n.track.id = :trackId AND n.uploadName = :oldName")
    int renameUploadName(@Param("trackId") Long trackId, @Param("oldName") String oldName, @Param("newName") String newName);
}
