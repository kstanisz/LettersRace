package com.kstanisz.lettersrace.model;

import com.kstanisz.lettersrace.data.WordPositionHelper;

import java.util.Arrays;
import java.util.List;

public class Phrase {
    private String text;
    private PhraseCategory category;

    public Phrase(String text, PhraseCategory category) {
        this.text = text;
        this.category = category;
    }

    public String getText() {
        return text;
    }

    public String getCategory() {
        return category.getName();
    }

    public List<String> getWords() {
        return Arrays.asList(text.split(" "));
    }

    public List<WordPosition> getPositions() {
        WordPositionHelper wordPositionHelper = new WordPositionHelper();
        return wordPositionHelper.arrange(text);
    }
}
