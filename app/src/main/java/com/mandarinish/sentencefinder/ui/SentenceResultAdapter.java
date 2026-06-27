package com.mandarinish.sentencefinder.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mandarinish.sentencefinder.R;
import com.mandarinish.sentencefinder.search.SearchMode;

import java.util.ArrayList;

public class SentenceResultAdapter extends ArrayAdapter<SentenceResultViewModel> {
    private final Activity activity;
    private final SearchResultHighlighter highlighter = new SearchResultHighlighter();

    public SentenceResultAdapter(Activity activity) {
        super(activity, R.layout.item_sentence, new ArrayList<SentenceResultViewModel>());
        this.activity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = activity.getLayoutInflater().inflate(R.layout.item_sentence, parent, false);
        }

        SentenceResultViewModel item = getItem(position);
        TextView hanziText = view.findViewById(R.id.hanziText);
        TextView pinyinText = view.findViewById(R.id.pinyinText);
        TextView englishText = view.findViewById(R.id.englishText);

        if (item != null) {
            hanziText.setText(highlighter.highlight(item, SearchMode.HANZI, item.getHanzi()));
            pinyinText.setText(highlighter.highlight(item, SearchMode.PINYIN, item.getPinyin()));
            englishText.setText(highlighter.highlight(item, SearchMode.DEFINITION, item.getEnglish()));
        }

        return view;
    }
}
