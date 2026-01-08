package com.example;

import java.util.Objects;

public class DocumentService {
    public String process(Document doc) {
        Objects.requireNonNull(doc, "Document must not be null");
        String content = doc.getContent();
        if (content == null) {
            return "";
        }
        //FIX: Sanitize output to prevent XSS if content is rendered in a UI
        return escapeHtml(content.toUpperCase());
    }

    //FIX: Added basic HTML escaping to mitigate XSS risk
    private String escapeHtml(String input) {
        if (input == null) return null;
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;")
                    .replace("/", "&#x2F;");
    }
}
