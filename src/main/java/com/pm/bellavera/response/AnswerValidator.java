package com.pm.bellavera.response;

import com.pm.bellavera.response.api.AnswerRequest;
import com.pm.bellavera.survey.Question;
import com.pm.bellavera.survey.QuestionOption;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Validates one submitted answer against its {@link Question}'s type and {@code config} JSONB. */
@Component
public class AnswerValidator {

    public List<String> validate(Question question, AnswerRequest answer, List<QuestionOption> options) {
        List<String> errors = new ArrayList<>();
        Map<String, Object> config = question.getConfig();
        String code = question.getCode();

        switch (question.getType()) {
            case SINGLE_CHOICE -> {
                if (isEmpty(answer.optionCodes())) {
                    errors.add(code + ": expected exactly one selected option");
                } else if (answer.optionCodes().size() != 1) {
                    errors.add(code + ": expected exactly one selected option, got " + answer.optionCodes().size());
                } else {
                    validateOptionCodes(code, answer.optionCodes(), options, errors);
                }
            }
            case MULTI_CHOICE -> {
                if (isEmpty(answer.optionCodes())) {
                    errors.add(code + ": expected at least one selected option");
                } else {
                    validateOptionCodes(code, answer.optionCodes(), options, errors);
                }
            }
            case SCALE, NUMBER -> {
                if (answer.valueNumber() == null) {
                    errors.add(code + ": expected a numeric value");
                } else {
                    validateRange(code, answer.valueNumber(), config, errors);
                }
            }
            case TEXT, LONG_TEXT -> {
                if (answer.valueText() == null || answer.valueText().isBlank()) {
                    errors.add(code + ": expected a text value");
                } else {
                    Object maxLength = config.get("maxLength");
                    if (maxLength instanceof Number n && answer.valueText().length() > n.intValue()) {
                        errors.add(code + ": exceeds max length of " + n.intValue());
                    }
                }
            }
            case DATE -> {
                if (answer.valueDate() == null) {
                    errors.add(code + ": expected a date value");
                }
            }
            case BOOLEAN -> {
                if (answer.valueBoolean() == null) {
                    errors.add(code + ": expected a boolean value");
                }
            }
        }
        return errors;
    }

    private void validateOptionCodes(String questionCode, List<String> submittedCodes,
                                      List<QuestionOption> options, List<String> errors) {
        var validCodes = options.stream().map(QuestionOption::getCode).toList();
        for (String submitted : submittedCodes) {
            if (!validCodes.contains(submitted)) {
                errors.add(questionCode + ": '" + submitted + "' is not a valid option");
            }
        }
    }

    private void validateRange(String questionCode, BigDecimal value, Map<String, Object> config, List<String> errors) {
        Object min = config.get("min");
        Object max = config.get("max");
        if (min instanceof Number n && value.doubleValue() < n.doubleValue()) {
            errors.add(questionCode + ": value below minimum of " + n);
        }
        if (max instanceof Number n && value.doubleValue() > n.doubleValue()) {
            errors.add(questionCode + ": value above maximum of " + n);
        }
    }

    private boolean isEmpty(List<String> list) {
        return list == null || list.isEmpty();
    }
}
