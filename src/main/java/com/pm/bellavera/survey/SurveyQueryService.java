package com.pm.bellavera.survey;

import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.response.SurveyResponseRepository;
import com.pm.bellavera.survey.api.QuestionDto;
import com.pm.bellavera.survey.api.QuestionOptionDto;
import com.pm.bellavera.survey.api.SectionDto;
import com.pm.bellavera.survey.api.SurveyDetailDto;
import com.pm.bellavera.survey.api.SurveySummaryDto;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SurveyQueryService {

    private final SurveyRepository surveyRepository;
    private final SurveyVersionRepository surveyVersionRepository;
    private final SurveySectionRepository surveySectionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final SurveyResponseRepository surveyResponseRepository;

    public SurveyQueryService(SurveyRepository surveyRepository,
                               SurveyVersionRepository surveyVersionRepository,
                               SurveySectionRepository surveySectionRepository,
                               QuestionRepository questionRepository,
                               QuestionOptionRepository questionOptionRepository,
                               SurveyResponseRepository surveyResponseRepository) {
        this.surveyRepository = surveyRepository;
        this.surveyVersionRepository = surveyVersionRepository;
        this.surveySectionRepository = surveySectionRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.surveyResponseRepository = surveyResponseRepository;
    }

    @Transactional(readOnly = true)
    public List<SurveySummaryDto> listActiveForUser(UUID userId) {
        return surveyRepository.findByActiveTrueOrderBySortOrder().stream()
                .map(survey -> toSummary(survey, userId))
                .toList();
    }

    private SurveySummaryDto toSummary(Survey survey, UUID userId) {
        var publishedVersion = surveyVersionRepository.findBySurveyIdAndStatus(survey.getId(), SurveyVersionStatus.PUBLISHED);
        boolean completed = publishedVersion.isPresent()
                && !surveyResponseRepository.findSubmittedForUserAndVersion(userId, publishedVersion.get().getId()).isEmpty();
        return new SurveySummaryDto(
                survey.getId(),
                survey.getCode(),
                survey.getTheme(),
                survey.getTitle(),
                survey.getDescription(),
                publishedVersion.map(SurveyVersion::getId).orElse(null),
                completed);
    }

    @Transactional(readOnly = true)
    public SurveyDetailDto getPublishedDetail(UUID surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new NotFoundException("Survey not found"));
        SurveyVersion version = surveyVersionRepository.findBySurveyIdAndStatus(surveyId, SurveyVersionStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Survey has no published version"));

        List<SurveySection> sections = surveySectionRepository.findBySurveyVersionIdOrderBySortOrder(version.getId());
        List<Question> questions = questionRepository.findBySurveyVersionIdOrderBySortOrder(version.getId());
        Map<UUID, List<QuestionOption>> optionsByQuestionId = questionOptionRepository
                .findByQuestionIdIn(questions.stream().map(Question::getId).toList()).stream()
                .collect(Collectors.groupingBy(o -> o.getQuestion().getId()));

        Map<UUID, List<Question>> questionsBySectionId = questions.stream()
                .filter(q -> q.getSection() != null)
                .collect(Collectors.groupingBy(q -> q.getSection().getId()));
        List<Question> unsectioned = questions.stream().filter(q -> q.getSection() == null).toList();

        List<SectionDto> sectionDtos = sections.stream()
                .map(section -> toSectionDto(section.getCode(), section.getTitle(), section.getDescription(),
                        questionsBySectionId.getOrDefault(section.getId(), List.of()), optionsByQuestionId))
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        if (!unsectioned.isEmpty()) {
            sectionDtos.add(toSectionDto(null, null, null, unsectioned, optionsByQuestionId));
        }

        return new SurveyDetailDto(survey.getId(), version.getId(), version.getVersion(), survey.getCode(),
                survey.getTheme(), survey.getTitle(), survey.getDescription(), sectionDtos);
    }

    private SectionDto toSectionDto(String code, String title, String description, List<Question> questions,
                                     Map<UUID, List<QuestionOption>> optionsByQuestionId) {
        List<QuestionDto> questionDtos = questions.stream()
                .sorted(Comparator.comparingInt(Question::getSortOrder))
                .map(q -> new QuestionDto(
                        q.getCode(), q.getType(), q.getPrompt(), q.getHelpText(), q.isRequired(), q.getSortOrder(),
                        q.getConfig(), q.getDisplayRule(),
                        optionsByQuestionId.getOrDefault(q.getId(), List.of()).stream()
                                .sorted(Comparator.comparingInt(QuestionOption::getSortOrder))
                                .map(o -> new QuestionOptionDto(o.getCode(), o.getLabel(), o.getSortOrder()))
                                .toList()))
                .toList();
        return new SectionDto(code, title, description, questionDtos);
    }
}
