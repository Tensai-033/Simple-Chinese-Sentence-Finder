package com.mandarinish.sentencefinder;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.mandarinish.sentencefinder.data.SentenceRepository;
import com.mandarinish.sentencefinder.model.SearchResult;
import com.mandarinish.sentencefinder.model.Sentence;
import com.mandarinish.sentencefinder.search.SearchMode;
import com.mandarinish.sentencefinder.search.SearchSession;
import com.mandarinish.sentencefinder.search.SentenceSearchEngine;
import com.mandarinish.sentencefinder.text.ChineseTextTools;
import com.mandarinish.sentencefinder.ui.SentenceResultAdapter;
import com.mandarinish.sentencefinder.ui.SentenceResultViewModel;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PAGE_SIZE = 10;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ChineseTextTools textTools;
    private SentenceRepository sentenceRepository;
    private SentenceSearchEngine searchEngine;

    private EditText searchInput;
    private RadioButton simplifiedRadio;
    private RadioButton pinyinSearchRadio;
    private RadioButton definitionSearchRadio;
    private TextView statusText;
    private Button searchButton;
    private Button loadMoreButton;
    private Button reloadDatasetButton;
    private SentenceResultAdapter adapter;

    private SearchSession activeSearchSession;
    private SearchMode activeSearchMode = SearchMode.HANZI;
    private String activeKeyword = "";
    private int visibleResultCount = 0;
    private boolean datasetLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textTools = new ChineseTextTools();
        sentenceRepository = new SentenceRepository(this);
        searchEngine = new SentenceSearchEngine(textTools);

        bindViews();
        loadDataset();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void bindViews() {
        searchInput = findViewById(R.id.searchInput);
        simplifiedRadio = findViewById(R.id.simplifiedRadio);
        pinyinSearchRadio = findViewById(R.id.pinyinSearchRadio);
        definitionSearchRadio = findViewById(R.id.definitionSearchRadio);
        searchButton = findViewById(R.id.searchButton);
        loadMoreButton = findViewById(R.id.loadMoreButton);
        reloadDatasetButton = findViewById(R.id.reloadDatasetButton);
        statusText = findViewById(R.id.statusText);
        ListView resultsList = findViewById(R.id.resultsList);

        adapter = new SentenceResultAdapter(this);
        resultsList.setAdapter(adapter);

        searchButton.setOnClickListener(view -> startSearch());
        loadMoreButton.setOnClickListener(view -> fetchNextResultsPage(false));
        reloadDatasetButton.setOnClickListener(view -> loadDataset());
        searchInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                startSearch();
                return true;
            }
            return false;
        });
    }

    private void loadDataset() {
        if (datasetLoading) {
            return;
        }

        datasetLoading = true;
        resetSearchState();
        setSearchingEnabled(false);
        reloadDatasetButton.setEnabled(false);
        statusText.setText("Memuat dataset...");

        executor.execute(() -> {
            long startTime = System.currentTimeMillis();
            List<Sentence> loaded;
            try {
                loaded = sentenceRepository.loadSentences();
            } catch (Exception e) {
                String message = e.getClass().getSimpleName() + ": " + e.getMessage();
                runOnUiThread(() -> {
                    datasetLoading = false;
                    setSearchingEnabled(!searchEngine.isEmpty());
                    reloadDatasetButton.setEnabled(true);
                    statusText.setText("Dataset gagal dimuat. " + message);
                });
                return;
            }

            long elapsedMs = System.currentTimeMillis() - startTime;
            runOnUiThread(() -> {
                searchEngine.setSentences(loaded);
                datasetLoading = false;
                setSearchingEnabled(true);
                reloadDatasetButton.setEnabled(true);
                statusText.setText(String.format(Locale.US, "Dataset siap: %,d kalimat dalam %.1f detik. Masukkan kata untuk mencari.", searchEngine.size(), elapsedMs / 1000.0));
            });
        });
    }

    private void startSearch() {
        if (datasetLoading) {
            statusText.setText("Dataset masih dimuat. Tunggu sebentar.");
            return;
        }
        if (searchEngine.isEmpty()) {
            statusText.setText("Dataset belum siap.");
            return;
        }

        String rawKeyword = searchInput.getText().toString().trim();
        if (rawKeyword.isEmpty()) {
            resetSearchState();
            statusText.setText("Masukkan kosakata Mandarin terlebih dahulu.");
            return;
        }

        boolean simplifiedMode = simplifiedRadio.isChecked();
        SearchMode searchMode = getSelectedSearchMode();
        String keyword = searchEngine.prepareKeyword(rawKeyword, simplifiedMode, searchMode);

        resetSearchState();
        activeKeyword = keyword;
        activeSearchMode = searchMode;
        activeSearchSession = searchEngine.newSession(keyword, simplifiedMode, searchMode);
        fetchNextResultsPage(true);
    }

    private SearchMode getSelectedSearchMode() {
        if (pinyinSearchRadio.isChecked()) {
            return SearchMode.PINYIN;
        }
        if (definitionSearchRadio.isChecked()) {
            return SearchMode.DEFINITION;
        }
        return SearchMode.HANZI;
    }

    private void fetchNextResultsPage(boolean firstPage) {
        SearchSession session = activeSearchSession;
        if (session == null || session.isExhausted()) {
            loadMoreButton.setVisibility(View.GONE);
            return;
        }

        setSearchingEnabled(false);
        statusText.setText(firstPage ? "Mencari '" + session.getKeyword() + "'..." : "Mencari hasil berikutnya...");

        executor.execute(() -> {
            List<SearchResult> results;
            try {
                results = searchEngine.findNext(session, PAGE_SIZE);
            } catch (Exception e) {
                String message = e.getClass().getSimpleName() + ": " + e.getMessage();
                runOnUiThread(() -> {
                    setSearchingEnabled(true);
                    statusText.setText("Search gagal. " + message);
                });
                return;
            }

            runOnUiThread(() -> renderSearchResults(session, results));
        });
    }

    private void renderSearchResults(SearchSession session, List<SearchResult> results) {
        if (activeSearchSession != session) {
            return;
        }

        setSearchingEnabled(true);
        if (results.isEmpty()) {
            loadMoreButton.setVisibility(View.GONE);
            if (visibleResultCount == 0) {
                statusText.setText("Kata '" + session.getKeyword() + "' tidak ditemukan.");
            } else {
                statusText.setText(String.format(
                        Locale.US,
                        "%s: menampilkan %,d contoh kalimat untuk '%s'. Tidak ada hasil tambahan.",
                        getSearchModeLabel(activeSearchMode),
                        visibleResultCount,
                        activeKeyword
                ));
            }
            return;
        }

        appendResults(results);
        updateSearchStatus();
    }

    private void appendResults(List<SearchResult> results) {
        for (SearchResult result : results) {
            String pinyin = result.getSentence().getPinyin(result.isSimplifiedMode(), textTools);
            result.setPinyin(pinyin);
            adapter.add(new SentenceResultViewModel(
                    result.getHanzi(),
                    formatPinyin(result),
                    result.getEnglish(),
                    result.getMode(),
                    activeKeyword
            ));
        }

        visibleResultCount += results.size();
        adapter.notifyDataSetChanged();
    }

    private String formatPinyin(SearchResult result) {
        if (result.getMode() == SearchMode.PINYIN) {
            return String.format(Locale.US, "%s  |  Pinyin score: %.0f%%", result.getPinyin(), result.getScore() * 100);
        }
        return result.getPinyin();
    }

    private void updateSearchStatus() {
        boolean hasMoreResults = activeSearchSession != null && !activeSearchSession.isExhausted();
        loadMoreButton.setVisibility(hasMoreResults ? View.VISIBLE : View.GONE);
        statusText.setText(String.format(
                Locale.US,
                "%s: menampilkan %,d contoh kalimat untuk '%s'.",
                getSearchModeLabel(activeSearchMode),
                visibleResultCount,
                activeKeyword
        ));
    }

    private String getSearchModeLabel(SearchMode searchMode) {
        if (searchMode == SearchMode.PINYIN) {
            return "Fuzzy pinyin";
        }
        if (searchMode == SearchMode.DEFINITION) {
            return "Definisi";
        }
        return "Hanzi";
    }

    private void resetSearchState() {
        adapter.clear();
        visibleResultCount = 0;
        activeKeyword = "";
        activeSearchMode = SearchMode.HANZI;
        activeSearchSession = null;
        loadMoreButton.setVisibility(View.GONE);
    }

    private void setSearchingEnabled(boolean enabled) {
        searchButton.setEnabled(enabled);
        reloadDatasetButton.setEnabled(enabled);
        if (!enabled) {
            loadMoreButton.setVisibility(View.GONE);
        }
    }
}
