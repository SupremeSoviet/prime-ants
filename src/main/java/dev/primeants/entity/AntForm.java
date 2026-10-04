package dev.primeants.entity;

public enum AntForm {
    WORKER("worker"), QUEEN("queen");

    private final String serializedName;

    AntForm(String serializedName) { this.serializedName = serializedName; }

    public String serializedName() { return serializedName; }
}
