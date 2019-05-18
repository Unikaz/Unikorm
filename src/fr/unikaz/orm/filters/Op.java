package fr.unikaz.orm.filters;

public enum Op{
    EQ("="),
    IN("in"); //todo make this work somewhere ^^

    private String operator;

    Op(String operator) {
        this.operator = operator;
    }

    public String get() {
        return operator;
    }
}
