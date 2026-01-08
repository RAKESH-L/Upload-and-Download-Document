package com.example;

import java.util.Objects;

public final class Document {
    //FIX: Made content final for immutability
    private final String content;

    //FIX: Added input validation to prevent null or empty content
    public Document(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Document content must not be null or empty"); //FIX: Validate input in constructor
        }
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    //FIX: Removed setter to make Document immutable and prevent arbitrary content changes

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(content, document.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        //FIX: Avoid exposing full content in toString to prevent sensitive data leakage
        return "Document{content='[PROTECTED]'}";
    }
}
