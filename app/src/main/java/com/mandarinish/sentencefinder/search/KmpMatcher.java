package com.mandarinish.sentencefinder.search;

class KmpMatcher {
    boolean contains(String text, String pattern) {
        if (pattern.isEmpty()) {
            return true;
        }
        if (text.isEmpty()) {
            return false;
        }

        int[] lps = computeLps(pattern);
        int textIndex = 0;
        int patternIndex = 0;

        while (textIndex < text.length()) {
            if (pattern.charAt(patternIndex) == text.charAt(textIndex)) {
                textIndex++;
                patternIndex++;
            }

            if (patternIndex == pattern.length()) {
                return true;
            }

            if (textIndex < text.length() && pattern.charAt(patternIndex) != text.charAt(textIndex)) {
                if (patternIndex != 0) {
                    patternIndex = lps[patternIndex - 1];
                } else {
                    textIndex++;
                }
            }
        }

        return false;
    }

    private int[] computeLps(String pattern) {
        int[] lps = new int[pattern.length()];
        int length = 0;
        int index = 1;

        while (index < pattern.length()) {
            if (pattern.charAt(index) == pattern.charAt(length)) {
                length++;
                lps[index] = length;
                index++;
            } else if (length != 0) {
                length = lps[length - 1];
            } else {
                lps[index] = 0;
                index++;
            }
        }

        return lps;
    }
}
