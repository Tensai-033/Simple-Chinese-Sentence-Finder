package com.mandarinish.sentencefinder.model;

import com.mandarinish.sentencefinder.search.SearchMode;

public class SearchResult {
    private final Sentence sentence;
    private final String hanzi;
    private final String english;
    private final SearchMode mode;
    private final double score;
    private final boolean simplifiedMode;
    private String pinyin = "";

    public SearchResult(Sentence sentence, String hanzi, String english, SearchMode mode, double score, boolean simplifiedMode) {
        this.sentence = sentence;
        this.hanzi = hanzi;
        this.english = english;
        this.mode = mode;
        this.score = score;
        this.simplifiedMode = simplifiedMode;
    }

    public Sentence getSentence() {
        return sentence;
    }

    public String getHanzi() {
        return hanzi;
    }

    public String getEnglish() {
        return english;
    }

    public SearchMode getMode() {
        return mode;
    }

    public double getScore() {
        return score;
    }

    public boolean isSimplifiedMode() {
        return simplifiedMode;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }
}
