package com.mandarinish.sentencefinder.data;

import java.util.ArrayList;
import java.util.List;

public class CsvParser {
    public List<String> parseLine(String line) {
        List<String> columns = new ArrayList<String>();
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
}
