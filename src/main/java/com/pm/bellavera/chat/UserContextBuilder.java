package com.pm.bellavera.chat;

import com.pm.bellavera.insight.InsightDto;
import com.pm.bellavera.insight.InsightQueryService;
import com.pm.bellavera.insight.InsightRun;
import com.pm.bellavera.response.Answer;
import com.pm.bellavera.response.AnswerRepository;
import com.pm.bellavera.response.SurveyResponse;
import com.pm.bellavera.response.SurveyResponseRepository;
import com.pm.bellavera.survey.QuestionOption;
import com.pm.bellavera.survey.Survey;
import com.pm.bellavera.survey.SurveyRepository;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.UserProfile;
import com.pm.bellavera.user.UserProfileRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the RAG context for {@code /chat}: no vector search - every call assembles this specific
 * user's profile, their latest submitted answers per survey, and their current insights, in a
 * fixed order. The result is hashed and persisted as a {@link ChatContextSnapshot}, reused as-is
 * until the content actually changes (a new submission or a new insight run).
 */
@Component
public class UserContextBuilder {

    private final UserProfileRepository userProfileRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final AnswerRepository answerRepository;
    private final InsightQueryService insightQueryService;
    private final ChatContextSnapshotRepository chatContextSnapshotRepository;

    public UserContextBuilder(UserProfileRepository userProfileRepository,
                               SurveyRepository surveyRepository,
                               SurveyResponseRepository surveyResponseRepository,
                               AnswerRepository answerRepository,
                               InsightQueryService insightQueryService,
                               ChatContextSnapshotRepository chatContextSnapshotRepository) {
        this.userProfileRepository = userProfileRepository;
        this.surveyRepository = surveyRepository;
        this.surveyResponseRepository = surveyResponseRepository;
        this.answerRepository = answerRepository;
        this.insightQueryService = insightQueryService;
        this.chatContextSnapshotRepository = chatContextSnapshotRepository;
    }

    @Transactional
    public ChatContextSnapshot buildOrReuse(AppUser user, int tokenBudget) {
        String content = renderContent(user, tokenBudget);
        String hash = sha256(content);

        var existing = chatContextSnapshotRepository.findFirstByUserIdOrderByBuiltAtDesc(user.getId());
        if (existing.isPresent() && existing.get().getContentHash().equals(hash)) {
            return existing.get();
        }

        InsightRun latestRun = insightQueryService.latestRun(user.getId()).orElse(null);
        ChatContextSnapshot snapshot = ChatContextSnapshot.builder()
                .user(user)
                .insightRun(latestRun)
                .content(content)
                .contentHash(hash)
                .tokenEstimate(estimateTokens(content))
                .build();
        return chatContextSnapshotRepository.save(snapshot);
    }

    private String renderContent(AppUser user, int tokenBudget) {
        StringBuilder sb = new StringBuilder();

        userProfileRepository.findById(user.getId()).ifPresent(profile -> renderProfile(sb, profile));

        List<Survey> surveys = surveyRepository.findByActiveTrueOrderBySortOrder();
        for (Survey survey : surveys) {
            List<SurveyResponse> submitted = surveyResponseRepository
                    .findSubmittedForUserAndSurvey(user.getId(), survey.getId());
            if (submitted.isEmpty()) {
                continue;
            }
            SurveyResponse latest = submitted.get(0);
            List<Answer> answers = answerRepository.findBySurveyResponseIdOrderByQuestionCode(latest.getId());
            if (answers.isEmpty()) {
                continue;
            }
            sb.append("\nSurvey: ").append(survey.getTitle()).append('\n');
            for (Answer answer : answers) {
                sb.append("- ").append(answer.getQuestion().getPrompt()).append(" -> ")
                        .append(formatAnswerValue(answer)).append('\n');
            }
        }

        List<InsightDto> insights = insightQueryService.latestForUser(user.getId());
        sb.append("\nCurrent insights:\n");
        if (insights.isEmpty()) {
            sb.append("- (none yet)\n");
        } else {
            for (InsightDto insight : insights) {
                sb.append("- [").append(insight.domain()).append("] ").append(insight.label())
                        .append(": ").append(insight.band());
                if (insight.rationale() != null) {
                    sb.append(" - ").append(insight.rationale());
                }
                sb.append('\n');
            }
        }

        String content = sb.toString();
        int maxChars = tokenBudget * 4;
        if (content.length() > maxChars) {
            content = content.substring(0, maxChars) + "\n[...context truncated to fit token budget...]";
        }
        return content;
    }

    private void renderProfile(StringBuilder sb, UserProfile profile) {
        sb.append("User profile:\n");
        if (profile.getDisplayName() != null) {
            sb.append("- Display name: ").append(profile.getDisplayName()).append('\n');
        }
        if (profile.getBirthYear() != null) {
            sb.append("- Birth year: ").append(profile.getBirthYear()).append('\n');
        }
        if (profile.getCountry() != null) {
            sb.append("- Country: ").append(profile.getCountry()).append('\n');
        }
        sb.append("- Unit system: ").append(profile.getUnitSystem()).append('\n');
    }

    private String formatAnswerValue(Answer answer) {
        if (!answer.getSelectedOptions().isEmpty()) {
            return answer.getSelectedOptions().stream()
                    .map(QuestionOption::getLabel)
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.joining(", "));
        }
        if (answer.getValueText() != null) {
            return answer.getValueText();
        }
        if (answer.getValueNumber() != null) {
            return answer.getValueNumber().toPlainString();
        }
        if (answer.getValueBoolean() != null) {
            return answer.getValueBoolean() ? "Yes" : "No";
        }
        if (answer.getValueDate() != null) {
            return answer.getValueDate().toString();
        }
        return "(no answer)";
    }

    private int estimateTokens(String content) {
        return content.length() / 4;
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
