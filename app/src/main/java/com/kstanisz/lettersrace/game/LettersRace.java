package com.kstanisz.lettersrace.game;

import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.kstanisz.lettersrace.MainActivity;
import com.kstanisz.lettersrace.R;
import com.kstanisz.lettersrace.data.PhraseData;
import com.kstanisz.lettersrace.model.LetterPosition;
import com.kstanisz.lettersrace.model.Phrase;
import com.kstanisz.lettersrace.model.WordPosition;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class LettersRace {

    private MainActivity activity;

    private final static int[][] PHRASE_FIELDS = {
            {R.id.phrase_0_0, R.id.phrase_0_1, R.id.phrase_0_2, R.id.phrase_0_3, R.id.phrase_0_4, R.id.phrase_0_5, R.id.phrase_0_6, R.id.phrase_0_7, R.id.phrase_0_8, R.id.phrase_0_9, R.id.phrase_0_10, R.id.phrase_0_11, R.id.phrase_0_12},
            {R.id.phrase_1_0, R.id.phrase_1_1, R.id.phrase_1_2, R.id.phrase_1_3, R.id.phrase_1_4, R.id.phrase_1_5, R.id.phrase_1_6, R.id.phrase_1_7, R.id.phrase_1_8, R.id.phrase_1_9, R.id.phrase_1_10, R.id.phrase_1_11, R.id.phrase_1_12},
            {R.id.phrase_2_0, R.id.phrase_2_1, R.id.phrase_2_2, R.id.phrase_2_3, R.id.phrase_2_4, R.id.phrase_2_5, R.id.phrase_2_6, R.id.phrase_2_7, R.id.phrase_2_8, R.id.phrase_2_9, R.id.phrase_2_10, R.id.phrase_2_11, R.id.phrase_2_12},
            {R.id.phrase_3_0, R.id.phrase_3_1, R.id.phrase_3_2, R.id.phrase_3_3, R.id.phrase_3_4, R.id.phrase_3_5, R.id.phrase_3_6, R.id.phrase_3_7, R.id.phrase_3_8, R.id.phrase_3_9, R.id.phrase_3_10, R.id.phrase_3_11, R.id.phrase_3_12}
    };

    private int hash;

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
    private TextView categoryField;
    private Button buttonGuessPhrase;
    private TextView guessInfo;
    private View keysTable;
    private View gameOverPanel;
    private TextView gameOverTextMain;
    private TextView gameOverTextBlurb;

    private CountDownTimer guessingTimer;
    private CountDownTimer freezeTimer;


    public LettersRace(MainActivity activity) {
        this.activity = activity;
        this.timerField = activity.findViewById(R.id.guess_timer);
        this.categoryField = activity.findViewById(R.id.phrase_category);
        this.buttonGuessPhrase = activity.findViewById(R.id.button_guess_phrase);
        this.guessInfo = activity.findViewById(R.id.guess_info);
        this.keysTable = activity.findViewById(R.id.keys_table);
        this.gameOverPanel = activity.findViewById(R.id.game_over_panel);
        this.gameOverTextMain = activity.findViewById(R.id.game_over_text_main);
        this.gameOverTextBlurb = activity.findViewById(R.id.game_over_text_blurb);
    }

    public void startGame(int hash) {
        this.hash = hash;
        this.phrase = getPhrase();

        List<String> words = phrase.getWords();
        List<WordPosition> positions = phrase.getPositions();

        if (words.size() != positions.size()) {
            throw new RuntimeException("Number of words in phrase must be the same as number of their positions.");
        }

        categoryField.setText(phrase.getCategory().toUpperCase());

        setHiddenLetters(words, positions);

        runLetters();
    }

    private void runLetters() {
        runLettersHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (letters.isEmpty()) {
                    endGame(false, null);
                    return;
                }
                if (gameStopped) {
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

        guessingTimer = new CountDownTimer(45000, 1000) {
            public void onTick(long millisUntilFinished) {
                if (!guessing) {
                    return;
                }
                long seconds = millisUntilFinished / 1000;
                timerField.setText("00:" + (seconds > 9L ? seconds : "0" + seconds));
            }

            public void onFinish() {
                Toast.makeText(activity, "Czas minął!", Toast.LENGTH_LONG).show();
                cancelGuess();
            }
        }.start();
    }

    public void confirmGuess() {
        guessing = false;
        guessingTimer.cancel();

        boolean correct = checkIfCorrectPhrase();
        if (!correct) {
            Toast.makeText(activity, "Błędne hasło!", Toast.LENGTH_LONG).show();
            cancelGuess();
            return;
        }

        keysTable.setVisibility(View.GONE);
        timerField.setText("");

        activity.sendGuessSucceededMessage();
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
        buttonGuessPhrase.setBackgroundColor(ContextCompat.getColor(activity, R.color.GuessFreezeColor));
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
                buttonGuessPhrase.setBackgroundColor(ContextCompat.getColor(activity, R.color.GuessPhraseColor));
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

        guessInfo.setText("");
        guessInfo.setVisibility(View.GONE);

        timerField.setText("");

        buttonGuessPhrase.setVisibility(View.VISIBLE);
        buttonGuessPhrase.setText("Odgaduję!");
        buttonGuessPhrase.setBackgroundColor(ContextCompat.getColor(activity, R.color.GuessPhraseColor));

        keysTable.setVisibility(View.GONE);
        gameOverPanel.setVisibility(View.GONE);
    }

    public void endGame(boolean success, String winnerName) {
        buttonGuessPhrase.setVisibility(View.GONE);
        guessInfo.setText("");

        if (success) {
            gameOverTextMain.setText("Zwycięstwo!");
            if (winnerName != null) {
                if (winnerName.toLowerCase().endsWith("a")) {
                    gameOverTextBlurb.setText("Jako pierwsza odgadłaś hasło.");
                } else {
                    gameOverTextBlurb.setText("Jako pierwszy odgadłeś hasło.");
                }
            } else {
                gameOverTextBlurb.setText("Udało Ci się odgadnąć hasło.\nZdobywasz " + getWinnerScore() + " punktów.");
            }
        } else {
            gameOverTextMain.setText("Koniec gry!");
            if (winnerName != null) {
                gameOverTextBlurb.setText(winnerName + " odgadł" + (winnerName.toLowerCase().endsWith("a") ? "a" : "") + " hasło.");
            } else {
                gameOverTextBlurb.setText("Nie udało Ci się odgadnąć hasła.");
            }
            showAllLetters();
        }
        gameOverPanel.setVisibility(View.VISIBLE);
    }

    public void letterPressed(String letter) {
        if (lettersLeft.isEmpty()) {
            return;
        }

        if (letter.equals("\u232b")) {
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
        int total = PhraseData.data.size();
        return PhraseData.data.get(hash % total);
    }

    private void showOneLetter() {
        LetterPosition letterToShow = letters.remove(hash % letters.size());
        TextView field = activity.findViewById(letterToShow.getRid());
        String fullText = phrase.getText();
        field.setText(Character.toString(fullText.charAt(letterToShow.getIndex())));
    }

    private void showAllLetters() {
        String fullText = phrase.getText();
        for (LetterPosition letter : letters) {
            TextView field = activity.findViewById(letter.getRid());
            field.setText(Character.toString(fullText.charAt(letter.getIndex())));
        }
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

    private long getWinnerScore() {
        double allLettersCount = phrase.getText().replace(" ", "").length();
        double pressedLettersCount = pressedLetters.size();

        return Math.round((pressedLettersCount / allLettersCount) * 100.0);
    }
}