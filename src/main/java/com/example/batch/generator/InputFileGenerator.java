package com.example.batch.generator;

import com.example.batch.config.BatchProperties;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("generate")
public class InputFileGenerator implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(InputFileGenerator.class);

    private final BatchProperties batchProperties;

    public InputFileGenerator(BatchProperties batchProperties) {
        this.batchProperties = batchProperties;
    }

    @Override
    public void run(String... args) throws Exception {
        Path path = Path.of(batchProperties.getInputFile());
        Files.createDirectories(path.getParent());
        long totalLines = batchProperties.getGenerator().getLines();
        int padding = batchProperties.getGenerator().getLinePadding();

        logger.info("Generating file {} with {} lines", path, totalLines);

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (long i = 1; i <= totalLines; i++) {
                writer.write(formatLine(i, padding));
                writer.newLine();
                if (i % 1_000_000 == 0) {
                    logger.info("Generated {} lines", i);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate input file", ex);
        }
        logger.info("File generation completed: {}", path);
    }

    private String formatLine(long index, int padding) {
        return String.format("line-%0" + padding + "d", index);
    }
}
