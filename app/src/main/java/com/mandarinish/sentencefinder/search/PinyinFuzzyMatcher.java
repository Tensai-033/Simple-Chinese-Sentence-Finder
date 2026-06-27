package com.mandarinish.sentencefinder.search;

import com.mandarinish.sentencefinder.text.ChineseTextTools;

public class PinyinFuzzyMatcher {
    public static final double DEFAULT_THRESHOLD = 0.65;

    private static final double COST_TONE_DIFFERENCE = 0.1;
    private static final double COST_SUBSTITUTION = 1.0;
    private static final double COST_INSERT_DELETE = 1.0;

    private static final char[][] PINYIN_GROUPS = new char[][]{
            {'a', '\u0101', '\u00e1', '\u01ce', '\u00e0'},
            {'e', '\u0113', '\u00e9', '\u011b', '\u00e8'},
            {'i', '\u012b', '\u00ed', '\u01d0', '\u00ec'},
            {'o', '\u014d', '\u00f3', '\u01d2', '\u00f2'},
            {'u', '\u016b', '\u00fa', '\u01d4', '\u00f9'},
            {'v', '\u00fc', '\u01d6', '\u01d8', '\u01da', '\u01dc', 'u'}
    };

    private final ChineseTextTools textTools;

    public PinyinFuzzyMatcher(ChineseTextTools textTools) {
        this.textTools = textTools;
    }

    public double findBestSimilarity(String normalizedQuery, String pinyin) {
        String normalizedTarget = textTools.normalizePinyin(pinyin);
        if (normalizedQuery.isEmpty() || normalizedTarget.isEmpty()) {
            return 0;
        }

        int queryLength = normalizedQuery.length();
        int minWindow = Math.max(1, queryLength - 2);
        int maxWindow = Math.min(normalizedTarget.length(), queryLength + 2);
        double bestScore = calculateSimilarity(normalizedQuery, normalizedTarget);

        for (int windowLength = minWindow; windowLength <= maxWindow; windowLength++) {
            for (int start = 0; start + windowLength <= normalizedTarget.length(); start++) {
                String candidate = normalizedTarget.substring(start, start + windowLength);
                double score = calculateSimilarity(normalizedQuery, candidate);
                if (score > bestScore) {
                    bestScore = score;
                }
                if (bestScore >= 1.0) {
                    return bestScore;
                }
            }
        }

        return bestScore;
    }

    private double calculateSimilarity(String source, String target) {
        int m = source.length();
        int n = target.length();
        double[][] distance = new double[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            distance[i][0] = i * COST_INSERT_DELETE;
        }
        for (int j = 0; j <= n; j++) {
            distance[0][j] = j * COST_INSERT_DELETE;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                double cost = getSubstitutionCost(source.charAt(i - 1), target.charAt(j - 1));
                distance[i][j] = Math.min(
                        Math.min(distance[i - 1][j] + COST_INSERT_DELETE, distance[i][j - 1] + COST_INSERT_DELETE),
                        distance[i - 1][j - 1] + cost
                );
            }
        }

        double maxLength = Math.max(Math.max(m, n), 1);
        return Math.max(0, 1 - (distance[m][n] / maxLength));
    }

    private double getSubstitutionCost(char left, char right) {
        char l = Character.toLowerCase(left);
        char r = Character.toLowerCase(right);

        if (l == r) {
            return 0;
        }

        for (char[] group : PINYIN_GROUPS) {
            boolean leftMatch = false;
            boolean rightMatch = false;
            for (char value : group) {
                if (value == l) {
                    leftMatch = true;
                }
                if (value == r) {
                    rightMatch = true;
                }
            }
            if (leftMatch && rightMatch) {
                return COST_TONE_DIFFERENCE;
            }
        }

        return COST_SUBSTITUTION;
    }
}
