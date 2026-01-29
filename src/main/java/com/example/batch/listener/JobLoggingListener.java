package com.example.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class JobLoggingListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(JobLoggingListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("Job {} starting. ExecutionId={}", jobExecution.getJobInstance().getJobName(), jobExecution.getId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        logger.info("Job {} finished with status {}", jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus());
    }
}
