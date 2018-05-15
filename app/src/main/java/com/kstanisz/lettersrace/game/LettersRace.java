package com.kstanisz.lettersrace.game;

import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.kstanisz.lettersrace.MainActivity;
import com.kstanisz.lettersrace.R;
import com.kstanisz.lettersrace.model.LetterPosition;
import com.kstanisz.lettersrace.model.Phrase;
import com.kstanisz.lettersrace.model.WordPosition;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.*;

public class LettersRace {

    private MainActivity activity;

    private final static String TAG = "LettersRace";

    private final static int[][] PHRASE_FIELDS = {
            {R.id.phrase_0_0, R.id.phrase_0_1, R.id.phrase_0_2, R.id.phrase_0_3, R.id.phrase_0_4, R.id.phrase_0_5, R.id.phrase_0_6, R.id.phrase_0_7, R.id.phrase_0_8, R.id.phrase_0_9, R.id.phrase_0_10, R.id.phrase_0_11, R.id.phrase_0_12},
            {R.id.phrase_1_0, R.id.phrase_1_1, R.id.phrase_1_2, R.id.phrase_1_3, R.id.phrase_1_4, R.id.phrase_1_5, R.id.phrase_1_6, R.id.phrase_1_7, R.id.phrase_1_8, R.id.phrase_1_9, R.id.phrase_1_10, R.id.phrase_1_11, R.id.phrase_1_12},
            {R.id.phrase_2_0, R.id.phrase_2_1, R.id.phrase_2_2, R.id.phrase_2_3, R.id.phrase_2_4, R.id.phrase_2_5, R.id.phrase_2_6, R.id.phrase_2_7, R.id.phrase_2_8, R.id.phrase_2_9, R.id.phrase_2_10, R.id.phrase_2_11, R.id.phrase_2_12},
            {R.id.phrase_3_0, R.id.phrase_3_1, R.id.phrase_3_2, R.id.phrase_3_3, R.id.phrase_3_4, R.id.phrase_3_5, R.id.phrase_3_6, R.id.phrase_3_7, R.id.phrase_3_8, R.id.phrase_3_9, R.id.phrase_3_10, R.id.phrase_3_11, R.id.phrase_3_12}
    };

    private final BigInteger hash;

    private final ArrayList<LetterPosition> letters = new ArrayList<>();
    private ArrayList<LetterPosition> allLetters = new ArrayList<>();

    private Phrase phrase;

    private Stack<LetterPosition> pressedLetters = new Stack<>();
    private LinkedList<LetterPosition> lettersLeft = new LinkedList<>();

    private final Handler runLettersHandler = new Handler();
    private boolean gameStopped = false;
    private boolean guessing = false;
    private boolean freezeAfterGuess = false;

    private TextView timerField;
    private Button buttonGuessPhrase;
    private View keysTable;

    private CountDownTimer guessingTimer;
    private CountDownTimer freezeTimer;


    public LettersRace(MainActivity activity, String roomId) {
        this.activity = activity;
        this.hash = getHash(roomId);
        this.timerField = activity.findViewById(R.id.guess_timer);
        this.buttonGuessPhrase = activity.findViewById(R.id.button_guess_phrase);
        this.keysTable = activity.findViewById(R.id.keys_table);
    }

    public void startGame() {
        this.phrase = getPhrase();

        List<String> words = phrase.getWords();
        List<WordPosition> positions = phrase.getPositions();

        if (words.size() != positions.size()) {
            throw new RuntimeException("Error");
        }

        setHiddenLetters(words, positions);

        runLetters();
    }

