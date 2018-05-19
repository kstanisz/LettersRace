package com.kstanisz.lettersrace.model;

public enum PhraseCategory {
    FILM("FILM"), DICTUM("POWIEDZENIA I PRZYSŁOWIA");

    private final String name;

    PhraseCategory(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }
}
