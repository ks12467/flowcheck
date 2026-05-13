package com.bootcamp.flowcheck.domain.survey.service;

import com.bootcamp.flowcheck.domain.survey.dto.SurveyQuestionResponse;
import com.bootcamp.flowcheck.domain.survey.dto.SurveyQuestionsUpdateRequest;
import com.bootcamp.flowcheck.domain.survey.entity.SurveyQuestion;
import com.bootcamp.flowcheck.domain.survey.repository.SurveyQuestionRepository;
import com.bootcamp.flowcheck.domain.track.entity.Track;
import com.bootcamp.flowcheck.domain.track.repository.TrackRepository;
import com.bootcamp.flowcheck.global.exception.BusinessException;
import com.bootcamp.flowcheck.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyQuestionService {

    private final SurveyQuestionRepository surveyQuestionRepository;
    private final TrackRepository trackRepository;

    @Transactional
    public List<SurveyQuestionResponse> getQuestions(Long trackId) {
        if (!surveyQuestionRepository.existsByTrack_Id(trackId)) {
            initDefaultQuestions(trackId);
        }
        return surveyQuestionRepository.findAllByTrack_IdOrderByQuestionOrderAsc(trackId)
                .stream().map(SurveyQuestionResponse::of).toList();
    }

    @Transactional
    public List<SurveyQuestionResponse> updateQuestions(Long trackId,
                                                        SurveyQuestionsUpdateRequest request) {
        for (SurveyQuestionsUpdateRequest.QuestionUpdate upd : request.getQuestions()) {
            surveyQuestionRepository.findById(upd.getId())
                    .filter(q -> q.getTrack().getId().equals(trackId))
                    .ifPresent(q -> q.updateText(upd.getQuestionText()));
        }
        return surveyQuestionRepository.findAllByTrack_IdOrderByQuestionOrderAsc(trackId)
                .stream().map(SurveyQuestionResponse::of).toList();
    }

    private void initDefaultQuestions(Long trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        surveyQuestionRepository.saveAll(List.of(
                SurveyQuestion.builder().track(track).questionOrder(1)
                        .questionText("오늘 학습 난이도는 어땠나요?")
                        .questionType(SurveyQuestion.QuestionType.SCORE)
                        .minScore(1).maxScore(5).createdAt(now).build(),
                SurveyQuestion.builder().track(track).questionOrder(2)
                        .questionText("오늘 학습 이해도는 어땠나요?")
                        .questionType(SurveyQuestion.QuestionType.SCORE)
                        .minScore(1).maxScore(5).createdAt(now).build(),
                SurveyQuestion.builder().track(track).questionOrder(3)
                        .questionText("오늘 학습 몰입도는 어땠나요?")
                        .questionType(SurveyQuestion.QuestionType.SCORE)
                        .minScore(1).maxScore(5).createdAt(now).build(),
                SurveyQuestion.builder().track(track).questionOrder(4)
                        .questionText("오늘 학습에서 어려웠던 점이나 코멘트를 남겨주세요.")
                        .questionType(SurveyQuestion.QuestionType.TEXT)
                        .createdAt(now).build()
        ));
    }
}
