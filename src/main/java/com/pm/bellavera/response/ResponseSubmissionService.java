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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
        List<String> errors = new ArrayList<>(duplicateCodeErrors(incoming));

        Map<String, AnswerRequest> incomingByCode = new LinkedHashMap<>();
        for (AnswerRequest answerRequest : incoming) {
            Question question = questionsByCode.get(answerRequest.questionCode());
            if (question == null) {
                errors.add("Unknown question code: " + answerRequest.questionCode());
                continue;
            }
            errors.addAll(answerValidator.validate(question, answerRequest,
                    optionsByQuestionId.getOrDefault(question.getId(), List.of())));
            incomingByCode.putIfAbsent(answerRequest.questionCode(), answerRequest);
        }

        Map<String, Answer> existingAnswersByCode = response.getId() != null
                ? answerRepository.findBySurveyResponseIdOrderByQuestionCode(response.getId()).stream()
                        .collect(Collectors.toMap(Answer::getQuestionCode, a -> a))
                : Map.of();

        if (request.status() == ResponseStatus.SUBMITTED) {
            errors.addAll(missingRequiredAnswers(questions, effectiveAnswers(existingAnswersByCode, incomingByCode)));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        surveyResponseRepository.save(response);

        for (AnswerRequest answerRequest : incomingByCode.values()) {
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

    /**
     * The same question twice in one request. The write loop would build a second {@code Answer}
     * for it and trip the {@code (survey_response_id, question_id)} unique index - a 500 for what
     * is plainly a bad request. Rejecting it also avoids having to invent which of the two wins.
     */
    private List<String> duplicateCodeErrors(List<AnswerRequest> incoming) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicated = new java.util.LinkedHashSet<>();
        for (AnswerRequest answer : incoming) {
            if (!seen.add(answer.questionCode())) {
                duplicated.add(answer.questionCode());
            }
        }
        return duplicated.stream().map(code -> code + ": answered more than once in one request").toList();
    }

    /**
     * What this response says after applying the request - saved answers, overlaid with the ones
     * just sent.
     *
     * <p>Required-ness has to be judged against this and not against the request alone. A survey is
     * answered over several saves, so a request carrying only the last page's answers says nothing
     * about the earlier ones; evaluating a display rule against it made every conditional question
     * look hidden, and a hidden question's required check is skipped. Conditional questions could
     * therefore be left unanswered by submitting a response one page at a time.
     */
    private Map<String, AnswerRequest> effectiveAnswers(Map<String, Answer> stored,
                                                         Map<String, AnswerRequest> incoming) {
        Map<String, AnswerRequest> effective = new HashMap<>();
        stored.forEach((code, answer) -> effective.put(code, toAnswerRequest(answer)));
        effective.putAll(incoming);
        return effective;
    }

    private List<String> missingRequiredAnswers(List<Question> questions, Map<String, AnswerRequest> answers) {
        List<String> errors = new ArrayList<>();
        for (Question question : questions) {
            if (!question.isRequired()) {
                continue;
            }
            if (!displayRuleEvaluator.isVisible(question.getDisplayRule(), answers)) {
                continue;
            }
            if (!hasValue(answers.get(question.getCode()))) {
                errors.add(question.getCode() + ": required");
            }
        }
        return errors;
    }

    private AnswerRequest toAnswerRequest(Answer answer) {
        return new AnswerRequest(
                answer.getQuestionCode(),
                answer.getValueText(),
                answer.getValueNumber(),
                answer.getValueBoolean(),
                answer.getValueDate(),
                answer.getSelectedOptions().stream().map(QuestionOption::getCode).toList());
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
