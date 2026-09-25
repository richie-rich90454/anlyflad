package com.anlyflad.core.model;
public enum ParamType {
    BOOLEAN,
    INTEGER,
    DOUBLE,
    STRING;
    boolean supportsRange() {
        return this==INTEGER||this==DOUBLE;
    }
    @Override
    public String toString() {
        return name();
    }
}
