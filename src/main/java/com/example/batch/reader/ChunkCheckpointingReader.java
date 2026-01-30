package com.example.batch.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

public class ChunkCheckpointingReader implements ItemStreamReader<String>, ItemStream {

    private static final Logger logger = LoggerFactory.getLogger(ChunkCheckpointingReader.class);
    private static final String CONTEXT_KEY = "chunkResume.committedOffset";

    private final Path inputPath;
    private final boolean saveState;

    private BufferedReader reader;
    private long committedOffset;
    private long currentIndex;
    private long lastCommittedOffset;

    public ChunkCheckpointingReader(Path inputPath, boolean saveState) {
        this.inputPath = inputPath;
        this.saveState = saveState;
        this.lastCommittedOffset = -1L;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (saveState && executionContext.containsKey(CONTEXT_KEY)) {
            committedOffset = executionContext.getLong(CONTEXT_KEY);
        } else {
            committedOffset = 0L;
        }

        currentIndex = committedOffset;
        lastCommittedOffset = committedOffset;

        try {
            reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8);
            skipToCommittedOffset();
            logger.info("Reader opened. Committed offset: {}", committedOffset);
        } catch (IOException ex) {
            throw new ItemStreamException("Failed to open input file: " + inputPath, ex);
        }
    }

    private void skipToCommittedOffset() throws IOException {
        long skipped = 0;
        while (skipped < committedOffset) {
            if (reader.readLine() == null) {
                break;
            }
            skipped++;
        }
        if (skipped != committedOffset) {
            logger.warn("Reached EOF while skipping to committed offset. Requested={}, skipped={}", committedOffset, skipped);
        }
    }

    @Override
    public String read() throws Exception {
        if (reader == null) {
            return null;
        }
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        currentIndex++;
        return line;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (!saveState) {
            return;
        }
        long offsetToPersist = lastCommittedOffset >= 0 ? lastCommittedOffset : committedOffset;
        logger.info("Persisting committed offset. lastCommittedOffset={}, committedOffset={}, offsetToPersist={}",
                lastCommittedOffset, committedOffset, offsetToPersist);
        executionContext.putLong(CONTEXT_KEY, offsetToPersist);
    }

    @Override
    public void close() throws ItemStreamException {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ex) {
                throw new ItemStreamException("Failed to close reader", ex);
            }
        }
    }

    public long getCurrentIndex() {
        return currentIndex;
    }

    public void markCommittedOffset(long committedOffset) {
        this.lastCommittedOffset = committedOffset;
    }
}
