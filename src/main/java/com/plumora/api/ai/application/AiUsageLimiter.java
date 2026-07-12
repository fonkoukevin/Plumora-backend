package com.plumora.api.ai.application;

import com.plumora.api.shared.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Simple in-memory sliding-window limiter for Plumo IA calls.
 * Per-instance and reset on restart: enough for an MVP, not a substitute for a persisted quota system.
 */
@Component
public class AiUsageLimiter {

	private static final int MAX_REQUESTS_PER_WINDOW = 20;
	private static final Duration WINDOW = Duration.ofMinutes(5);

	private final Map<String, Deque<Instant>> requestsByUser = new ConcurrentHashMap<>();

	public void checkAndRecord(String currentUserEmail) {
		Deque<Instant> timestamps = requestsByUser.computeIfAbsent(currentUserEmail, key -> new ArrayDeque<>());
		Instant now = Instant.now();
		synchronized (timestamps) {
			while (!timestamps.isEmpty() && Duration.between(timestamps.peekFirst(), now).compareTo(WINDOW) > 0) {
				timestamps.pollFirst();
			}
			if (timestamps.size() >= MAX_REQUESTS_PER_WINDOW) {
				throw new BusinessException("Plumo IA usage limit reached, please try again in a few minutes");
			}
			timestamps.addLast(now);
		}
	}
}
