package madoku.craft.API.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Shared naming rules for Madoku systems, files, and mod-facing identifiers. */
public final class MadokuNamingSystem {
	public static final String SYSTEM_NAME = "Madoku-Naming-System";
	public static final String MADOKU_PREFIX = "madoku-craft.";

	private static final String DEFAULT_SCOPED_FALLBACK = "unknown";
	private static final String DEFAULT_FILE_FALLBACK = "file";
	private static final String DEFAULT_CLASS_FALLBACK = "MadokuSystem";
	private static final Set<String> ACRONYMS = Set.of("api", "json", "ui", "hud", "gui");

	private MadokuNamingSystem() {
	}

	/** Returns a canonical scoped identifier like madoku-craft.API or madoku-craft.tools. */
	public static String scopedName(String rawName) {
		String withoutPrefix = stripPrefix(rawName);
		String normalized = normalizeScopedBody(withoutPrefix, DEFAULT_SCOPED_FALLBACK);
		return MADOKU_PREFIX + normalized;
	}

	/** Splits and normalizes nested folder paths into scoped segments. */
	public static List<String> scopedPathSegments(String rawPath) {
		List<String> segments = new ArrayList<>();
		if (rawPath == null || rawPath.isBlank()) {
			segments.add(scopedName(DEFAULT_SCOPED_FALLBACK));
			return segments;
		}

		String normalizedPath = rawPath.replace('\\', '/');
		for (String piece : normalizedPath.split("/+")) {
			if (!piece.isBlank()) {
				segments.add(scopedName(piece));
			}
		}

		if (segments.isEmpty()) {
			segments.add(scopedName(DEFAULT_SCOPED_FALLBACK));
		}
		return segments;
	}

	/** Normalizes a JSON file base name (without extension) into Madoku scoped style. */
	public static String jsonFileBaseName(String rawName) {
		String normalized = rawName == null ? "" : rawName.trim();
		if (normalized.toLowerCase(Locale.ROOT).endsWith(".json")) {
			normalized = normalized.substring(0, normalized.length() - ".json".length());
		}
		return scopedName(normalized.isBlank() ? DEFAULT_FILE_FALLBACK : normalized);
	}

	/** Normalizes Java class names into PascalCase with acronym support (API, JSON, etc). */
	public static String javaClassName(String rawName) {
		if (rawName == null || rawName.isBlank()) {
			return DEFAULT_CLASS_FALLBACK;
		}

		String cleaned = rawName.replaceAll("[^A-Za-z0-9]+", " ").trim();
		if (cleaned.isEmpty()) {
			return DEFAULT_CLASS_FALLBACK;
		}

		StringBuilder builder = new StringBuilder();
		for (String token : cleaned.split("\\s+")) {
			if (token.isBlank()) {
				continue;
			}
			String lower = token.toLowerCase(Locale.ROOT);
			if (ACRONYMS.contains(lower)) {
				builder.append(lower.toUpperCase(Locale.ROOT));
			} else {
				builder
					.append(Character.toUpperCase(lower.charAt(0)))
					.append(lower.substring(1));
			}
		}

		if (builder.isEmpty()) {
			return DEFAULT_CLASS_FALLBACK;
		}
		return builder.toString();
	}

	private static String stripPrefix(String rawName) {
		String normalized = rawName == null ? "" : rawName.trim();
		if (normalized.toLowerCase(Locale.ROOT).startsWith(MADOKU_PREFIX)) {
			return normalized.substring(MADOKU_PREFIX.length());
		}
		return normalized;
	}

	private static String normalizeScopedBody(String raw, String fallback) {
		String value = raw == null ? "" : raw.trim();
		value = value.replaceAll("[^A-Za-z0-9._-]+", "-");
		value = value.replaceAll("-{2,}", "-");
		value = value.replaceAll("\\.+", ".");
		value = value.replaceAll("^[.-]+|[.-]+$", "");
		if (value.isEmpty()) {
			value = fallback;
		}

		List<String> dotParts = new ArrayList<>();
		for (String dotPart : value.split("\\.")) {
			String cleaned = normalizeDashPiece(dotPart);
			if (!cleaned.isEmpty()) {
				dotParts.add(cleaned);
			}
		}

		if (dotParts.isEmpty()) {
			return fallback;
		}
		return String.join(".", dotParts);
	}

	private static String normalizeDashPiece(String raw) {
		String piece = raw == null ? "" : raw.trim();
		piece = piece.replaceAll("[^A-Za-z0-9_-]+", "-");
		piece = piece.replaceAll("-{2,}", "-");
		piece = piece.replaceAll("^-+|-+$", "");
		if (piece.isEmpty()) {
			return "";
		}

		List<String> parts = new ArrayList<>();
		for (String token : piece.split("-+")) {
			if (token.isEmpty()) {
				continue;
			}
			String lower = token.toLowerCase(Locale.ROOT);
			if (ACRONYMS.contains(lower)) {
				parts.add(lower.toUpperCase(Locale.ROOT));
			} else if (isExplicitUpperToken(token)) {
				parts.add(token.toUpperCase(Locale.ROOT));
			} else {
				parts.add(lower);
			}
		}
		return String.join("-", parts);
	}

	private static boolean isExplicitUpperToken(String token) {
		if (token == null || token.isEmpty()) {
			return false;
		}
		boolean hasLetter = false;
		for (char ch : token.toCharArray()) {
			if (Character.isLetter(ch)) {
				hasLetter = true;
				if (!Character.isUpperCase(ch)) {
					return false;
				}
			}
		}
		return hasLetter;
	}
}
