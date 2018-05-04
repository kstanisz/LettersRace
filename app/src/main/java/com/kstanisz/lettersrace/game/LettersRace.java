package com.kstanisz.lettersrace.game;

import android.os.Handler;
import android.widget.TextView;
import com.kstanisz.lettersrace.MainActivity;
import com.kstanisz.lettersrace.R;
import com.kstanisz.lettersrace.model.LetterPosition;
import com.kstanisz.lettersrace.model.Phrase;
import com.kstanisz.lettersrace.model.WordPosition;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LettersRace {

    private MainActivity activity;

    private final static int[][] PHRASE_FIELDS = {
            {R.id.phrase_0_0, R.id.phrase_0_1, R.id.phrase_0_2, R.id.phrase_0_3, R.id.phrase_0_4, R.id.phrase_0_5, R.id.phrase_0_6, R.id.phrase_0_7, R.id.phrase_0_8, R.id.phrase_0_9, R.id.phrase_0_10, R.id.phrase_0_11, R.id.phrase_0_12},
            {R.id.phrase_1_0, R.id.phrase_1_1, R.id.phrase_1_2, R.id.phrase_1_3, R.id.phrase_1_4, R.id.phrase_1_5, R.id.phrase_1_6, R.id.phrase_1_7, R.id.phrase_1_8, R.id.phrase_1_9, R.id.phrase_1_10, R.id.phrase_1_11, R.id.phrase_1_12},
            {R.id.phrase_2_0, R.id.phrase_2_1, R.id.phrase_2_2, R.id.phrase_2_3, R.id.phrase_2_4, R.id.phrase_2_5, R.id.phrase_2_6, R.id.phrase_2_7, R.id.phrase_2_8, R.id.phrase_2_9, R.id.phrase_2_10, R.id.phrase_2_11, R.id.phrase_2_12},
            {R.id.phrase_3_0, R.id.phrase_3_1, R.id.phrase_3_2, R.id.phrase_3_3, R.id.phrase_3_4, R.id.phrase_3_5, R.id.phrase_3_6, R.id.phrase_3_7, R.id.phrase_3_8, R.id.phrase_3_9, R.id.phrase_3_10, R.id.phrase_3_11, R.id.phrase_3_12}
    };
    private final BigInteger hash;

    private final List<LetterPosition> letters = new ArrayList<>();
    private String text;
    private boolean gameStopped = false;

    public LettersRace(MainActivity activity, String roomId) {
        this.activity = activity;
        this.hash = getHash(roomId);
    }

    public void startGame() {
        Phrase phrase = getPhrase();
        text = phrase.getText();

        List<String> words = phrase.getWords();
        List<WordPosition> positions = phrase.getPositions();
        if (words.size() != positions.size()) {
            throw new RuntimeException("Error");
        }

        setHiddenLetters(words, positions);

        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (letters.isEmpty()) {
                    return;
                }

                if (!gameStopped) {
                    showOneLetter();
                }

                h.postDelayed(this, 1000);
            }
        }, 1000);
    }

    public boolean canUserGuess() {
        return letters.size() > 0;
    }

    public void stopGame(){
        gameStopped = true;
    }

    public void resumeGame(){
        gameStopped = false;
    }


    private Phrase getPhrase() {
        //return new Phrase("PIERWSZY TEST GRY", "TEST", "1,2;2,2;2,7");
        return new Phrase("KOCHAM CIĘ PAULINKO", "TEST", "1,1;1,8;2,2");
    }

    private void showOneLetter() {
        LetterPosition letterToShow = letters.remove(hash.mod(BigInteger.valueOf(letters.size())).intValue());
        TextView field = activity.findViewById(letterToShow.getRid());
        field.setText(Character.toString(text.charAt(letterToShow.getIndex())));
    }

    private void setHiddenLetters(List<String> words, List<WordPosition> positions) {
        int index = 0;
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            WordPosition position = positions.get(i);
            for (int j = 0; j < word.length(); j++) {
                int rid = PHRASE_FIELDS[position.getRow()][position.getColumn() + j];
                TextView field = activity.findViewById(rid);
                field.setBackgroundResource(R.drawable.phrase_letter_back);

                letters.add(new LetterPosition(index, rid));
                index++;
            }
            index++;
        }
    }

    private BigInteger getHash(String str) {
        if (str == null) {
            Random random = new Random();
            int min = 10000000, max = Integer.MAX_VALUE;
            int hash = random.nextInt(max - min + 1) + min;
            return BigInteger.valueOf(hash);
        }

        str = str.substring(0, 10);
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            sb.append((int) c);
        }
        return new BigInteger(sb.toString());
    }
}
