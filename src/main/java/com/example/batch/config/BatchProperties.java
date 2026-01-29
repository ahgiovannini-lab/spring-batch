package com.example.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "batch")
public class BatchProperties {

    private int chunkSize = 1000;
    private String inputFile = "src/main/resources/input/huge-file.txt";
    private final FailProperties fail = new FailProperties();
    private final GeneratorProperties generator = new GeneratorProperties();

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public FailProperties getFail() {
        return fail;
    }

    public GeneratorProperties getGenerator() {
        return generator;
    }

    public static class FailProperties {
        private boolean enabled = false;
        private long atChunk = 7;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getAtChunk() {
            return atChunk;
        }

        public void setAtChunk(long atChunk) {
            this.atChunk = atChunk;
        }
    }

    public static class GeneratorProperties {
        private long lines = 5_000_000L;
        private int linePadding = 10;

        public long getLines() {
            return lines;
        }

        public void setLines(long lines) {
            this.lines = lines;
        }

        public int getLinePadding() {
            return linePadding;
        }

        public void setLinePadding(int linePadding) {
            this.linePadding = linePadding;
        }
    }
}
