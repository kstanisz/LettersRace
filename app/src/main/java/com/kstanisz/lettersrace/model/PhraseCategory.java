package com.kstanisz.lettersrace.model;

public enum PhraseCategory {
    FILM("FILM"), POWIEDZENIA_I_PRZYSLOWIA("POWIEDZENIA I PRZYSŁOWIA"), LITERATURA("LITERATURA"),
    SPORTOWIEC("SPORTOWIEC"), POLSCY_PISARZE_I_POECI("POLSCY PISARZE I POECI");

    private final String name;

    PhraseCategory(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }
}
