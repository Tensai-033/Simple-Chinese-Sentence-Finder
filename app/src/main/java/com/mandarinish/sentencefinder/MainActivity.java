package com.mandarinish.sentencefinder;

import android.app.Activity;
import android.icu.text.Transliterator;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PAGE_SIZE = 10;

    private final List<Sentence> sentences = new ArrayList<>();
    private final List<SentenceMatch> currentMatches = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Transliterator simplifiedConverter;
    private Transliterator traditionalConverter;
    private Transliterator pinyinConverter;

    private EditText searchInput;
    private RadioButton simplifiedRadio;
    private TextView statusText;
    private Button loadMoreButton;
    private ArrayAdapter<SentenceResult> adapter;
    private int visibleResultCount = 0;
    private String activeKeyword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initTransliterators();
        bindViews();
        loadDataset();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void initTransliterators() {
        simplifiedConverter = createTransliterator("Traditional-Simplified");
        traditionalConverter = createTransliterator("Simplified-Traditional");
        pinyinConverter = createTransliterator("Han-Latin/Names");
    }

    private Transliterator createTransliterator(String id) {
        try {
            return Transliterator.getInstance(id);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void bindViews() {
        searchInput = findViewById(R.id.searchInput);
        simplifiedRadio = findViewById(R.id.simplifiedRadio);
        Button searchButton = findViewById(R.id.searchButton);
        loadMoreButton = findViewById(R.id.loadMoreButton);
        statusText = findViewById(R.id.statusText);
        ListView resultsList = findViewById(R.id.resultsList);

        adapter = new ArrayAdapter<SentenceResult>(this, R.layout.item_sentence, new ArrayList<SentenceResult>()) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = convertView;
                if (view == null) {
                    view = getLayoutInflater().inflate(R.layout.item_sentence, parent, false);
                }

                SentenceResult item = getItem(position);
                TextView hanziText = view.findViewById(R.id.hanziText);
                TextView pinyinText = view.findViewById(R.id.pinyinText);
                TextView englishText = view.findViewById(R.id.englishText);

                if (item != null) {
                    hanziText.setText(item.hanzi);
                    pinyinText.setText(item.pinyin);
                    englishText.setText(item.english);
                }

                return view;
            }
        };
        resultsList.setAdapter(adapter);

        searchButton.setOnClickListener(view -> performSearch());
        loadMoreButton.setOnClickListener(view -> showNextResultsPage());
        searchInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void loadDataset() {
        statusText.setText("Memuat dataset...");
        executor.execute(() -> {
            List<Sentence> loaded;
            try {
                loaded = readSentencesFromAssets();
            } catch (IOException e) {
                runOnUiThread(() -> statusText.setText("Dataset gagal dimuat: " + e.getMessage()));
                return;
            }

            runOnUiThread(() -> {
                sentences.clear();
                sentences.addAll(loaded);
                statusText.setText(String.format(Locale.US, "Dataset siap: %,d kalimat. Masukkan kata untuk mencari.", sentences.size()));
            });
        });
    }

    private List<Sentence> readSentencesFromAssets() throws IOException {
        List<Sentence> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("sentence.csv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> columns = parseCsvLine(line);
                if (columns.size() < 4) {
                    continue;
                }

                String originalHanzi = columns.get(1).trim();
                String english = columns.get(3).trim();
                if (originalHanzi.isEmpty() || english.isEmpty()) {
                    continue;
                }

                String simplified = convert(originalHanzi, simplifiedConverter);
                String traditional = convert(originalHanzi, traditionalConverter);
                loaded.add(new Sentence(simplified, traditional, english));
            }
        }
        return loaded;
    }

    private List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                columns.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        columns.add(current.toString());
        return columns;
    }

    private void performSearch() {
        if (sentences.isEmpty()) {
            statusText.setText("Dataset belum siap.");
            return;
        }

        String rawKeyword = searchInput.getText().toString().trim();
        if (rawKeyword.isEmpty()) {
            adapter.clear();
            currentMatches.clear();
            visibleResultCount = 0;
            activeKeyword = "";
            loadMoreButton.setVisibility(View.GONE);
            statusText.setText("Masukkan kosakata Mandarin terlebih dahulu.");
            return;
        }

        boolean simplifiedMode = simplifiedRadio.isChecked();
        String keyword = convert(rawKeyword, simplifiedMode ? simplifiedConverter : traditionalConverter);
        List<SentenceMatch> matches = search(keyword, simplifiedMode);

        adapter.clear();
        currentMatches.clear();
        currentMatches.addAll(matches);
        visibleResultCount = 0;
        activeKeyword = keyword;

        if (currentMatches.isEmpty()) {
            loadMoreButton.setVisibility(View.GONE);
            statusText.setText("Kata '" + keyword + "' tidak ditemukan.");
        } else {
            showNextResultsPage();
        }
    }

    private List<SentenceMatch> search(String keyword, boolean simplifiedMode) {
        List<SentenceMatch> results = new ArrayList<>();
        for (Sentence sentence : sentences) {
            String hanzi = simplifiedMode ? sentence.simplified : sentence.traditional;
            if (hanzi.contains(keyword)) {
                results.add(new SentenceMatch(hanzi, sentence.english));
            }
        }
        return results;
    }

    private void showNextResultsPage() {
        int nextVisibleCount = Math.min(visibleResultCount + PAGE_SIZE, currentMatches.size());
        for (int i = visibleResultCount; i < nextVisibleCount; i++) {
            SentenceMatch match = currentMatches.get(i);
            adapter.add(new SentenceResult(match.hanzi, toPinyin(match.hanzi), match.english));
        }

        visibleResultCount = nextVisibleCount;
        adapter.notifyDataSetChanged();
        updateSearchStatus();
    }

    private void updateSearchStatus() {
        boolean hasMoreResults = visibleResultCount < currentMatches.size();
        loadMoreButton.setVisibility(hasMoreResults ? View.VISIBLE : View.GONE);
        statusText.setText(String.format(
                Locale.US,
                "Menampilkan %,d dari %,d contoh kalimat untuk '%s'.",
                visibleResultCount,
                currentMatches.size(),
                activeKeyword
        ));
    }

    private String convert(String value, Transliterator transliterator) {
        if (transliterator == null) {
            return value;
        }
        return transliterator.transliterate(value);
    }

    private String toPinyin(String hanzi) {
        String pinyin = convert(hanzi, pinyinConverter);
        return pinyin.replaceAll("\\s+", " ").trim();
    }

    private static class Sentence {
        final String simplified;
        final String traditional;
        final String english;

        Sentence(String simplified, String traditional, String english) {
            this.simplified = simplified;
            this.traditional = traditional;
            this.english = english;
        }
    }

    private static class SentenceMatch {
        final String hanzi;
        final String english;

        SentenceMatch(String hanzi, String english) {
            this.hanzi = hanzi;
            this.english = english;
        }
    }

    private static class SentenceResult {
        final String hanzi;
        final String pinyin;
        final String english;

        SentenceResult(String hanzi, String pinyin, String english) {
            this.hanzi = hanzi;
            this.pinyin = pinyin;
            this.english = english;
        }
    }
}
