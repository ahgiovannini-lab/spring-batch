package com.example.batch.listener;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ExecutionContext;

public class ChunkTrackingListener implements StepExecutionListener, ChunkListener {

    private static final Logger logger = LoggerFactory.getLogger(ChunkTrackingListener.class);
    private static final String CHUNK_COUNT_KEY = "chunkResume.chunkCount";

    private final AtomicLong chunkCount = new AtomicLong(0);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        ExecutionContext executionContext = stepExecution.getExecutionContext();
        long restored = executionContext.containsKey(CHUNK_COUNT_KEY)
                ? executionContext.getLong(CHUNK_COUNT_KEY)
                : 0L;
        chunkCount.set(restored);
        logger.info("Step starting. Restored committed chunks: {}", restored);
    }

    @Override
    public void beforeChunk(ChunkContext context) {
        long nextChunk = chunkCount.get() + 1;
        logger.info("Starting chunk {}", nextChunk);
    }

    @Override
    public void afterChunk(ChunkContext context) {
        long completed = chunkCount.incrementAndGet();
        context.getStepContext().getStepExecution().getExecutionContext()
                .putLong(CHUNK_COUNT_KEY, completed);
        logger.info("Chunk {} processed", completed);
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        logger.warn("Chunk failed. Will restart from last committed chunk.");
    }

    public long getChunkCount() {
        return chunkCount.get();
    }
}
