package com.soundshelf.api.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundshelf.api.common.UpstreamException;
import com.soundshelf.api.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns a question into a {@link InterpretedFilter} using Claude's structured output
 * mode. The model is given the shape of the filter and nothing else — no database
 * handle, no SQL, no access to anyone's rows. The worst a prompt injection can do
 * here is produce a filter that returns the wrong subset of the caller's own library.
 */
@Component
public class ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 2048;

    private static final String SYSTEM_PROMPT = """
            You translate a question about someone's saved album library into a structured filter.

            Rules:
            - Only use the fields in the schema. If the question does not constrain a field, set it to null.
            - Decades map to year ranges: "the 90s" is yearFrom 1990, yearTo 1999.
            - "favourites", "best", "top rated" mean minRating 4. "loved" or "5 star" means minRating 5.
            - Genre names should match iTunes genres, e.g. Alternative, Rock, Pop, Hip-Hop/Rap, R&B/Soul, Jazz, Country.
            - Put an artist name in artistContains and an album name in titleContains. Never put an artist in titleContains.
            - "long albums" means minTracks 15, "short albums" or "EPs" means maxTracks 6.
            - interpretation: one short sentence, addressed to the user, describing the filter you chose.
            """;

    /**
     * Nullable fields use anyOf rather than a type array, because the structured
     * output schema subset supports anyOf but not multi-type declarations.
     */
    private static final String FILTER_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "interpretation": { "type": "string" },
                "genres": { "anyOf": [ { "type": "array", "items": { "type": "string" } }, { "type": "null" } ] },
                "artistContains": { "anyOf": [ { "type": "string" }, { "type": "null" } ] },
                "titleContains": { "anyOf": [ { "type": "string" }, { "type": "null" } ] },
                "yearFrom": { "anyOf": [ { "type": "integer" }, { "type": "null" } ] },
                "yearTo": { "anyOf": [ { "type": "integer" }, { "type": "null" } ] },
                "minRating": { "anyOf": [ { "type": "integer" }, { "type": "null" } ] },
                "maxRating": { "anyOf": [ { "type": "integer" }, { "type": "null" } ] },
                "minTracks": { "anyOf": [ { "type": "integer" }, { "type": "null" } ] },
                "maxTracks": { "anyOf": [ { "type": "integer" }, { "type": "null" } ] }
              },
              "required": [
                "interpretation", "genres", "artistContains", "titleContains",
                "yearFrom", "yearTo", "minRating", "maxRating", "minTracks", "maxTracks"
              ],
              "additionalProperties": false
            }
            """;

    private final RestClient client;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    public ClaudeClient(RestClient anthropicRestClient, AppProperties properties, ObjectMapper objectMapper) {
        this.client = anthropicRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return properties.ai().enabled();
    }

    public Optional<InterpretedFilter> interpret(String question) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        Map<String, Object> body = Map.of(
                "model", properties.ai().model(),
                "max_tokens", MAX_TOKENS,
                "system", SYSTEM_PROMPT,
                // Low effort keeps this fast and cheap: the task is a translation, not
                // a reasoning problem, and thinking stays on so the model does not fall
                // back to writing structured output as prose.
                "output_config", Map.of(
                        "effort", "low",
                        "format", Map.of(
                                "type", "json_schema",
                                "schema", readSchema())),
                "messages", List.of(Map.of("role", "user", "content", question)));

        try {
            MessagesResponse response = client.post()
                    .uri("/v1/messages")
                    .header("x-api-key", properties.ai().apiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MessagesResponse.class);

            if (response == null) {
                throw new UpstreamException("The language model returned an empty response");
            }
            if ("refusal".equals(response.stopReason())) {
                log.warn("Model declined to answer a library query");
                return Optional.empty();
            }
            return response.firstText().map(this::parseFilter);

        } catch (RestClientException ex) {
            log.warn("Claude request failed, falling back to keyword parsing", ex);
            return Optional.empty();
        }
    }

    private InterpretedFilter parseFilter(String json) {
        try {
            return objectMapper.readValue(json, InterpretedFilter.class);
        } catch (JsonProcessingException ex) {
            throw new UpstreamException("The language model returned something unreadable", ex);
        }
    }

    private Object readSchema() {
        try {
            return objectMapper.readValue(FILTER_SCHEMA, Map.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Filter schema is not valid JSON", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessagesResponse(List<ContentBlock> content, @JsonProperty("stop_reason") String stopReason) {

        Optional<String> firstText() {
            if (content == null) {
                return Optional.empty();
            }
            return content.stream()
                    .filter(block -> "text".equals(block.type()) && block.text() != null)
                    .map(ContentBlock::text)
                    .findFirst();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContentBlock(String type, String text) {
    }
}
