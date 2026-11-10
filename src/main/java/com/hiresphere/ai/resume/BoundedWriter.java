package com.hiresphere.ai.resume;

import java.io.IOException;
import java.io.Writer;

final class BoundedWriter extends Writer {

    static final class LimitReached extends IOException {
        LimitReached() {
            super("Text limit reached");
        }
    }

    private final StringBuilder out = new StringBuilder();
    private final int max;
    private boolean capped;

    BoundedWriter(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("max must be positive");
        }
        this.max = max;
    }

    @Override
    public void write(char[] buffer, int offset, int length) throws IOException {
        int room = max - out.length();
        if (length <= room) {
            out.append(buffer, offset, length);
            return;
        }
        out.append(buffer, offset, Math.max(room, 0));
        capped = true;
        throw new LimitReached();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    boolean capped() {
        return capped;
    }

    String text() {
        int length = out.length();
        if (length > 0 && Character.isHighSurrogate(out.charAt(length - 1))) {
            return out.substring(0, length - 1);
        }
        return out.toString();
    }
}