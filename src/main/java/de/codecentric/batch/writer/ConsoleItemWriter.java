package de.codecentric.batch.writer;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;

import java.util.List;


/**
 * Logs each content item by reflection with the {@link ToStringBuilder#reflectionToString(Object)}
 * method.
 *
 * @author Antoine
 *
 * @param <T>
 */
public class ConsoleItemWriter<T> implements ItemWriter<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ConsoleItemWriter.class);

    @Override
    public void write(List<? extends T> items) throws Exception {
        LOG.trace("Console item writer starts");
        for (T item : items) {
            LOG.info(ToStringBuilder.reflectionToString(item));
        }
        LOG.info("Console item writer ends");
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        ExecutionContext stepContext = stepExecution.getExecutionContext();
        JobParameters parameters = stepExecution.getJobParameters();
        String from = parameters.getString("from");
        String to = parameters.getString("to");

        LOG.info("from", from);
        LOG.info("to", to);
    }
}