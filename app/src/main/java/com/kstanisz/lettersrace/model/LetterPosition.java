package com.kstanisz.lettersrace.model;

public class LetterPosition {
    private int index;
    private int rid;

    public LetterPosition(int index, int rid){
        this.index = index;
        this.rid = rid;
    }

    public int getIndex() {
        return index;
    }

    public int getRid() {
        return rid;
    }
}
