package com.pm.bellavera.admin;

import com.pm.bellavera.admin.api.AdminOptionDto;
import com.pm.bellavera.admin.api.AdminQuestionDto;
import com.pm.bellavera.admin.api.AdminSectionDto;
import com.pm.bellavera.admin.api.AdminSurveyDto;
import com.pm.bellavera.admin.api.AdminSurveyVersionDto;
import com.pm.bellavera.admin.api.AdminVersionSummaryDto;
import com.pm.bellavera.admin.api.CreateSurveyRequest;
import com.pm.bellavera.admin.api.SaveVersionContentRequest;
import com.pm.bellavera.admin.api.UpdateSurveyRequest;
import com.pm.bellavera.audit.AuditService;
import com.pm.bellavera.common.ConflictException;
import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.common.ValidationException;
import com.pm.bellavera.survey.Question;
import com.pm.bellavera.survey.QuestionOption;
import com.pm.bellavera.survey.QuestionOptionRepository;
import com.pm.bellavera.survey.QuestionRepository;
import com.pm.bellavera.survey.Survey;
import com.pm.bellavera.survey.SurveyRepository;
import com.pm.bellavera.survey.SurveySection;
import com.pm.bellavera.survey.SurveySectionRepository;
import com.pm.bellavera.survey.SurveyVersion;
import com.pm.bellavera.survey.SurveyVersionRepository;
import com.pm.bellavera.survey.SurveyVersionStatus;
import com.pm.bellavera.user.AppUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Survey authoring. This is the tooling, not the content: it can build any survey the renderer can
 * display, and ships with none of its own.
 *
 * <p>Two invariants shape every method here:
 * <ul>
 *   <li><strong>Published versions are immutable.</strong> Editing a live survey means cloning it
 *       to a new draft, editing that, and publishing - which archives the version it replaced.
 *       Responses point at a {@code survey_version_id}, so a published version that changed under
 *       them would silently rewrite what people were asked.</li>
 *   <li><strong>Removal is deactivation.</strong> {@link #update} can flip a survey inactive so it
 *       leaves {@code /surveys/active}; nothing here deletes a survey. Only an unpublished draft
 *       can be deleted, because nothing can have answered it.</li>
 * </ul>
 */
@Service
public class AdminSurveyService {

    static final String AUDIT_ENTITY_SURVEY = "survey";
    static final String AUDIT_ENTITY_VERSION = "survey_version";

    private final SurveyRepository surveyRepository;
    private final SurveyVersionRepository surveyVersionRepository;
    private final SurveySectionRepository surveySectionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final AuditService auditService;

    public AdminSurveyService(SurveyRepository surveyRepository,
                               SurveyVersionRepository surveyVersionRepository,
                               SurveySectionRepository surveySectionRepository,
                               QuestionRepository questionRepository,
                               QuestionOptionRepository questionOptionRepository,
                               AuditService auditService) {
        this.surveyRepository = surveyRepository;
        this.surveyVersionRepository = surveyVersionRepository;
        this.surveySectionRepository = surveySectionRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.auditService = auditService;
    }

    // ---------------------------------------------------------------- surveys

    @Transactional(readOnly = true)
    public List<AdminSurveyDto> list() {
        List<Survey> surveys = surveyRepository.findAllByOrderBySortOrderAscTitleAsc();
        if (surveys.isEmpty()) {
            return List.of();
        }

        List<UUID> surveyIds = surveys.stream().map(Survey::getId).toList();
        List<SurveyVersion> versions = surveyVersionRepository.findBySurveyIdInOrderByVersionDesc(surveyIds);
        Map<UUID, Integer> questionCounts = questionCounts(versions);

        Map<UUID, List<SurveyVersion>> versionsBySurvey = versions.stream()
                .collect(Collectors.groupingBy(v -> v.getSurvey().getId()));

        return surveys.stream()
                .map(survey -> toSurveyDto(survey, versionsBySurvey.getOrDefault(survey.getId(), List.of()), questionCounts))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminSurveyDto get(UUID surveyId) {
        Survey survey = findSurvey(surveyId);
        List<SurveyVersion> versions = surveyVersionRepository.findBySurveyIdOrderByVersionDesc(surveyId);
        return toSurveyDto(survey, versions, questionCounts(versions));
    }

    @Transactional
    public AdminSurveyDto create(AppUser admin, CreateSurveyRequest request) {
        if (surveyRepository.existsByCode(request.code())) {
            throw new ValidationException("A survey with code '" + request.code() + "' already exists");
        }

        Survey survey = surveyRepository.saveAndFlush(Survey.builder()
                .code(request.code())
                .theme(request.theme())
                .title(request.title())
                .description(request.description())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .active(true)
                .build());

        SurveyVersion firstDraft = surveyVersionRepository.saveAndFlush(SurveyVersion.builder()
                .survey(survey)
                .version(1)
                .status(SurveyVersionStatus.DRAFT)
                .build());

        auditService.record(admin, "SURVEY_CREATED", AUDIT_ENTITY_SURVEY, survey.getId(), Map.of(
                "code", survey.getCode(),
                "theme", survey.getTheme().name(),
                "title", survey.getTitle()));

        return toSurveyDto(survey, List.of(firstDraft), Map.of(firstDraft.getId(), 0));
    }

    @Transactional
    public AdminSurveyDto update(AppUser admin, UUID surveyId, UpdateSurveyRequest request) {
        Survey survey = findSurvey(surveyId);

        Map<String, Object> before = new HashMap<>();
        before.put("title", survey.getTitle());
        before.put("description", survey.getDescription());
        before.put("sortOrder", survey.getSortOrder());
        before.put("active", survey.isActive());

        if (request.title() != null) {
            survey.setTitle(request.title());
        }
        if (request.description() != null) {
            survey.setDescription(request.description());
        }
        if (request.sortOrder() != null) {
            survey.setSortOrder(request.sortOrder());
        }
        if (request.active() != null) {
            survey.setActive(request.active());
        }
        surveyRepository.saveAndFlush(survey);

        Map<String, Object> after = new HashMap<>();
        after.put("title", survey.getTitle());
        after.put("description", survey.getDescription());
        after.put("sortOrder", survey.getSortOrder());
        after.put("active", survey.isActive());
        auditService.record(admin, "SURVEY_UPDATED", AUDIT_ENTITY_SURVEY, survey.getId(), before, after);

        return get(surveyId);
    }

    // --------------------------------------------------------------- versions

    /**
     * Opens a new draft, cloned from the newest existing version so editing live content starts
     * from that content rather than a blank page. One draft at a time per survey: a second
     * concurrent draft would make "which one publishes next" a coin flip.
     */
    @Transactional
    public AdminSurveyVersionDto createDraft(AppUser admin, UUID surveyId) {
        Survey survey = findSurvey(surveyId);
        List<SurveyVersion> existing = surveyVersionRepository.findBySurveyIdOrderByVersionDesc(surveyId);

        existing.stream()
                .filter(v -> v.getStatus() == SurveyVersionStatus.DRAFT)
                .findFirst()
                .ifPresent(draft -> {
                    throw new ConflictException("Survey already has a draft (version " + draft.getVersion()
                            + "). Publish or delete it before starting another.");
                });

        int nextVersion = existing.stream().mapToInt(SurveyVersion::getVersion).max().orElse(0) + 1;
        SurveyVersion draft = surveyVersionRepository.saveAndFlush(SurveyVersion.builder()
                .survey(survey)
                .version(nextVersion)
                .status(SurveyVersionStatus.DRAFT)
                .build());

        existing.stream().findFirst().ifPresent(source -> copyContent(source, draft));

        auditService.record(admin, "SURVEY_DRAFT_CREATED", AUDIT_ENTITY_VERSION, draft.getId(), Map.of(
                "surveyId", surveyId.toString(),
                "version", nextVersion));

        return getVersion(surveyId, draft.getId());
    }

    @Transactional(readOnly = true)
    public AdminSurveyVersionDto getVersion(UUID surveyId, UUID versionId) {
        SurveyVersion version = findVersion(surveyId, versionId);

        List<SurveySection> sections = surveySectionRepository.findBySurveyVersionIdOrderBySortOrder(versionId);
        List<Question> questions = questionRepository.findBySurveyVersionIdOrderBySortOrder(versionId);
        Map<UUID, List<QuestionOption>> optionsByQuestion = questions.isEmpty()
                ? Map.of()
                : questionOptionRepository.findByQuestionIdIn(questions.stream().map(Question::getId).toList()).stream()
                        .collect(Collectors.groupingBy(o -> o.getQuestion().getId()));

        Map<UUID, List<Question>> questionsBySection = questions.stream()
                .filter(q -> q.getSection() != null)
                .collect(Collectors.groupingBy(q -> q.getSection().getId()));

        List<AdminSectionDto> sectionDtos = new ArrayList<>(sections.stream()
                .map(section -> new AdminSectionDto(
                        section.getCode(),
                        section.getTitle(),
                        section.getDescription(),
                        section.getSortOrder(),
                        toQuestionDtos(questionsBySection.getOrDefault(section.getId(), List.of()), optionsByQuestion)))
                .toList());

        List<Question> unsectioned = questions.stream().filter(q -> q.getSection() == null).toList();
        if (!unsectioned.isEmpty()) {
            sectionDtos.add(new AdminSectionDto("_unsectioned", "Ungrouped questions", null,
                    sectionDtos.size(), toQuestionDtos(unsectioned, optionsByQuestion)));
        }

        return new AdminSurveyVersionDto(surveyId, version.getId(), version.getVersion(), version.getStatus(),
                version.getPublishedAt(), version.getNotes(), sectionDtos);
    }

    /** Replaces a draft's content wholesale. Refuses any version that is not a draft. */
    @Transactional
    public AdminSurveyVersionDto saveVersionContent(AppUser admin, UUID surveyId, UUID versionId,
                                                     SaveVersionContentRequest request) {
        SurveyVersion version = findVersion(surveyId, versionId);
        requireDraft(version, "edited");

        List<AdminSectionDto> sections = request.sections() == null ? List.of() : request.sections();
        validateContent(sections);

        clearContent(versionId);
        writeContent(version, sections);

        version.setNotes(request.notes());
        surveyVersionRepository.saveAndFlush(version);

        auditService.record(admin, "SURVEY_DRAFT_SAVED", AUDIT_ENTITY_VERSION, versionId, Map.of(
                "surveyId", surveyId.toString(),
                "sectionCount", sections.size(),
                "questionCount", sections.stream().mapToInt(s -> s.questions() == null ? 0 : s.questions().size()).sum()));

        return getVersion(surveyId, versionId);
    }

    /**
     * Publishes a draft, archiving whatever version it replaces. The archive is flushed first:
     * a partial unique index allows exactly one published version per survey, so publishing before
     * standing the old one down would trip it.
     */
    @Transactional
    public AdminSurveyVersionDto publish(AppUser admin, UUID surveyId, UUID versionId) {
        SurveyVersion version = findVersion(surveyId, versionId);
        requireDraft(version, "published");

        if (questionRepository.countBySurveyVersionId(versionId) == 0) {
            throw new ValidationException("A version with no questions cannot be published");
        }

        surveyVersionRepository.findBySurveyIdAndStatus(surveyId, SurveyVersionStatus.PUBLISHED)
                .ifPresent(previous -> {
                    previous.setStatus(SurveyVersionStatus.ARCHIVED);
                    surveyVersionRepository.saveAndFlush(previous);
                });

        version.setStatus(SurveyVersionStatus.PUBLISHED);
        version.setPublishedAt(Instant.now());
        surveyVersionRepository.saveAndFlush(version);

        auditService.record(admin, "SURVEY_VERSION_PUBLISHED", AUDIT_ENTITY_VERSION, versionId, Map.of(
                "surveyId", surveyId.toString(),
                "version", version.getVersion()));

        return getVersion(surveyId, versionId);
    }

    /**
     * Deletes a draft. Only a draft: a published or archived version is what somebody's answers
     * point at, and deleting it would orphan them.
     */
    @Transactional
    public void deleteDraft(AppUser admin, UUID surveyId, UUID versionId) {
        SurveyVersion version = findVersion(surveyId, versionId);
        requireDraft(version, "deleted");

        clearContent(versionId);
        surveyVersionRepository.delete(version);

        auditService.record(admin, "SURVEY_DRAFT_DELETED", AUDIT_ENTITY_VERSION, versionId, Map.of(
                "surveyId", surveyId.toString(),
                "version", version.getVersion()));
    }

    // -------------------------------------------------------------- internals

    private void requireDraft(SurveyVersion version, String verb) {
        if (version.getStatus() != SurveyVersionStatus.DRAFT) {
            throw new ConflictException("Version " + version.getVersion() + " is "
                    + version.getStatus() + " and cannot be " + verb
                    + ". Published content is immutable - start a new draft instead.");
        }
    }

    /**
     * Checks the whole document at once so the editor gets every problem in one response rather
     * than one per save.
     */
    private void validateContent(List<AdminSectionDto> sections) {
        List<String> errors = new ArrayList<>();
        Map<String, Integer> sectionCodes = new LinkedHashMap<>();
        Map<String, Integer> questionCodes = new LinkedHashMap<>();

        for (AdminSectionDto section : sections) {
            if (sectionCodes.merge(section.code(), 1, Integer::sum) == 2) {
                errors.add("Duplicate section code: " + section.code());
            }
            for (AdminQuestionDto question : section.questions() == null ? List.<AdminQuestionDto>of() : section.questions()) {
                if (questionCodes.merge(question.code(), 1, Integer::sum) == 2) {
                    errors.add("Duplicate question code: " + question.code());
                }
                validateQuestion(question, errors);
            }
        }

        // Display rules are evaluated against answers keyed by question code; a rule naming a code
        // that is not in this version can never be satisfied, so the question would never appear.
        for (AdminSectionDto section : sections) {
            for (AdminQuestionDto question : section.questions() == null ? List.<AdminQuestionDto>of() : section.questions()) {
                validateDisplayRule(question, questionCodes.keySet(), errors);
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private void validateQuestion(AdminQuestionDto question, List<String> errors) {
        List<AdminOptionDto> options = question.options() == null ? List.of() : question.options();
        boolean isChoice = switch (question.type()) {
            case SINGLE_CHOICE, MULTI_CHOICE -> true;
            default -> false;
        };

        if (isChoice && options.isEmpty()) {
            errors.add(question.code() + ": a " + question.type() + " question needs at least one option");
        }
        if (!isChoice && !options.isEmpty()) {
            errors.add(question.code() + ": a " + question.type() + " question cannot have options");
        }

        Map<String, Integer> optionCodes = new LinkedHashMap<>();
        for (AdminOptionDto option : options) {
            if (optionCodes.merge(option.code(), 1, Integer::sum) == 2) {
                errors.add(question.code() + ": duplicate option code '" + option.code() + "'");
            }
        }

        // AnswerValidator range-checks SCALE and NUMBER answers against these keys, so an editor
        // that let a scale ship without them would produce a question nothing can bound.
        if (question.type() == com.pm.bellavera.survey.QuestionType.SCALE) {
            Map<String, Object> config = question.config() == null ? Map.of() : question.config();
            if (!(config.get("min") instanceof Number) || !(config.get("max") instanceof Number)) {
                errors.add(question.code() + ": a SCALE question needs numeric 'min' and 'max' in its config");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateDisplayRule(AdminQuestionDto question, java.util.Set<String> knownCodes, List<String> errors) {
        Map<String, Object> rule = question.displayRule();
        if (rule == null || rule.isEmpty()) {
            return;
        }
        if (!(rule.get("all") instanceof List<?> conditions)) {
            errors.add(question.code() + ": a display rule must be shaped {\"all\": [ ... ]}");
            return;
        }
        for (Object raw : conditions) {
            if (!(raw instanceof Map<?, ?> condition)) {
                errors.add(question.code() + ": display rule conditions must be objects");
                continue;
            }
            Map<String, Object> typed = (Map<String, Object>) condition;
            Object referenced = typed.get("questionCode");
            Object op = typed.get("op");
            if (referenced == null || !knownCodes.contains(String.valueOf(referenced))) {
                errors.add(question.code() + ": display rule references unknown question '" + referenced + "'");
            }
            if (referenced != null && String.valueOf(referenced).equals(question.code())) {
                errors.add(question.code() + ": a display rule cannot depend on its own question");
            }
            if (!List.of("eq", "ne", "in").contains(String.valueOf(op))) {
                errors.add(question.code() + ": unsupported display rule operator '" + op
                        + "' (DisplayRuleEvaluator understands eq, ne, in)");
            }
        }
    }

    private void clearContent(UUID versionId) {
        List<Question> questions = questionRepository.findBySurveyVersionIdOrderBySortOrder(versionId);
        if (!questions.isEmpty()) {
            questionOptionRepository.deleteByQuestionIdIn(questions.stream().map(Question::getId).toList());
        }
        questionRepository.deleteBySurveyVersionId(versionId);
        surveySectionRepository.deleteBySurveyVersionId(versionId);
        questionRepository.flush();
        surveySectionRepository.flush();
    }

    private void writeContent(SurveyVersion version, List<AdminSectionDto> sections) {
        int sectionOrder = 0;
        for (AdminSectionDto sectionDto : sections) {
            SurveySection section = surveySectionRepository.saveAndFlush(SurveySection.builder()
                    .surveyVersion(version)
                    .code(sectionDto.code())
                    .title(sectionDto.title())
                    .description(sectionDto.description())
                    .sortOrder(sectionDto.sortOrder() == 0 ? sectionOrder : sectionDto.sortOrder())
                    .build());
            sectionOrder++;

            int questionOrder = 0;
            for (AdminQuestionDto questionDto : sectionDto.questions() == null ? List.<AdminQuestionDto>of() : sectionDto.questions()) {
                Question question = questionRepository.saveAndFlush(Question.builder()
                        .surveyVersion(version)
                        .section(section)
                        .code(questionDto.code())
                        .type(questionDto.type())
                        .prompt(questionDto.prompt())
                        .helpText(questionDto.helpText())
                        .required(questionDto.required())
                        .sortOrder(questionDto.sortOrder() == 0 ? questionOrder : questionDto.sortOrder())
                        .config(questionDto.config() == null ? Map.of() : questionDto.config())
                        .displayRule(questionDto.displayRule())
                        .build());
                questionOrder++;

                int optionOrder = 0;
                for (AdminOptionDto optionDto : questionDto.options() == null ? List.<AdminOptionDto>of() : questionDto.options()) {
                    questionOptionRepository.save(QuestionOption.builder()
                            .question(question)
                            .code(optionDto.code())
                            .label(optionDto.label())
                            .sortOrder(optionDto.sortOrder() == 0 ? optionOrder : optionDto.sortOrder())
                            .valueNumeric(optionDto.valueNumeric())
                            .metadata(optionDto.metadata() == null ? Map.of() : optionDto.metadata())
                            .build());
                    optionOrder++;
                }
            }
        }
        questionOptionRepository.flush();
    }

    private void copyContent(SurveyVersion source, SurveyVersion target) {
        AdminSurveyVersionDto content = getVersion(source.getSurvey().getId(), source.getId());
        writeContent(target, content.sections());
    }

    private List<AdminQuestionDto> toQuestionDtos(List<Question> questions,
                                                   Map<UUID, List<QuestionOption>> optionsByQuestion) {
        return questions.stream()
                .sorted(Comparator.comparingInt(Question::getSortOrder))
                .map(q -> new AdminQuestionDto(
                        q.getCode(), q.getType(), q.getPrompt(), q.getHelpText(), q.isRequired(), q.getSortOrder(),
                        q.getConfig(), q.getDisplayRule(),
                        optionsByQuestion.getOrDefault(q.getId(), List.of()).stream()
                                .sorted(Comparator.comparingInt(QuestionOption::getSortOrder))
                                .map(o -> new AdminOptionDto(o.getCode(), o.getLabel(), o.getSortOrder(),
                                        o.getValueNumeric(), o.getMetadata()))
                                .toList()))
                .toList();
    }

    private Map<UUID, Integer> questionCounts(List<SurveyVersion> versions) {
        if (versions.isEmpty()) {
            return Map.of();
        }
        List<UUID> versionIds = versions.stream().map(SurveyVersion::getId).toList();
        Map<UUID, Integer> counts = new HashMap<>();
        for (Question question : questionRepository.findBySurveyVersionIdIn(versionIds)) {
            counts.merge(question.getSurveyVersion().getId(), 1, Integer::sum);
        }
        return counts;
    }

    private AdminSurveyDto toSurveyDto(Survey survey, List<SurveyVersion> versions, Map<UUID, Integer> questionCounts) {
        return new AdminSurveyDto(
                survey.getId(), survey.getCode(), survey.getTheme(), survey.getTitle(), survey.getDescription(),
                survey.getSortOrder(), survey.isActive(),
                versions.stream()
                        .sorted(Comparator.comparingInt(SurveyVersion::getVersion).reversed())
                        .map(v -> new AdminVersionSummaryDto(v.getId(), v.getVersion(), v.getStatus(),
                                v.getPublishedAt(), questionCounts.getOrDefault(v.getId(), 0)))
                        .toList());
    }

    private Survey findSurvey(UUID surveyId) {
        return surveyRepository.findById(surveyId)
                .orElseThrow(() -> new NotFoundException("Survey not found"));
    }

    private SurveyVersion findVersion(UUID surveyId, UUID versionId) {
        SurveyVersion version = surveyVersionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("Survey version not found"));
        if (!version.getSurvey().getId().equals(surveyId)) {
            throw new NotFoundException("Survey version not found");
        }
        return version;
    }
}
