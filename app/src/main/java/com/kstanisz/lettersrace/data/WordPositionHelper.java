package com.kstanisz.lettersrace.data;

import com.kstanisz.lettersrace.model.WordPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WordPositionHelper {

    private final static int ROW_SIZE = 11;
    private final static int ROW_SIZE_PRESENTED = 13;

    private List<List<String>> rows = new ArrayList<>();

    public List<WordPosition> arrange(String text) {
        String[] tokens = text.split(" ");
        fillRowsArray(tokens);

        int startRow = rows.size() <= 2 ? 1 : 0;
        List<WordPosition> positions = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            positions.addAll(getRowPositions(startRow + i, rows.get(i)));
        }
        return positions;
    }

    private void fillRowsArray(String[] tokens) {
        int lettersInCurrentRow = 0;
        List<String> currentRow = new ArrayList<>();
        for (String token : tokens) {
            if (lettersInCurrentRow == 0) {
                currentRow.add(token);
                lettersInCurrentRow += token.length();
            } else if (tokens.length > 2 && currentRow.size() <= 1 && (lettersInCurrentRow + token.length() + 1) <= ROW_SIZE) {
                currentRow.add(token);
                lettersInCurrentRow += (token.length() + 1);
            } else {
                rows.add(currentRow);
                currentRow = Collections.singletonList(token);
                lettersInCurrentRow = token.length();
            }
        }
        rows.add(currentRow);
    }

    private List<WordPosition> getRowPositions(int row, List<String> tokens) {
        List<WordPosition> positions = new ArrayList<>();
        int currentColumn = getStartColumn(tokens);
        for (String token : tokens) {
            positions.add(new WordPosition(row, currentColumn));
            currentColumn += (token.length() + 1);
        }
        return positions;
    }

    private int getStartColumn(List<String> tokens) {
        int numberOfLetters = getNumberOfLettersInRow(tokens);
        return (ROW_SIZE_PRESENTED - numberOfLetters) / 2;
    }

    private int getNumberOfLettersInRow(List<String> tokens) {
        int numberOfLetters = 0;
        for (int i = 0; i < tokens.size(); i++) {
            numberOfLetters += tokens.get(i).length();
            if (i != tokens.size() - 1) {
                numberOfLetters++;
            }
        }
        return numberOfLetters;
    }
}