package com.plumora.api.book.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternalBookContentSanitizerTest {

	private final ExternalBookContentSanitizer sanitizer = new ExternalBookContentSanitizer();

	@Test
	void sanitizeHtmlRemovesScriptsStylesHandlersAndTags() {
		String content = """
			<style>body { color: red; }</style>
			<h1 onclick="alert('xss')">Title&nbsp;&amp;&nbsp;Intro</h1>
			<p onload="bad()">First <strong>paragraph</strong>.</p>
			<script>alert('xss')</script>
			""";

		String sanitized = sanitizer.sanitize(content, "text/html; charset=utf-8");

		assertThat(sanitized).contains("Title & Intro");
		assertThat(sanitized).contains("First paragraph.");
		assertThat(sanitized).doesNotContain("script", "style", "onclick", "onload", "<", ">");
	}

	@Test
	void sanitizePlainTextNormalizesWhitespaceAndDecodesEntities() {
		String sanitized = sanitizer.sanitize(" Line&nbsp;one\r\n\r\n\r\nLine&#32;two &mdash; done ", "text/plain");

		assertThat(sanitized).isEqualTo("Line one\n\nLine two - done");
	}
}
