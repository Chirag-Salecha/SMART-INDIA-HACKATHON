package com.sih.bidcompliance.service;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Lightweight fuzzy company-name matching (token overlap + Levenshtein
 * ratio on the joined string), used wherever the spec calls for
 * "RapidFuzz or equivalent". Tolerant of legitimate naming variations
 * ("Pvt Ltd" vs "Private Limited") without a heavyweight NLP dependency.
 * Returns a similarity percentage 0-100.
 */
@Component
public class FuzzyNameMatcher {

    private static final Set<String> NOISE_WORDS = Set.of(
            "PVT", "PRIVATE", "LTD", "LIMITED", "LLP", "INC", "CO", "COMPANY", "THE"
    );

    /** Similarity score from 0 to 100. */
    public int similarity(String a, String b) {
        if (a == null || b == null) return 0;
        String normA = normalize(a);
        String normB = normalize(b);
        if (normA.isEmpty() || normB.isEmpty()) return 0;
        if (normA.equals(normB)) return 100;

        int distance = levenshtein(normA, normB);
        int maxLen = Math.max(normA.length(), normB.length());
        int levenshteinScore = maxLen == 0 ? 100 : (int) Math.round((1.0 - ((double) distance / maxLen)) * 100);

        int tokenScore = tokenOverlapScore(a, b);

        // Weighted blend: token overlap is more forgiving of word order and
        // suffix differences; Levenshtein catches close misspellings.
        return (int) Math.round(tokenScore * 0.6 + levenshteinScore * 0.4);
    }

    public boolean isMatch(String a, String b, int thresholdPercent) {
        return similarity(a, b) >= thresholdPercent;
    }

    private int tokenOverlapScore(String a, String b) {
        Set<String> tokensA = significantTokens(a);
        Set<String> tokensB = significantTokens(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0;

        Set<String> overlap = new HashSet<>(tokensA);
        overlap.retainAll(tokensB);
        int unionSize = tokensA.size() + tokensB.size() - overlap.size();
        return unionSize == 0 ? 100 : (int) Math.round((double) overlap.size() / unionSize * 100);
    }

    private Set<String> significantTokens(String value) {
        Set<String> tokens = new HashSet<>();
        for (String token : value.toUpperCase().split("[^A-Z0-9]+")) {
            if (!token.isBlank() && !NOISE_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String normalize(String value) {
        return Arrays.stream(value.toUpperCase().split("[^A-Z0-9]+"))
                .filter(t -> !t.isBlank() && !NOISE_WORDS.contains(t))
                .reduce("", (acc, t) -> acc + t);
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
