package com.mandarinish.sentencefinder.ui;

import com.mandarinish.sentencefinder.search.SearchMode;

public class SentenceResultViewModel {
    private final String hanzi;
    private final String pinyin;
    private final String english;
    private final SearchMode mode;
    private final String keyword;

    public SentenceResultViewModel(String hanzi, String pinyin, String english, SearchMode mode, String keyword) {
        this.hanzi = hanzi;
        this.pinyin = pinyin;
        this.english = english;
        this.mode = mode;
        this.keyword = keyword;
    }

    public String getHanzi() {
        return hanzi;
    }

    public String getPinyin() {
        return pinyin;
    }

    public String getEnglish() {
        return english;
    }

    public SearchMode getMode() {
        return mode;
    }

    public String getKeyword() {
        return keyword;
    }
}
