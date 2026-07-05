package com.plumora.api.book.application;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ExternalBookContentSanitizer {

	private static final Pattern SCRIPT_BLOCK = Pattern.compile("(?is)<script\\b[^>]*>.*?</script>");
	private static final Pattern STYLE_BLOCK = Pattern.compile("(?is)<style\\b[^>]*>.*?</style>");
	private static final Pattern NOSCRIPT_BLOCK = Pattern.compile("(?is)<noscript\\b[^>]*>.*?</noscript>");
	private static final Pattern EVENT_HANDLER_ATTRIBUTE = Pattern.compile("(?is)\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
	private static final Pattern HTML_TAG = Pattern.compile("(?is)<[^>]+>");
	private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?[0-9a-fA-F]+);");
	private static final Map<String, String> NAMED_ENTITIES = namedEntities();

	public String sanitize(String rawContent, String mediaType) {
		if (!StringUtils.hasText(rawContent)) {
			return "";
		}
		if (mediaType != null && mediaType.toLowerCase().startsWith("text/html")) {
			return sanitizeHtml(rawContent);
		}
		return normalizeText(decodeEntities(rawContent));
	}

	private String sanitizeHtml(String rawHtml) {
		String content = SCRIPT_BLOCK.matcher(rawHtml).replaceAll(" ");
		content = STYLE_BLOCK.matcher(content).replaceAll(" ");
		content = NOSCRIPT_BLOCK.matcher(content).replaceAll(" ");
		content = EVENT_HANDLER_ATTRIBUTE.matcher(content).replaceAll("");
		content = content.replaceAll("(?i)<\\s*br\\s*/?\\s*>", "\n");
		content = content.replaceAll("(?i)</\\s*(p|div|h[1-6]|li|blockquote|tr|section|article)\\s*>", "\n");
		content = content.replaceAll("(?i)<\\s*(p|div|h[1-6]|li|blockquote|tr|section|article)\\b[^>]*>", "\n");
		content = HTML_TAG.matcher(content).replaceAll(" ");
		return normalizeText(decodeEntities(content));
	}

	private String decodeEntities(String value) {
		String decoded = value;
		for (Map.Entry<String, String> entity : NAMED_ENTITIES.entrySet()) {
			decoded = decoded.replace(entity.getKey(), entity.getValue());
		}

		Matcher matcher = NUMERIC_ENTITY.matcher(decoded);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			String rawCode = matcher.group(1);
			try {
				int codePoint = rawCode.startsWith("x") || rawCode.startsWith("X")
					? Integer.parseInt(rawCode.substring(1), 16)
					: Integer.parseInt(rawCode);
				matcher.appendReplacement(result, Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
			} catch (IllegalArgumentException exception) {
				matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
			}
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private String normalizeText(String value) {
		return value
			.replace("\r\n", "\n")
			.replace('\r', '\n')
			.replace('\u00A0', ' ')
			.replaceAll("[\\t\\f ]+", " ")
			.replaceAll(" +([.,;:!?])", "$1")
			.replaceAll(" *\\n *", "\n")
			.replaceAll("\\n{3,}", "\n\n")
			.trim();
	}

	private static Map<String, String> namedEntities() {
		Map<String, String> entities = new HashMap<>();
		entities.put("&nbsp;", " ");
		entities.put("&amp;", "&");
		entities.put("&lt;", "<");
		entities.put("&gt;", ">");
		entities.put("&quot;", "\"");
		entities.put("&#39;", "'");
		entities.put("&apos;", "'");
		entities.put("&rsquo;", "'");
		entities.put("&lsquo;", "'");
		entities.put("&rdquo;", "\"");
		entities.put("&ldquo;", "\"");
		entities.put("&mdash;", "-");
		entities.put("&ndash;", "-");
		entities.put("&hellip;", "...");
		return entities;
	}
}
