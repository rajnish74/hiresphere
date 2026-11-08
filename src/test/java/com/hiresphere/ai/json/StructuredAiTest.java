package com.hiresphere.ai.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hiresphere.ai.AiClient;
import com.hiresphere.ai.AiException;
import com.hiresphere.ai.AiException.Kind;
import com.hiresphere.ai.AiRequest;
import com.hiresphere.ai.AiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StructuredAiTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Answer(List<String> skills, Integer years) {
    }

    @Mock
    private AiClient client;

    private StructuredAi ai;
    private final Map<String, Object> schema = Map.of("type", "object");

    @BeforeEach
    void setUp() {
        ai = new StructuredAi(client, JsonMapper.builder().build());
    }

    private AiResponse ok(String text) {
        return new AiResponse(text, "STOP", false, 10, 5, 15, "m", 12);
    }

    private void answer(String text) {
        when(client.generate(any(AiRequest.class))).thenReturn(ok(text));
    }

    private void assertInvalid(String text) {
        answer(text);
        assertThatThrownBy(() -> ai.ask("sys", "prompt", schema, Answer.class))
                .isInstanceOfSatisfying(AiException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(Kind.INVALID_OUTPUT);
                    assertThat(e.isRetryable()).isTrue();
                });
    }

    @Test
    void shouldParseAValidAnswerIntoTheType() {
        answer("{\"skills\":[\"Java\",\"Docker\"],\"years\":3}");

        StructuredAi.Structured<Answer> result = ai.ask("sys", "prompt", schema, Answer.class);

        assertThat(result.value().skills()).containsExactly("Java", "Docker");
        assertThat(result.value().years()).isEqualTo(3);
        assertThat(result.response().totalTokens()).isEqualTo(15);
    }

    @Test
    void shouldAskForJsonWithTheSchemaAndPassTheInstructions() {
        answer("{\"skills\":[]}");

        ai.ask("the system text", "the prompt", schema, Answer.class);

        ArgumentCaptor<AiRequest> request = ArgumentCaptor.forClass(AiRequest.class);
        verify(client).generate(request.capture());
        assertThat(request.getValue().wantsJson()).isTrue();
        assertThat(request.getValue().jsonSchema()).isEqualTo(schema);
        assertThat(request.getValue().systemInstruction()).isEqualTo("the system text");
        assertThat(request.getValue().prompt()).isEqualTo("the prompt");
        assertThat(request.getValue().temperature()).isEqualTo(0.2);
    }

    @Test
    void anAnswerWrappedInAMarkdownFenceShouldStillBeUsed() {
        answer("```json\n{\"skills\":[\"Java\"]}\n```");

        assertThat(ai.ask("s", "p", schema, Answer.class).value().skills()).containsExactly("Java");
    }

    @Test
    void unknownExtraFieldsShouldBeIgnored() {
        answer("{\"skills\":[\"Java\"],\"confidence\":0.9,\"notes\":\"hi\"}");

        assertThat(ai.ask("s", "p", schema, Answer.class).value().skills()).containsExactly("Java");
    }

    @Test
    void aMissingFieldShouldComeBackAsNullForTheCallerToCheck() {
        answer("{\"years\":2}");

        assertThat(ai.ask("s", "p", schema, Answer.class).value().skills()).isNull();
    }

    @Test
    void anAnswerCutOffByTheTokenLimitShouldBeRejected() {
        when(client.generate(any(AiRequest.class)))
                .thenReturn(new AiResponse("{\"skills\":[\"Ja", "MAX_TOKENS", true, 10, 5, 15, "m", 12));

        assertThatThrownBy(() -> ai.ask("s", "p", schema, Answer.class))
                .isInstanceOfSatisfying(AiException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.INVALID_OUTPUT));
    }

    @Test
    void proseAroundTheJsonShouldBeRejected() {
        assertInvalid("Sure! Here you go: {\"skills\":[\"Java\"]}");
        assertInvalid("{\"skills\":[\"Java\"]} Hope that helps!");
    }

    @Test
    void anArrayOrAScalarInsteadOfAnObjectShouldBeRejected() {
        assertInvalid("[\"Java\"]");
        assertInvalid("42");
        assertInvalid("null");
    }

    @Test
    void brokenJsonShouldBeRejectedWithoutEchoingTheContentInTheDetail() {
        answer("{\"skills\": [\"Java\", \"secret-person-name\" \"oops\"]}");

        assertThatThrownBy(() -> ai.ask("s", "p", schema, Answer.class))
                .isInstanceOfSatisfying(AiException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(Kind.INVALID_OUTPUT);
                    assertThat(e.getDetail()).startsWith("Unparseable JSON");
                    assertThat(e.getMessage()).doesNotContain("secret-person-name");
                });
    }

    @Test
    void theWrongTypeForAFieldShouldBeRejected() {
        assertInvalid("{\"skills\":\"Java\",\"years\":1}");
        assertInvalid("{\"skills\":[],\"years\":\"many\"}");
    }

    @Test
    void errorsFromTheClientShouldPassThroughUntouched() {
        when(client.generate(any(AiRequest.class))).thenThrow(new AiException(Kind.TIMEOUT, "slow"));

        assertThatThrownBy(() -> ai.ask("s", "p", schema, Answer.class))
                .isInstanceOfSatisfying(AiException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.TIMEOUT));
    }

    @Test
    void unwrapShouldOnlyTouchFencedAnswers() {
        assertThat(StructuredAi.unwrap("  {\"a\":1}  ")).isEqualTo("{\"a\":1}");
        assertThat(StructuredAi.unwrap("```\n{\"a\":1}\n```")).isEqualTo("{\"a\":1}");
        assertThat(StructuredAi.unwrap("```json\n{\"a\":1}")).isEqualTo("{\"a\":1}");
        assertThat(StructuredAi.unwrap("```")).isEmpty();
    }
}