package com.example.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.config.BatchProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@EnableBatchProcessing
class ChunkResumeJobIntegrationTest {

    private static final int TOTAL_LINES = 5;
    private static final int CHUNK_SIZE = 2;
    private static final String CONTEXT_KEY = "chunkResume.committedOffset";

    @TempDir
    static Path tempDir;

    private static Path inputFile;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private BatchProperties batchProperties;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("batch.inputFile", () -> inputFile.toString());
        registry.add("batch.chunkSize", () -> CHUNK_SIZE);
    }

    @BeforeAll
    static void setUp() throws IOException {
        inputFile = tempDir.resolve("input.txt");
        List<String> lines = List.of(
                "line-0000000001",
                "line-0000000002",
                "line-0000000003",
                "line-0000000004",
                "line-0000000005"
        );
        Files.write(inputFile, lines);
    }

    @Test
    void restartReprocessesInterruptedChunkWithoutReprocessingCommittedChunks() throws Exception {
        batchProperties.getFail().setEnabled(true);
        batchProperties.getFail().setAtChunk(2);

        JobParameters params = new JobParametersBuilder()
                .addString("run.id", "integration-test")
                .toJobParameters();

        JobExecution firstRun = jobLauncherTestUtils.launchJob(params);
        assertThat(firstRun.getStatus()).isEqualTo(BatchStatus.FAILED);

        StepExecution firstStep = firstRun.getStepExecutions().iterator().next();
        assertThat(firstStep.getCommitCount()).isEqualTo(1);
        assertThat(firstStep.getReadCount()).isEqualTo(4);
        assertThat(firstStep.getExecutionContext().getLong(CONTEXT_KEY)).isEqualTo(2L);

        batchProperties.getFail().setEnabled(false);

        JobExecution secondRun = jobLauncherTestUtils.launchJob(params);
        assertThat(secondRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution secondStep = secondRun.getStepExecutions().iterator().next();
        assertThat(secondStep.getReadCount()).isEqualTo(TOTAL_LINES - 2);
        assertThat(secondStep.getExecutionContext().getLong(CONTEXT_KEY)).isEqualTo((long) TOTAL_LINES);
    }
}
