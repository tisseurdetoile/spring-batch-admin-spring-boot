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
    public class BatchConfiguration {
        private static final Logger LOG = LoggerFactory.getLogger(BatchConfiguration.class);

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
        public FlatFileItemReader<Person> reader() {
            LOG.info("reader()");

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

        @Bean
        public ConsoleItemWriter<Person> writer() {
            return new ConsoleItemWriter<>();
        }

        @Bean
        public JobExecutionListener exitListener () { return  new ExitJobListener(); }

        // tag::jobstep[]
        @Bean
        public Job importUserJob(Step step1, JobExecutionListener exitListener) {
            return jobBuilderFactory.get("exampleImportUserJob")
                    .incrementer(new RunIdIncrementer())
                    //.preventRestart()
                    .flow(step1)
                    .end()
                    .listener(exitListener)
                    .build();
        }

        @Bean
        public Step step1() {
            return stepBuilderFactory.get("step1")
                    .<Person, Person> chunk(10)
                    .reader(reader())
                    .processor(processor())
                    .writer(writer())
                    .build();
        }
    // end::jobstep[]
    }
