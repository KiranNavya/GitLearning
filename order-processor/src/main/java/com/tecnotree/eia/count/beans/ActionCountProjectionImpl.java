package com.tecnotree.eia.count.beans;

public record ActionCountProjectionImpl(String actionCode, Integer recordCount) implements ActionCountProjection {

    // Constructor to initialize the fields

    // Optional: Add toString for better logging and debugging
    @Override
    public String toString() {
        return "ActionCountProjectionImpl{" +
                "actionCode='" + actionCode + '\'' +
                ", recordCount=" + recordCount +
                '}';
    }
}