package com.example.batch.config;

import com.example.batch.listener.ChunkTrackingListener;
import com.example.batch.listener.JobLoggingListener;
import com.example.batch.listener.StepLoggingListener;
import com.example.batch.reader.ChunkCheckpointingReader;
import com.example.batch.writer.ChunkCommitWriter;
import java.nio.file.Path;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ChunkResumeJobConfig {

    private static final String JOB_NAME = "chunkResumeJob";
    private static final String STEP_NAME = "chunkResumeStep";

    @Bean
    public Job chunkResumeJob(JobRepository jobRepository,
                              Step chunkResumeStep,
                              JobLoggingListener jobLoggingListener) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(chunkResumeStep)
                .listener(jobLoggingListener)
                .build();
    }

    @Bean
    public Step chunkResumeStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                ChunkCheckpointingReader reader,
                                ChunkCommitWriter writer,
                                ChunkTrackingListener chunkTrackingListener,
                                StepLoggingListener stepLoggingListener,
                                BatchProperties batchProperties) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<String, String>chunk(batchProperties.getChunkSize(), transactionManager)
                .reader(reader)
                .writer(writer)
                .listener(chunkTrackingListener)
                .listener(stepLoggingListener)
                .build();
    }

    @Bean
    @Scope(value = "step", proxyMode = ScopedProxyMode.INTERFACES)
    public ChunkCheckpointingReader chunkCheckpointingReader(BatchProperties batchProperties) {
        return new ChunkCheckpointingReader(Path.of(batchProperties.getInputFile()), true);
    }

    @Bean
    public ChunkCommitWriter chunkCommitWriter(ChunkCheckpointingReader reader,
                                               ChunkTrackingListener chunkTrackingListener,
                                               BatchProperties batchProperties) {
        return new ChunkCommitWriter(reader, chunkTrackingListener, batchProperties);
    }

    @Bean
    public ChunkTrackingListener chunkTrackingListener() {
        return new ChunkTrackingListener();
    }

    @Bean
    public JobLoggingListener jobLoggingListener() {
        return new JobLoggingListener();
    }

    @Bean
    public StepLoggingListener stepLoggingListener() {
        return new StepLoggingListener();
    }

    
}
