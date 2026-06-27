package com.mandarinish.sentencefinder.search;

import com.mandarinish.sentencefinder.model.SearchResult;
import com.mandarinish.sentencefinder.model.Sentence;
import com.mandarinish.sentencefinder.text.ChineseTextTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SentenceSearchEngine {
    private final ChineseTextTools textTools;
    private final PinyinFuzzyMatcher pinyinFuzzyMatcher;
    private final KmpMatcher kmpMatcher = new KmpMatcher();
    private List<Sentence> sentences = new ArrayList<Sentence>();
    private HanziIndex simplifiedHanziIndex;
    private HanziIndex traditionalHanziIndex;

    public SentenceSearchEngine(ChineseTextTools textTools) {
        this.textTools = textTools;
        this.pinyinFuzzyMatcher = new PinyinFuzzyMatcher(textTools);
    }

    public void setSentences(List<Sentence> sentences) {
        this.sentences = sentences;
        simplifiedHanziIndex = null;
        traditionalHanziIndex = null;
    }

    public boolean isEmpty() {
        return sentences.isEmpty();
    }

    public int size() {
        return sentences.size();
    }

    public String prepareKeyword(String rawKeyword, boolean simplifiedMode, SearchMode searchMode) {
        if (searchMode == SearchMode.HANZI) {
            return simplifiedMode ? textTools.toSimplified(rawKeyword) : textTools.toTraditional(rawKeyword);
        }
        if (searchMode == SearchMode.PINYIN) {
            return textTools.normalizePinyin(rawKeyword);
        }
        return rawKeyword.toLowerCase(Locale.ROOT);
    }

    public SearchSession newSession(String keyword, boolean simplifiedMode, SearchMode searchMode) {
        return new SearchSession(keyword, simplifiedMode, searchMode);
    }

    public List<SearchResult> findNext(SearchSession session, int pageSize) {
        if (session.getSearchMode() == SearchMode.HANZI) {
            return findNextHanziMatches(session, pageSize);
        }

        List<SearchResult> results = new ArrayList<SearchResult>();
        while (session.sentenceCursor < sentences.size() && results.size() < pageSize) {
            Sentence sentence = sentences.get(session.sentenceCursor);
            session.sentenceCursor++;

            if (session.searchMode == SearchMode.DEFINITION && kmpMatcher.contains(sentence.getEnglish().toLowerCase(Locale.ROOT), session.keyword)) {
                String hanzi = sentence.getHanzi(session.simplifiedMode, textTools);
                results.add(new SearchResult(sentence, hanzi, sentence.getEnglish(), SearchMode.DEFINITION, 1.0, session.simplifiedMode));
            } else if (session.searchMode == SearchMode.PINYIN) {
                String pinyin = sentence.getPinyin(session.simplifiedMode, textTools);
                double score = pinyinFuzzyMatcher.findBestSimilarity(session.keyword, pinyin);
                if (score >= PinyinFuzzyMatcher.DEFAULT_THRESHOLD) {
                    String hanzi = sentence.getHanzi(session.simplifiedMode, textTools);
                    results.add(new SearchResult(sentence, hanzi, sentence.getEnglish(), SearchMode.PINYIN, score, session.simplifiedMode));
                }
            }
        }

        session.markExhausted(session.sentenceCursor >= sentences.size());
        return results;
    }

    private List<SearchResult> findNextHanziMatches(SearchSession session, int pageSize) {
        List<SearchResult> results = new ArrayList<SearchResult>();
        if (session.keyword.isEmpty()) {
            session.markExhausted(true);
            return results;
        }

        if (!session.hanziCandidatesReady) {
            session.hanziCandidateIds = getHanziCandidateIds(session.keyword, session.simplifiedMode);
            session.hanziCandidatesReady = true;
            if (session.hanziCandidateIds == null || session.hanziCandidateIds.isEmpty()) {
                session.markExhausted(true);
                return results;
            }
        }

        while (session.hanziCandidateCursor < session.hanziCandidateIds.size() && results.size() < pageSize) {
            int sentenceId = session.hanziCandidateIds.get(session.hanziCandidateCursor);
            session.hanziCandidateCursor++;
            Sentence sentence = sentences.get(sentenceId);
            String hanzi = sentence.getHanzi(session.simplifiedMode, textTools);
            if (hanzi.contains(session.keyword)) {
                results.add(new SearchResult(sentence, hanzi, sentence.getEnglish(), SearchMode.HANZI, 1.0, session.simplifiedMode));
            }
        }

        session.markExhausted(session.hanziCandidateCursor >= session.hanziCandidateIds.size());
        return results;
    }

    private List<Integer> getHanziCandidateIds(String keyword, boolean simplifiedMode) {
        HanziIndex index = ensureHanziIndex(simplifiedMode);
        Set<Character> uniqueQueryChars = new HashSet<Character>();
        for (int i = 0; i < keyword.length(); i++) {
            char value = keyword.charAt(i);
            if (!Character.isWhitespace(value)) {
                uniqueQueryChars.add(value);
            }
        }

        List<Integer> candidateIds = null;
        for (Character value : uniqueQueryChars) {
            List<Integer> postings = index.postingsByChar.get(value);
            if (postings == null) {
                return null;
            }
            if (candidateIds == null || postings.size() < candidateIds.size()) {
                candidateIds = postings;
            }
        }
        return candidateIds;
    }

    private HanziIndex ensureHanziIndex(boolean simplifiedMode) {
        HanziIndex existingIndex = simplifiedMode ? simplifiedHanziIndex : traditionalHanziIndex;
        if (existingIndex != null) {
            return existingIndex;
        }

        HanziIndex newIndex = buildHanziIndex(simplifiedMode);
        if (simplifiedMode) {
            simplifiedHanziIndex = newIndex;
        } else {
            traditionalHanziIndex = newIndex;
        }
        return newIndex;
    }

    private HanziIndex buildHanziIndex(boolean simplifiedMode) {
        Map<Character, List<Integer>> postingsByChar = new HashMap<Character, List<Integer>>();
        for (int sentenceId = 0; sentenceId < sentences.size(); sentenceId++) {
            String hanzi = sentences.get(sentenceId).getHanzi(simplifiedMode, textTools);
            Set<Character> seenInSentence = new HashSet<Character>();

            for (int charIndex = 0; charIndex < hanzi.length(); charIndex++) {
                char value = hanzi.charAt(charIndex);
                if (Character.isWhitespace(value) || seenInSentence.contains(value)) {
                    continue;
                }

                seenInSentence.add(value);
                List<Integer> postings = postingsByChar.get(value);
                if (postings == null) {
                    postings = new ArrayList<Integer>();
                    postingsByChar.put(value, postings);
                }
                postings.add(sentenceId);
            }
        }
        return new HanziIndex(postingsByChar);
    }
}
