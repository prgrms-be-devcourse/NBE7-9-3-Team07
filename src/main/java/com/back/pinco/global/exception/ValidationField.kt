package com.back.pinco.global.exception;

public sealed interface ValidationField
        permits LatitudeField, LongitudeField, ContentField, UnknownField {

    String name();

    static ValidationField from(String fieldName) {
        return switch (fieldName) {
            case "latitude" -> new LatitudeField();
            case "longitude" -> new LongitudeField();
            case "content" -> new ContentField();
            default -> new UnknownField(fieldName);
        };
    }
}

final class LatitudeField implements ValidationField {
    @Override public String name() { return "latitude"; }
}

final class LongitudeField implements ValidationField {
    @Override public String name() { return "longitude"; }
}

final class ContentField implements ValidationField {
    @Override public String name() { return "content"; }
}

final class UnknownField implements ValidationField {
    private final String name;
    public UnknownField(String name) { this.name = name; }
    @Override public String name() { return name; }
}
