package com.plumora.api.ai.infrastructure.provider.gemini;

import java.util.List;

public record GeminiGenerateContentResponse(List<GeminiCandidate> candidates) {
}
