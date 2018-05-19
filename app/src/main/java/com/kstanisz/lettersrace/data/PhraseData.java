package com.kstanisz.lettersrace.data;

import com.kstanisz.lettersrace.model.Phrase;
import com.kstanisz.lettersrace.model.PhraseCategory;

import java.util.*;

public class PhraseData {
    public static final List<Phrase> data = Arrays.asList(
            new Phrase("DZIEŃ ŚWIRA", PhraseCategory.FILM),
            new Phrase("OJCIEC CHRZESTNY", PhraseCategory.FILM),
            new Phrase("GWIEZDNE WOJNY", PhraseCategory.FILM),
            new Phrase("NAJLEPSZĄ OBRONĄ JEST ATAK", PhraseCategory.DICTUM),
            new Phrase("BYĆ PRACOWITYM JAK PSZCZOŁA", PhraseCategory.DICTUM),
            new Phrase("TRENING CZYNI MISTRZA", PhraseCategory.DICTUM)
    );
}
