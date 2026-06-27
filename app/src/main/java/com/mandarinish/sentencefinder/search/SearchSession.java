package com.mandarinish.sentencefinder.search;

import java.util.List;

public class SearchSession {
    final String keyword;
    final boolean simplifiedMode;
    final SearchMode searchMode;
    int sentenceCursor = 0;
    List<Integer> hanziCandidateIds;
    int hanziCandidateCursor = 0;
    boolean hanziCandidatesReady = false;
    private boolean exhausted = false;

    public SearchSession(String keyword, boolean simplifiedMode, SearchMode searchMode) {
        this.keyword = keyword;
        this.simplifiedMode = simplifiedMode;
        this.searchMode = searchMode;
    }

    public String getKeyword() {
        return keyword;
    }

    public SearchMode getSearchMode() {
        return searchMode;
    }

    public boolean isExhausted() {
        return exhausted;
    }

    void markExhausted(boolean exhausted) {
        this.exhausted = exhausted;
    }
}
