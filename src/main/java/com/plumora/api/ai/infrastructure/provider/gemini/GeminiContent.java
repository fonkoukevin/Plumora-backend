package com.plumora.api.ai.infrastructure.provider.gemini;

import java.util.List;

public record GeminiContent(String role, List<GeminiPart> parts) {
}
