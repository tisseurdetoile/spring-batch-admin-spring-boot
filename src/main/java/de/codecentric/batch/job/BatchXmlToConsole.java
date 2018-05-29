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
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchXmlToConsole {
    private static final Logger LOG = LoggerFactory.getLogger(BatchXmlToConsole.class);

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean
    public StaxEventItemReader<Person> reader() {
        StaxEventItemReader<Person> reader = new StaxEventItemReader<Person>();
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(Person.class);
        reader.setResource(new ClassPathResource("sample-data.xml"));
        reader.setFragmentRootElementName("USER");
        reader.setUnmarshaller(marshaller);
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
    public Job xmlToConsoleJob(Step step1, JobExecutionListener exitListener) {
        return jobBuilderFactory.get("xmlToConsoleJob")
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
