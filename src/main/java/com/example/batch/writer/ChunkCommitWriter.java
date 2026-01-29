package com.example.batch.writer;

import com.example.batch.config.BatchProperties;
import com.example.batch.listener.ChunkTrackingListener;
import com.example.batch.reader.ChunkCheckpointingReader;
import java.util.List;
import org.springframework.batch.item.ItemWriter;

public class ChunkCommitWriter implements ItemWriter<String> {

    private final ChunkCheckpointingReader reader;
    private final ChunkTrackingListener chunkTrackingListener;
    private final BatchProperties batchProperties;

    public ChunkCommitWriter(ChunkCheckpointingReader reader,
                             ChunkTrackingListener chunkTrackingListener,
                             BatchProperties batchProperties) {
        this.reader = reader;
        this.chunkTrackingListener = chunkTrackingListener;
        this.batchProperties = batchProperties;
    }

    @Override
    public void write(List<? extends String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        long currentChunkIndex = chunkTrackingListener.getChunkCount() + 1;
        if (batchProperties.getFail().isEnabled()
                && currentChunkIndex == batchProperties.getFail().getAtChunk()) {
            throw new RuntimeException("Simulated failure at chunk " + currentChunkIndex);
        }
        reader.markCommittedOffset(reader.getCurrentIndex());
    }
}
