package de.codecentric.batch.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class ExitJobListener  implements JobExecutionListener {
    @Override
    public void beforeJob(JobExecution jobExecution) {
        // nothing to do.
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        ExitStatus es = jobExecution.getExitStatus();
        jobExecution.setExitStatus(new ExitStatus(es.getExitCode(), "foo"));
    }
}
