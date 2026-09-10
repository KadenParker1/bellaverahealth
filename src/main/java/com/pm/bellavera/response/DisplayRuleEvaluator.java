package com.pm.bellavera.response;

import com.pm.bellavera.response.api.AnswerRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Evaluates a question's {@code display_rule} JSONB config against the answers submitted so far,
 * so a hidden (not-currently-displayed) question is never flagged as a missing required answer.
 *
 * <p>Only the {@code {"all": [{"questionCode","op","value"}, ...]}} shape (AND of simple
 * comparisons) is supported - that is the only shape any survey content needs today. Extend this
 * if branching logic grows more complex.
 *
 * <p>{@code AdminSurveyService} rejects an unrecognized {@code op} before a survey can be
 * published, but content authored directly in a migration (the other route CLAUDE.md allows for
 * Stage 8) never goes through that check. An unrecognized op here therefore fails closed - the
 * condition counts as unmet and the question stays hidden - rather than defaulting to "condition
 * satisfied", which would silently show or require a question a broken rule was meant to gate.
 */
@Component
public class DisplayRuleEvaluator {

    private static final Logger log = LoggerFactory.getLogger(DisplayRuleEvaluator.class);

    @SuppressWarnings("unchecked")
    public boolean isVisible(Map<String, Object> displayRule, Map<String, AnswerRequest> answersByQuestionCode) {
        if (displayRule == null || displayRule.isEmpty()) {
            return true;
        }
        Object conditions = displayRule.get("all");
        if (!(conditions instanceof List<?> list)) {
            return true;
        }
        for (Object raw : list) {
            if (!(raw instanceof Map<?, ?> condition)) {
                continue;
            }
            if (!matches((Map<String, Object>) condition, answersByQuestionCode)) {
                return false;
            }
        }
        return true;
    }

    private boolean matches(Map<String, Object> condition, Map<String, AnswerRequest> answersByQuestionCode) {
        String questionCode = String.valueOf(condition.get("questionCode"));
        String op = String.valueOf(condition.get("op"));
        Object expected = condition.get("value");
        AnswerRequest answer = answersByQuestionCode.get(questionCode);
        Object actual = actualValue(answer);

        return switch (op) {
            case "eq" -> Objects.equals(String.valueOf(actual), String.valueOf(expected));
            case "ne" -> !Objects.equals(String.valueOf(actual), String.valueOf(expected));
            case "in" -> expected instanceof List<?> options
                    && options.stream().anyMatch(o -> Objects.equals(String.valueOf(o), String.valueOf(actual)));
            default -> {
                log.warn("Unsupported display rule operator '{}' on question '{}'; treating its "
                        + "condition as unmet", op, questionCode);
                yield false;
            }
        };
    }

    private Object actualValue(AnswerRequest answer) {
        if (answer == null) {
            return null;
        }
        if (answer.optionCodes() != null && !answer.optionCodes().isEmpty()) {
            return answer.optionCodes().size() == 1 ? answer.optionCodes().get(0) : answer.optionCodes();
        }
        if (answer.valueBoolean() != null) {
            return answer.valueBoolean();
        }
        if (answer.valueNumber() != null) {
            return answer.valueNumber();
        }
        if (answer.valueDate() != null) {
            return answer.valueDate();
        }
        return answer.valueText();
    }
}
