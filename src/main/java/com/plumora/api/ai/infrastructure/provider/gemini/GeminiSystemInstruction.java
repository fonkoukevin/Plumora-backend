package com.plumora.api.ai.infrastructure.provider.gemini;

import java.util.List;

public record GeminiSystemInstruction(List<GeminiPart> parts) {
}
