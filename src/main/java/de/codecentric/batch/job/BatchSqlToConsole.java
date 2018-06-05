package de.codecentric.batch.job;

import de.codecentric.batch.listener.ExitJobListener;
import de.codecentric.batch.processor.PersonItemProcessor;
import de.codecentric.batch.sql.CustomerRowMapper;
import de.codecentric.batch.vo.Person;
import de.codecentric.batch.writer.ConsoleItemWriter;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Configuration
public class BatchSqlToConsole {
    private static final Logger LOG = LoggerFactory.getLogger(BatchSqlToConsole.class);

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Value("${user.schema.script}")
    private Resource schemaScript;

    @Value("${user.data.script}")
    private Resource dataScript;

    @Value("${user.jdbc.url}")
    private String url;

    @Value("${user.jdbc.class}")
    private String className;

    @Value("${user.jdbc.username}")
    private String username;

    @Value("${user.jdbc.password}")
    private String password;

    @Bean
    public DataSource userDataSource() {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(className);
        basicDataSource.setUrl(url);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
        basicDataSource.setTestWhileIdle(true);
        basicDataSource.setDefaultAutoCommit(true);
        return basicDataSource;
    }

    @Bean
    public DataSourceInitializer dataSourceInitializer(final DataSource userDataSource) {
        final DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(userDataSource);
        initializer.setDatabasePopulator(databasePopulator());
        return initializer;
    }

    private DatabasePopulator databasePopulator() {
        final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(schemaScript);
        populator.addScript(dataScript);
        return populator;
    }


    @Bean
    public JdbcCursorItemReader<Person> jdbcCursorItemReader() {

        // ResultSet and JdbcCursorItemReader are not thread safe..
        // ORDER BY is necessary because in case of failure wont be able to know
        // where to start
        JdbcCursorItemReader<Person> jdbcCursorItemReader = new JdbcCursorItemReader<>();
        // String sql = "person_id, first_name , last_name FROM
        // people ORDER BY last_name , first_name";
        String sql = "SELECT person_id, first_name , last_name FROM people ORDER BY person_id";
        jdbcCursorItemReader.setSql(sql);
        jdbcCursorItemReader.setDataSource(userDataSource());
        jdbcCursorItemReader.setRowMapper(new CustomerRowMapper());

        return jdbcCursorItemReader;
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
    public JobExecutionListener exitListener () { return new ExitJobListener(); }

    // tag::jobstep[]
    @Bean
    public Job sqlToConsoleJob(Step stepSqlConsole, JobExecutionListener exitListener) {
        return jobBuilderFactory.get("BatchSqlToConsole")
                .incrementer(new RunIdIncrementer())
                //.preventRestart()
                .flow(stepSqlConsole)
                .end()
                .listener(exitListener)
                .build();
    }

    @Bean
    public Step stepSqlConsole() {
        return stepBuilderFactory.get("step1")
                .<Person, Person> chunk(10)
                .reader(jdbcCursorItemReader())
                .processor(processor())
                .writer(writer())
                .build();
    }
    // end::jobstep[]
}