    private void runLetters() {
        runLettersHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (letters.isEmpty() || gameStopped) {
                    return;
                }

                showOneLetter();
                runLettersHandler.postDelayed(this, 1500);
            }
        }, 1500);
    }

    public boolean canUserGuess() {
        return letters.size() > 0 && !freezeAfterGuess;
    }

    public void stopGame() {
        gameStopped = true;
    }

    public void startGuessing() {
        stopGame();
        guessing = true;

        keysTable.setVisibility(View.VISIBLE);
        buttonGuessPhrase.setVisibility(View.GONE);

        lettersLeft.addAll(letters);

        guessingTimer = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                if (!guessing) {
                    return;
                }
                long seconds = millisUntilFinished / 1000;
                timerField.setText("00:" + (seconds > 9L ? seconds : "0" + seconds));
            }

            public void onFinish() {
                resumeGame();
            }
        }.start();
    }

    public void confirmGuess() {
        guessing = false;
        guessingTimer.cancel();

        boolean correct = checkIfCorrectPhrase();
        if (!correct) {
            cancelGuess();
            return;
        }

        System.out.println("Correct phrase");

        keysTable.setVisibility(View.GONE);
        timerField.setText("");
    }

    public void cancelGuess() {
        guessing = false;
        guessingTimer.cancel();

        keysTable.setVisibility(View.GONE);
        buttonGuessPhrase.setVisibility(View.VISIBLE);
        timerField.setText("");

        lettersLeft.clear();
        for (LetterPosition letter : pressedLetters) {
            int rid = letter.getRid();
            TextView field = activity.findViewById(rid);
            field.setText("");
        }
        pressedLetters.clear();

        activity.sendGuessFailedMessage();

        freezeAfterGuess = true;
        freezeTimer = new CountDownTimer(6000, 1000) {

            public void onTick(long millisUntilFinished) {
                if (!freezeAfterGuess) {
                    return;
                }
                long seconds = millisUntilFinished / 1000;
                buttonGuessPhrase.setText(String.valueOf(seconds));
            }

            public void onFinish() {
                freezeAfterGuess = false;
                buttonGuessPhrase.setText("Odgaduję!");
            }
        }.start();
    }

    public void resumeGame() {
        gameStopped = false;
        runLetters();
    }

    public void resetGame() {
        runLettersHandler.removeCallbacksAndMessages(null);

        if (guessingTimer != null) {
            guessingTimer.cancel();
        }

        if (freezeTimer != null) {
            freezeTimer.cancel();
        }

        gameStopped = true;
        guessing = false;
        freezeAfterGuess = false;

        for (int[] row : PHRASE_FIELDS) {
            for (int rid : row) {
                if (rid == R.id.phrase_0_0 || rid == R.id.phrase_0_12 || rid == R.id.phrase_3_0 || rid == R.id.phrase_3_12) {
                    continue;
                }

                TextView field = activity.findViewById(rid);
                field.setText("");
                field.setBackgroundResource(R.drawable.phrase_empty_back);
            }
        }

        TextView guessInfo = activity.findViewById(R.id.guess_info);
        guessInfo.setText("");
        guessInfo.setVisibility(View.GONE);

        Button guessButton = activity.findViewById(R.id.button_guess_phrase);
        guessButton.setVisibility(View.VISIBLE);

        View keysTable = activity.findViewById(R.id.keys_table);
        keysTable.setVisibility(View.GONE);
    }

    public void letterPressed(String letter) {
        System.out.println("User pressed: " + letter + " letter");
        if (lettersLeft.isEmpty()) {
            return;
        }

        if (letter.equals("BCK")) {
            removeLastPressedLetter();
            return;
        }

        LetterPosition positionToShow = lettersLeft.removeFirst();
        int rid = positionToShow.getRid();
        TextView field = activity.findViewById(rid);
        field.setText(letter);

        pressedLetters.push(positionToShow);
    }

    private void removeLastPressedLetter() {
        if (pressedLetters.isEmpty()) {
            return;
        }

        LetterPosition lastPressedLetter = pressedLetters.pop();
        int rid = lastPressedLetter.getRid();
        TextView field = activity.findViewById(rid);
        field.setText("");

        lettersLeft.addFirst(lastPressedLetter);
    }

    private boolean checkIfCorrectPhrase() {
        String text = phrase.getText();
        for (LetterPosition letter : allLetters) {
            int index = letter.getIndex();
            int rid = letter.getRid();

            TextView field = activity.findViewById(rid);
            String fieldText = field.getText().toString();
            if (StringUtils.isEmpty(fieldText)) {
                return false;
            }

            String correctLetter = String.valueOf(text.charAt(index));
            if (!fieldText.equals(correctLetter)) {
                return false;
            }
        }

        return true;
    }


    private Phrase getPhrase() {
        return new Phrase("PIERWSZY TEST GRY", "TEST", "1,2;2,2;2,7");
        //return new Phrase("KOCHAM CIĘ PAULINKO", "TEST", "1,1;1,8;2,2");
    }

    private void showOneLetter() {
        LetterPosition letterToShow = letters.remove(hash.mod(BigInteger.valueOf(letters.size())).intValue());
        TextView field = activity.findViewById(letterToShow.getRid());
        String fullText = phrase.getText();
        field.setText(Character.toString(fullText.charAt(letterToShow.getIndex())));
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
        allLetters.addAll(letters);
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