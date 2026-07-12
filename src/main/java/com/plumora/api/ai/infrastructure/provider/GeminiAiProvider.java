package com.plumora.api.ai.infrastructure.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plumora.api.ai.domain.AiWritingActionType;
import com.plumora.api.ai.infrastructure.provider.gemini.GeminiClient;
import com.plumora.api.ai.infrastructure.provider.gemini.GeminiSystemPrompt;
import com.plumora.api.shared.exception.AiInvalidResponseException;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "plumora.ai.provider", havingValue = "gemini")
public class GeminiAiProvider implements AiProvider {

	private final GeminiClient geminiClient;
	private final ObjectMapper objectMapper;

	public GeminiAiProvider(GeminiClient geminiClient, ObjectMapper objectMapper) {
		this.geminiClient = geminiClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public AiProviderResponse generateWritingSuggestion(AiWritingPrompt prompt) {
		String instruction = switch (prompt.actionType()) {
			case REFORMULATE -> "Reformule le texte suivant sans changer son sens.";
			case IMPROVE_STYLE -> "Ameliore le style du texte suivant sans changer son sens.";
			case FIX_REPETITIONS -> "Reecris le texte suivant en supprimant les repetitions inutiles.";
			case MAKE_MORE_EMOTIONAL -> "Rends le texte suivant plus emotionnel, sans changer son sens.";
			case MAKE_DIALOGUE_NATURAL -> "Rends le dialogue suivant plus naturel et oral.";
		};
		String contextTitle = joinTitles(prompt.bookTitle(), prompt.chapterTitle());
		AiTextGenerationResult result = generate(instruction, new AiTextGenerationPrompt(
			prompt.selectedText(),
			null,
			null,
			prompt.contextText(),
			contextTitle
		));
		return new AiProviderResponse(result.suggestion(), result.explanation());
	}

	@Override
	public AiTextGenerationResult rewriteText(AiTextGenerationPrompt prompt) {
		return generate("Reformule le texte suivant sans changer son sens, en respectant le ton demande.", prompt);
	}

	@Override
	public AiTextGenerationResult summarizeText(AiTextGenerationPrompt prompt) {
		return generate("Resume le texte suivant de facon fidele et concise.", prompt);
	}

	@Override
	public AiTextGenerationResult continueText(AiTextGenerationPrompt prompt) {
		return generate(
			"Propose une suite coherente au texte suivant, dans le meme style et la meme voix narrative.",
			prompt
		);
	}

	@Override
	public AiTitleSuggestionResult suggestTitles(AiTextGenerationPrompt prompt) {
		String userPrompt = buildUserPrompt("Propose plusieurs titres accrocheurs et pertinents pour ce texte.", prompt)
			+ "\n\nReponds uniquement avec un JSON valide de la forme : "
			+ "{\"titles\": [\"...\", \"...\"], \"explanation\": \"...\", \"warnings\": [\"...\"]}";
		String json = geminiClient.generateJson(GeminiSystemPrompt.TEXT, userPrompt);
		GeminiTitleSuggestionPayload payload = parse(json, GeminiTitleSuggestionPayload.class);
		return new AiTitleSuggestionResult(
			nullToEmpty(payload.titles()),
			payload.explanation(),
			nullToEmpty(payload.warnings())
		);
	}

	@Override
	public AiBetaReadingResult analyzeForBetaReading(AiBetaReadingPrompt prompt) {
		String userPrompt = buildBetaReadingPrompt(prompt)
			+ "\n\nReponds uniquement avec un JSON valide de la forme : "
			+ "{\"globalFeedback\": \"...\", \"strengths\": [\"...\"], \"weaknesses\": [\"...\"], "
			+ "\"clarityScore\": 0, \"rhythmScore\": 0, \"coherenceScore\": 0, \"characterScore\": 0, "
			+ "\"suggestions\": [\"...\"], \"warnings\": [\"...\"]} (scores entre 0 et 10)";
		String json = geminiClient.generateJson(GeminiSystemPrompt.TEXT, userPrompt);
		GeminiBetaReadingPayload payload = parse(json, GeminiBetaReadingPayload.class);
		return new AiBetaReadingResult(
			payload.globalFeedback(),
			nullToEmpty(payload.strengths()),
			nullToEmpty(payload.weaknesses()),
			clampScore(payload.clarityScore()),
			clampScore(payload.rhythmScore()),
			clampScore(payload.coherenceScore()),
			clampScore(payload.characterScore()),
			nullToEmpty(payload.suggestions()),
			nullToEmpty(payload.warnings())
		);
	}

	@Override
	public String providerName() {
		return "gemini";
	}

	@Override
	public String modelName() {
		return geminiClient.model();
	}

	private AiTextGenerationResult generate(String instruction, AiTextGenerationPrompt prompt) {
		String userPrompt = buildUserPrompt(instruction, prompt)
			+ "\n\nReponds uniquement avec un JSON valide de la forme : "
			+ "{\"suggestion\": \"...\", \"explanation\": \"...\", \"warnings\": [\"...\"]}";
		String json = geminiClient.generateJson(GeminiSystemPrompt.TEXT, userPrompt);
		GeminiTextGenerationPayload payload = parse(json, GeminiTextGenerationPayload.class);
		return new AiTextGenerationResult(payload.suggestion(), payload.explanation(), nullToEmpty(payload.warnings()));
	}

	private String buildUserPrompt(String instruction, AiTextGenerationPrompt prompt) {
		StringBuilder builder = new StringBuilder();
		builder.append(instruction).append('\n');
		if (StringUtils.hasText(prompt.language())) {
			builder.append("Langue demandee : ").append(prompt.language()).append('\n');
		}
		if (StringUtils.hasText(prompt.tone())) {
			builder.append("Ton demande : ").append(prompt.tone()).append('\n');
		}
		if (StringUtils.hasText(prompt.contextTitle())) {
			builder.append("Contexte : ").append(prompt.contextTitle()).append('\n');
		}
		if (StringUtils.hasText(prompt.instruction())) {
			builder.append("Instruction complementaire de l'utilisateur : ").append(prompt.instruction()).append('\n');
		}
		builder.append("Texte :\n").append(prompt.text());
		return builder.toString();
	}

	private String buildBetaReadingPrompt(AiBetaReadingPrompt prompt) {
		StringBuilder builder = new StringBuilder();
		builder.append("Analyse cet extrait comme un beta-lecteur professionnel et donne un retour structure.\n");
		if (StringUtils.hasText(prompt.language())) {
			builder.append("Langue demandee : ").append(prompt.language()).append('\n');
		}
		if (StringUtils.hasText(prompt.genre())) {
			builder.append("Genre : ").append(prompt.genre()).append('\n');
		}
		if (StringUtils.hasText(prompt.expectedFeedbackLevel())) {
			builder.append("Niveau de detail attendu : ").append(prompt.expectedFeedbackLevel()).append('\n');
		}
		builder.append("Texte :\n").append(prompt.text());
		return builder.toString();
	}

	private String joinTitles(String bookTitle, String chapterTitle) {
		if (!StringUtils.hasText(bookTitle)) {
			return chapterTitle;
		}
		if (!StringUtils.hasText(chapterTitle)) {
			return bookTitle;
		}
		return bookTitle + " - " + chapterTitle;
	}

	private <T> T parse(String json, Class<T> type) {
		try {
			return objectMapper.readValue(json, type);
		} catch (JsonProcessingException exception) {
			throw new AiInvalidResponseException("Gemini returned a response that could not be parsed as JSON");
		}
	}

	private List<String> nullToEmpty(List<String> values) {
		return values == null ? List.of() : values;
	}

	private int clampScore(int score) {
		return Math.max(0, Math.min(score, 10));
	}

	private record GeminiTextGenerationPayload(String suggestion, String explanation, List<String> warnings) {
	}

	private record GeminiTitleSuggestionPayload(List<String> titles, String explanation, List<String> warnings) {
	}

	private record GeminiBetaReadingPayload(
		String globalFeedback,
		List<String> strengths,
		List<String> weaknesses,
		int clarityScore,
		int rhythmScore,
		int coherenceScore,
		int characterScore,
		List<String> suggestions,
		List<String> warnings
	) {
	}
}
