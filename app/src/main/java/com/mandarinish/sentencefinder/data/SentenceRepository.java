package com.mandarinish.sentencefinder.data;

import android.content.Context;

import com.mandarinish.sentencefinder.model.Sentence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SentenceRepository {
    private static final String ASSET_FILE = "sentence.csv";
    private static final int HANZI_COLUMN = 1;
    private static final int ENGLISH_COLUMN = 3;

    private final Context context;
    private final CsvParser csvParser = new CsvParser();

    public SentenceRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<Sentence> loadSentences() throws IOException {
        List<Sentence> loaded = new ArrayList<Sentence>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(ASSET_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> columns = csvParser.parseLine(line);
                if (columns.size() <= ENGLISH_COLUMN) {
                    continue;
                }

                String originalHanzi = columns.get(HANZI_COLUMN).trim();
                String english = columns.get(ENGLISH_COLUMN).trim();
                if (originalHanzi.isEmpty() || english.isEmpty()) {
                    continue;
                }

                loaded.add(new Sentence(originalHanzi, english));
            }
        }
        return loaded;
    }
}
