package com.pm.bellavera.response;

import com.pm.bellavera.response.api.AnswerRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayRuleEvaluatorTest {

    private final DisplayRuleEvaluator evaluator = new DisplayRuleEvaluator();

    @Test
    void noRuleIsAlwaysVisible() {
        assertThat(evaluator.isVisible(null, Map.of())).isTrue();
        assertThat(evaluator.isVisible(Map.of(), Map.of())).isTrue();
    }

    @Test
    void eqMatchesTheAnsweredValue() {
        Map<String, Object> rule = rule("eq", "smokes", "yes");
        assertThat(evaluator.isVisible(rule, answers("smokes", textAnswer("yes")))).isTrue();
        assertThat(evaluator.isVisible(rule, answers("smokes", textAnswer("no")))).isFalse();
    }

    @Test
    void neIsTheOppositeOfEq() {
        Map<String, Object> rule = rule("ne", "smokes", "yes");
        assertThat(evaluator.isVisible(rule, answers("smokes", textAnswer("no")))).isTrue();
        assertThat(evaluator.isVisible(rule, answers("smokes", textAnswer("yes")))).isFalse();
    }

    @Test
    void inMatchesAnyOfTheOptions() {
        Map<String, Object> rule = Map.of("all", List.of(
                Map.of("questionCode", "country", "op", "in", "value", List.of("US", "CA"))));
        assertThat(evaluator.isVisible(rule, answers("country", textAnswer("CA")))).isTrue();
        assertThat(evaluator.isVisible(rule, answers("country", textAnswer("FR")))).isFalse();
    }

    @Test
    void anUnansweredGatingQuestionMeansTheConditionIsUnmet() {
        Map<String, Object> rule = rule("eq", "smokes", "yes");
        assertThat(evaluator.isVisible(rule, Map.of())).isFalse();
    }

    /**
     * The admin editor rejects an unknown operator before publish, but content authored directly
     * in a migration skips that check. This must fail closed - hide the question - rather than the
     * old behaviour of defaulting to "condition satisfied".
     */
    @Test
    void anUnrecognizedOperatorFailsClosedRatherThanDefaultingToVisible() {
        Map<String, Object> rule = rule("startswith", "smokes", "y");
        assertThat(evaluator.isVisible(rule, answers("smokes", textAnswer("yes")))).isFalse();
    }

    private static Map<String, Object> rule(String op, String questionCode, Object value) {
        return Map.of("all", List.of(Map.of("questionCode", questionCode, "op", op, "value", value)));
    }

    private static Map<String, AnswerRequest> answers(String questionCode, AnswerRequest answer) {
        return Map.of(questionCode, answer);
    }

    private static AnswerRequest textAnswer(String value) {
        return new AnswerRequest("q", value, null, null, null, null);
    }
}
