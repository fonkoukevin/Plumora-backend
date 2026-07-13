package com.plumora.api.ai.application;

import com.plumora.api.shared.exception.AiProviderUnavailableException;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * In-memory switch letting an admin temporarily disable Plumo IA. Per-instance and reset on
 * restart, like AiUsageLimiter: enough for an MVP, not a substitute for a persisted setting.
 */
@Component
public class AiFeatureToggle {

	private volatile boolean enabled = true;
	private volatile LocalDateTime updatedAt = LocalDateTime.now();

	public boolean isEnabled() {
		return enabled;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		this.updatedAt = LocalDateTime.now();
	}

	public void ensureEnabled() {
		if (!enabled) {
			throw new AiProviderUnavailableException("Plumo IA is temporarily disabled by an administrator");
		}
	}
}
