package com.plumora.api.ai.infrastructure.provider;

import com.plumora.api.ai.domain.AiWritingActionType;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "plumora.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiProvider implements AiProvider {

	@Override
	public AiProviderResponse generateWritingSuggestion(AiWritingPrompt prompt) {
		String suggestion = switch (prompt.actionType()) {
			case REFORMULATE -> "Reformulation : " + soften(prompt.selectedText());
			case IMPROVE_STYLE -> "Version plus fluide : " + polish(prompt.selectedText());
			case FIX_REPETITIONS -> "Version allegee des repetitions : " + reduceRepetitions(prompt.selectedText());
			case MAKE_MORE_EMOTIONAL -> "Version plus emotionnelle : " + addEmotion(prompt.selectedText());
			case MAKE_DIALOGUE_NATURAL -> "Dialogue plus naturel : " + naturalDialogue(prompt.selectedText());
		};
		String explanation = "MockAiProvider local : suggestion generee sans appel externe pour l'action "
			+ prompt.actionType().name() + ".";
		return new AiProviderResponse(suggestion, explanation);
	}

	@Override
	public AiTextGenerationResult rewriteText(AiTextGenerationPrompt prompt) {
		String suggestion = "Reformulation : " + polish(prompt.text());
		return new AiTextGenerationResult(
			suggestion,
			"MockAiProvider local : reformulation generee sans appel externe.",
			List.of()
		);
	}

	@Override
	public AiTextGenerationResult summarizeText(AiTextGenerationPrompt prompt) {
		String suggestion = "Resume : " + shorten(prompt.text());
		return new AiTextGenerationResult(
			suggestion,
			"MockAiProvider local : resume genere sans appel externe.",
			List.of()
		);
	}

	@Override
	public AiTextGenerationResult continueText(AiTextGenerationPrompt prompt) {
		String suggestion = prompt.text().trim()
			+ " La suite restait encore incertaine, mais quelque chose avait deja change.";
		return new AiTextGenerationResult(
			suggestion,
			"MockAiProvider local : suite generee sans appel externe.",
			List.of()
		);
	}

	@Override
	public AiTitleSuggestionResult suggestTitles(AiTextGenerationPrompt prompt) {
		String seed = firstWords(prompt.text(), 4);
		List<String> titles = List.of(
			"Le Secret de " + seed,
			seed + " : le commencement",
			"Au-dela de " + seed
		);
		return new AiTitleSuggestionResult(
			titles,
			"MockAiProvider local : titres generes sans appel externe.",
			List.of()
		);
	}

	@Override
	public AiBetaReadingResult analyzeForBetaReading(AiBetaReadingPrompt prompt) {
		return new AiBetaReadingResult(
			"Le texte est globalement clair, avec un potentiel a developper.",
			List.of("Voix narrative reconnaissable.", "Rythme correct sur l'extrait fourni."),
			List.of("Certaines phrases pourraient etre resserrees."),
			7,
			7,
			7,
			7,
			List.of("Relire les transitions entre les paragraphes."),
			List.of("Analyse generee localement (MockAiProvider), a affiner avec une relecture humaine.")
		);
	}

	@Override
	public String providerName() {
		return "mock";
	}

	@Override
	public String modelName() {
		return "local-heuristic";
	}

	private String soften(String text) {
		return text.trim() + " (idee reformulee avec une formulation plus claire).";
	}

	private String polish(String text) {
		return text.trim().replace(" tres ", " vraiment ") + " (style lisse et rythme).";
	}

	private String reduceRepetitions(String text) {
		return text.trim().replaceAll("\\b(\\w+)\\s+\\1\\b", "$1") + " (repetitions reduites).";
	}

	private String addEmotion(String text) {
		return text.trim() + " Elle sentit ce choix peser plus lourd qu'elle ne voulait l'avouer.";
	}

	private String naturalDialogue(String text) {
		return text.trim().replace("Je ne sais pas", "J'en sais rien") + " (dialogue rendu plus oral).";
	}

	private String shorten(String text) {
		String trimmed = text.trim();
		if (trimmed.length() <= 140) {
			return trimmed;
		}
		return trimmed.substring(0, 140).trim() + "...";
	}

	private String firstWords(String text, int count) {
		if (!StringUtils.hasText(text)) {
			return "l'Histoire";
		}
		String[] words = text.trim().split("\\s+");
		int limit = Math.min(count, words.length);
		return String.join(" ", List.of(words).subList(0, limit));
	}
}
