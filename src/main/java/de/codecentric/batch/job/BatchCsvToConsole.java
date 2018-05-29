    package de.codecentric.batch.job;

import de.codecentric.batch.listener.ExitJobListener;
import de.codecentric.batch.processor.PersonItemProcessor;
import de.codecentric.batch.vo.Person;
import de.codecentric.batch.writer.ConsoleItemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

    @Configuration
    public class BatchCsvToConsole {
        private static final Logger LOG = LoggerFactory.getLogger(BatchCsvToConsole.class);

        @Autowired
        public JobBuilderFactory jobBuilderFactory;

        /*
        @Autowired
        public JobLocator JobLocator;
        */

        @Autowired
        PlatformTransactionManager transactionManager;

        @Autowired
        public StepBuilderFactory stepBuilderFactory;

        // tag::readerwriterprocessor[]
        @Bean
        public FlatFileItemReader<Person> csvReader() {
            LOG.info("csvReader()");

            FlatFileItemReader<Person> reader = new FlatFileItemReader<Person>();
                    reader.setResource(new ClassPathResource("sample-data.csv"));
                    reader.setLineMapper(new DefaultLineMapper<Person>() {{
                            setLineTokenizer(new DelimitedLineTokenizer(){{
                                    setNames(new String[]{"firstName", "lastName"});
                                }});
                        setFieldSetMapper(new BeanWrapperFieldSetMapper<Person>(){{
                                    setTargetType(Person.class);
                                }});
                        }});
                    return reader;
        }

        @Bean
        public PersonItemProcessor processor() {
            return new PersonItemProcessor();
        }

        /**
         * NOTA : use the @StepScope Annotation in order
         * to use je @Value("#{jobParameters['fileName'].. in the ConsoleItemWriter
         */
        @Bean
        @StepScope
        public ConsoleItemWriter<Person> writer() {
            return new ConsoleItemWriter<>();
        }

        @Bean
        public JobExecutionListener exitListener () { return  new ExitJobListener(); }

        // tag::jobstep[]
        @Bean
        public Job csvToConsoleJob(Step stepCsvConsole, JobExecutionListener exitListener) {
            return jobBuilderFactory.get("BatchCsvToConsole")
                    .incrementer(new RunIdIncrementer())
                    //.preventRestart()
                    .flow(stepCsvConsole)
                    .end()
                    .listener(exitListener)
                    .build();
        }

        @Bean
        public Step stepCsvConsole() {
            return stepBuilderFactory.get("step1")
                    .<Person, Person> chunk(10)
                    .reader(csvReader())
                    .processor(processor())
                    .writer(writer())
                    .build();
        }
    // end::jobstep[]
    }
