package com.example.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

public class StepLoggingListener implements StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(StepLoggingListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.info("Step {} starting. ExecutionId={}", stepExecution.getStepName(), stepExecution.getId());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        logger.info("Step {} finished with status {}", stepExecution.getStepName(), stepExecution.getStatus());
        return stepExecution.getExitStatus();
    }
}
