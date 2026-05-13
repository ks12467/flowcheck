package com.bootcamp.flowcheck.domain.survey.repository;

import com.bootcamp.flowcheck.domain.survey.entity.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {

    List<SurveyQuestion> findAllByTrack_IdOrderByQuestionOrderAsc(Long trackId);

    boolean existsByTrack_Id(Long trackId);
}
