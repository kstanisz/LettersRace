package com.kstanisz.lettersrace.model;

public enum PhraseCategory {
    FILM("FILM"), POWIEDZENIA_I_PRZYSLOWIA("POWIEDZENIA I PRZYSŁOWIA");

    private final String name;

    PhraseCategory(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }
}
