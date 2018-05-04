package com.kstanisz.lettersrace.model;

public class WordPosition {
    private int row;
    private int column;

    public WordPosition(){}

    public WordPosition(int row, int column){
        this.row = row;
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }
}
