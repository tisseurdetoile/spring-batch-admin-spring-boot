package de.codecentric.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * Logs each content item by using toString. method.
 * NOTA : The calling method in the batch Configuration need tu use the @StepScope Annotation in order
 * to use je @Value("#{jobParameters['fileName']
 * @param <T>
 */
public class ConsoleItemWriter<T> implements ItemWriter<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ConsoleItemWriter.class);

    /**
     * test of passing parameter
     */
    @Value("#{jobParameters['fileName']?: 'some default'}")
    public String fileName;

    @Override
    public void write(List<? extends T> items) throws Exception {
        LOG.info(String.format("fileName=%s", this.fileName));
        LOG.info("Console item writer starts");
        for (T item : items) {
            LOG.info(item.toString());
        }
        LOG.info("Console item writer ends");
    }
}