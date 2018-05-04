package com.kstanisz.lettersrace.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Phrase {
    private String text;
    private String category;
    private String positions;

    public Phrase(String text, String category, String positions) {
        this.text = text;
        this.category = category;
        this.positions = positions;
    }

    public String getText() {
        return text;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getWords(){
        return Arrays.asList(text.split(" "));
    }

    public List<WordPosition> getPositions() {
        if (positions == null || positions.isEmpty()) {
            return Collections.emptyList();
        }

        List<WordPosition> wordPositions = new ArrayList<>();
        String[] separatedPositions = positions.split(";");
        for (String position : separatedPositions) {
            String[] rowColumnPair = position.split(",");
            int row = Integer.parseInt(rowColumnPair[0]);
            int column = Integer.parseInt(rowColumnPair[1]);
            wordPositions.add(new WordPosition(row, column));
        }

        return wordPositions;
    }
}
