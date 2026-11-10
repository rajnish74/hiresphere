package com.hiresphere.ai.resume;


public record ExtractedText(String text, int pagesRead, int totalPages, boolean textCapped) {

    public boolean pagesCutShort() {
        return pagesRead < totalPages;
    }

    public boolean cutShort() {
        return pagesCutShort() || textCapped;
    }
}