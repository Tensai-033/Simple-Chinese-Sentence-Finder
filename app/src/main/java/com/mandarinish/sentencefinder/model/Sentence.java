package com.mandarinish.sentencefinder.model;

import com.mandarinish.sentencefinder.text.ChineseTextTools;

public class Sentence {
    private final String originalHanzi;
    private final String english;
    private String simplified;
    private String traditional;
    private String simplifiedPinyin;
    private String traditionalPinyin;

    public Sentence(String originalHanzi, String english) {
        this.originalHanzi = originalHanzi;
        this.english = english;
    }

    public String getEnglish() {
        return english;
    }

    public String getHanzi(boolean simplifiedMode, ChineseTextTools textTools) {
        if (simplifiedMode) {
            if (simplified == null) {
                simplified = textTools.toSimplified(originalHanzi);
            }
            return simplified;
        }

        if (traditional == null) {
            traditional = textTools.toTraditional(originalHanzi);
        }
        return traditional;
    }

    public String getPinyin(boolean simplifiedMode, ChineseTextTools textTools) {
        if (simplifiedMode) {
            if (simplifiedPinyin == null) {
                simplifiedPinyin = textTools.toPinyin(getHanzi(true, textTools));
            }
            return simplifiedPinyin;
        }

        if (traditionalPinyin == null) {
            traditionalPinyin = textTools.toPinyin(getHanzi(false, textTools));
        }
        return traditionalPinyin;
    }
}
