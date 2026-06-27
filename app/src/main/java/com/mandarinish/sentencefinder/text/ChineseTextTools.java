package com.mandarinish.sentencefinder.text;

import android.icu.text.Transliterator;

import java.util.Locale;

public class ChineseTextTools {
    private final Transliterator simplifiedConverter;
    private final Transliterator traditionalConverter;
    private final Transliterator pinyinConverter;

    public ChineseTextTools() {
        simplifiedConverter = createTransliterator("Traditional-Simplified");
        traditionalConverter = createTransliterator("Simplified-Traditional");
        pinyinConverter = createTransliterator("Han-Latin/Names");
    }

    public String toSimplified(String value) {
        return convert(value, simplifiedConverter);
    }

    public String toTraditional(String value) {
        return convert(value, traditionalConverter);
    }

    public String toPinyin(String hanzi) {
        return convert(hanzi, pinyinConverter).replaceAll("\\s+", " ").trim();
    }

    public String normalizePinyin(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private Transliterator createTransliterator(String id) {
        try {
            return Transliterator.getInstance(id);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String convert(String value, Transliterator transliterator) {
        if (transliterator == null) {
            return value;
        }
        return transliterator.transliterate(value);
    }
}
