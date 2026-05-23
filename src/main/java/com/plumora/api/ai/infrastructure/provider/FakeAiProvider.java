package com.plumora.api.ai.infrastructure.provider;

import com.plumora.api.ai.domain.AiWritingActionType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "plumora.ai.provider", havingValue = "fake", matchIfMissing = true)
public class FakeAiProvider implements AiProvider {

	@Override
	public AiProviderResponse generateWritingSuggestion(AiWritingPrompt prompt) {
		String suggestion = switch (prompt.actionType()) {
			case REFORMULATE -> "Reformulation : " + soften(prompt.selectedText());
			case IMPROVE_STYLE -> "Version plus fluide : " + polish(prompt.selectedText());
			case FIX_REPETITIONS -> "Version allegee des repetitions : " + reduceRepetitions(prompt.selectedText());
			case MAKE_MORE_EMOTIONAL -> "Version plus emotionnelle : " + addEmotion(prompt.selectedText());
			case MAKE_DIALOGUE_NATURAL -> "Dialogue plus naturel : " + naturalDialogue(prompt.selectedText());
		};
		String explanation = "FakeAiProvider local : suggestion generee sans appel externe pour l'action "
			+ prompt.actionType().name() + ".";
		return new AiProviderResponse(suggestion, explanation);
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
}
