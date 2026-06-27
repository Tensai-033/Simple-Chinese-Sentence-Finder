package com.mandarinish.sentencefinder.search;

import java.util.List;
import java.util.Map;

class HanziIndex {
    final Map<Character, List<Integer>> postingsByChar;

    HanziIndex(Map<Character, List<Integer>> postingsByChar) {
        this.postingsByChar = postingsByChar;
    }
}
