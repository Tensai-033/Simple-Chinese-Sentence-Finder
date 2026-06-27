package com.mandarinish.sentencefinder.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;

import com.mandarinish.sentencefinder.search.SearchMode;

import java.util.Locale;

class SearchResultHighlighter {
    private static final int HIGHLIGHT_COLOR = Color.rgb(255, 238, 140);

    CharSequence highlight(SentenceResultViewModel item, SearchMode textField, String text) {
        if (item.getMode() == SearchMode.PINYIN && textField == SearchMode.PINYIN) {
            return highlightWholeText(text);
        }
        if (item.getMode() != textField || item.getKeyword().isEmpty()) {
            return text;
        }
        return highlightExactMatches(text, item.getKeyword(), item.getMode() == SearchMode.DEFINITION);
    }

    private CharSequence highlightWholeText(String text) {
        SpannableString spannable = new SpannableString(text);
        spannable.setSpan(new BackgroundColorSpan(HIGHLIGHT_COLOR), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private CharSequence highlightExactMatches(String text, String keyword, boolean ignoreCase) {
        if (text.isEmpty() || keyword.isEmpty()) {
            return text;
        }

        SpannableString spannable = new SpannableString(text);
        String searchableText = ignoreCase ? text.toLowerCase(Locale.ROOT) : text;
        String searchableKeyword = ignoreCase ? keyword.toLowerCase(Locale.ROOT) : keyword;
        int index = searchableText.indexOf(searchableKeyword);

        while (index >= 0) {
            int end = index + searchableKeyword.length();
            spannable.setSpan(new BackgroundColorSpan(HIGHLIGHT_COLOR), index, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), index, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            index = searchableText.indexOf(searchableKeyword, end);
        }

        return spannable;
    }
}
