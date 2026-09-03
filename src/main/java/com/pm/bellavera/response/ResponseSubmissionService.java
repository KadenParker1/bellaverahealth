package com.pm.bellavera.response;

import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.common.ValidationException;
import com.pm.bellavera.response.api.AnswerDto;
import com.pm.bellavera.response.api.AnswerRequest;
import com.pm.bellavera.response.api.SubmitResponseRequest;
import com.pm.bellavera.response.api.SurveyResponseDetailDto;
import com.pm.bellavera.survey.Question;
import com.pm.bellavera.survey.QuestionOption;
import com.pm.bellavera.survey.QuestionOptionRepository;
import com.pm.bellavera.survey.QuestionRepository;
import com.pm.bellavera.survey.Survey;
import com.pm.bellavera.survey.SurveyRepository;
import com.pm.bellavera.survey.SurveyVersion;
import com.pm.bellavera.survey.SurveyVersionRepository;
import com.pm.bellavera.survey.SurveyVersionStatus;
import com.pm.bellavera.user.AppUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResponseSubmissionService {

    private final SurveyRepository surveyRepository;
    private final SurveyVersionRepository surveyVersionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final AnswerRepository answerRepository;
    private final AnswerValidator answerValidator;
    private final DisplayRuleEvaluator displayRuleEvaluator;
    private final ApplicationEventPublisher eventPublisher;

    public ResponseSubmissionService(SurveyRepository surveyRepository,
                                      SurveyVersionRepository surveyVersionRepository,
                                      QuestionRepository questionRepository,
                                      QuestionOptionRepository questionOptionRepository,
                                      SurveyResponseRepository surveyResponseRepository,
                                      AnswerRepository answerRepository,
                                      AnswerValidator answerValidator,
                                      DisplayRuleEvaluator displayRuleEvaluator,
                                      ApplicationEventPublisher eventPublisher) {
        this.surveyRepository = surveyRepository;
        this.surveyVersionRepository = surveyVersionRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.surveyResponseRepository = surveyResponseRepository;
        this.answerRepository = answerRepository;
        this.answerValidator = answerValidator;
        this.displayRuleEvaluator = displayRuleEvaluator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SurveyResponseDetailDto upsert(AppUser user, UUID surveyId, SubmitResponseRequest request) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new NotFoundException("Survey not found"));
        SurveyVersion version = surveyVersionRepository.findBySurveyIdAndStatus(surveyId, SurveyVersionStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Survey has no published version"));

        List<Question> questions = questionRepository.findBySurveyVersionIdOrderBySortOrder(version.getId());
        Map<String, Question> questionsByCode = questions.stream()
                .collect(Collectors.toMap(Question::getCode, q -> q));
        Map<UUID, List<QuestionOption>> optionsByQuestionId = questionOptionRepository
                .findByQuestionIdIn(questions.stream().map(Question::getId).toList()).stream()
                .collect(Collectors.groupingBy(o -> o.getQuestion().getId()));

        SurveyResponse response = surveyResponseRepository
                .findByUserIdAndSurveyVersionIdAndStatus(user.getId(), version.getId(), ResponseStatus.IN_PROGRESS)
                .orElseGet(() -> SurveyResponse.builder()
                        .user(user)
                        .surveyVersion(version)
                        .status(ResponseStatus.IN_PROGRESS)
                        .startedAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build());

        List<AnswerRequest> incoming = request.answers() != null ? request.answers() : List.of();
        Map<String, AnswerRequest> incomingByCode = incoming.stream()
                .collect(Collectors.toMap(AnswerRequest::questionCode, a -> a, (a, b) -> a));

        List<String> errors = new ArrayList<>();
        for (AnswerRequest answerRequest : incoming) {
            Question question = questionsByCode.get(answerRequest.questionCode());
            if (question == null) {
                errors.add("Unknown question code: " + answerRequest.questionCode());
                continue;
            }
            errors.addAll(answerValidator.validate(question, answerRequest,
                    optionsByQuestionId.getOrDefault(question.getId(), List.of())));
        }

        Map<String, Answer> existingAnswersByCode = response.getId() != null
                ? answerRepository.findBySurveyResponseIdOrderByQuestionCode(response.getId()).stream()
                        .collect(Collectors.toMap(Answer::getQuestionCode, a -> a))
                : Map.of();

        if (request.status() == ResponseStatus.SUBMITTED) {
            for (Question question : questions) {
                if (!question.isRequired()) {
                    continue;
                }
                if (!displayRuleEvaluator.isVisible(question.getDisplayRule(), incomingByCode)) {
                    continue;
                }
                boolean answeredNow = hasValue(incomingByCode.get(question.getCode()));
                boolean answeredBefore = existingAnswersByCode.containsKey(question.getCode());
                if (!answeredNow && !answeredBefore) {
                    errors.add(question.getCode() + ": required");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        surveyResponseRepository.save(response);

        for (AnswerRequest answerRequest : incoming) {
            Question question = questionsByCode.get(answerRequest.questionCode());
            Answer answer = existingAnswersByCode.getOrDefault(question.getCode(), Answer.builder()
                    .surveyResponse(response)
                    .question(question)
                    .questionCode(question.getCode())
                    .build());
            answer.setValueText(answerRequest.valueText());
            answer.setValueNumber(answerRequest.valueNumber());
            answer.setValueBoolean(answerRequest.valueBoolean());
            answer.setValueDate(answerRequest.valueDate());
            answer.setUpdatedAt(Instant.now());
            if (answerRequest.optionCodes() != null) {
                Set<QuestionOption> selected = optionsByQuestionId.getOrDefault(question.getId(), List.of()).stream()
                        .filter(o -> answerRequest.optionCodes().contains(o.getCode()))
                        .collect(Collectors.toSet());
                answer.setSelectedOptions(selected);
            }
            answerRepository.save(answer);
        }

        response.setUpdatedAt(Instant.now());
        if (request.status() == ResponseStatus.SUBMITTED) {
            response.setStatus(ResponseStatus.SUBMITTED);
            response.setSubmittedAt(Instant.now());
        }
        surveyResponseRepository.save(response);

        if (request.status() == ResponseStatus.SUBMITTED) {
            eventPublisher.publishEvent(new SurveyResponseSubmittedEvent(user.getId(), response.getId(), surveyId, survey.getCode()));
        }

        return toDetailDto(response);
    }

    @Transactional(readOnly = true)
    public SurveyResponseDetailDto getMine(AppUser user, UUID surveyId) {
        surveyRepository.findById(surveyId).orElseThrow(() -> new NotFoundException("Survey not found"));
        SurveyVersion version = surveyVersionRepository.findBySurveyIdAndStatus(surveyId, SurveyVersionStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Survey has no published version"));

        SurveyResponse response = surveyResponseRepository
                .findByUserIdAndSurveyVersionIdAndStatus(user.getId(), version.getId(), ResponseStatus.SUBMITTED)
                .or(() -> surveyResponseRepository
                        .findByUserIdAndSurveyVersionIdAndStatus(user.getId(), version.getId(), ResponseStatus.IN_PROGRESS))
                .orElseThrow(() -> new NotFoundException("No response found for this survey"));

        return toDetailDto(response);
    }

    private SurveyResponseDetailDto toDetailDto(SurveyResponse response) {
        List<AnswerDto> answers = answerRepository.findBySurveyResponseIdOrderByQuestionCode(response.getId()).stream()
                .map(a -> new AnswerDto(
                        a.getQuestionCode(),
                        a.getValueText(),
                        a.getValueNumber(),
                        a.getValueBoolean(),
                        a.getValueDate(),
                        a.getSelectedOptions().stream().map(QuestionOption::getCode)
                                .sorted(Comparator.naturalOrder()).toList()))
                .toList();
        return new SurveyResponseDetailDto(response.getId(), response.getStatus(), response.getStartedAt(),
                response.getSubmittedAt(), answers);
    }

    private boolean hasValue(AnswerRequest answer) {
        if (answer == null) {
            return false;
        }
        return (answer.optionCodes() != null && !answer.optionCodes().isEmpty())
                || answer.valueText() != null
                || answer.valueNumber() != null
                || answer.valueBoolean() != null
                || answer.valueDate() != null;
    }
}
