    package de.codecentric.batch.job;

import de.codecentric.batch.listener.ExitJobListener;
import de.codecentric.batch.processor.PersonItemProcessor;
import de.codecentric.batch.vo.Person;
import de.codecentric.batch.writer.ConsoleItemWriter;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.slf4j.Logger;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.slf4j.LoggerFactory.getLogger;

    /**
     * BatchMinioToConsole
     *
     * POC :
     * - read a csv file from a minio
     * - uppercase the data
     * - write it to the console.
     * - bucket=csv,file=sample-data.minio.csv
     * - docker run -p 9000:9000 --name minio1 -v /Volumes/HOME/tmp/minio:/data  -v /Volumes/HOME/tmp/minio_config:/root/.minio minio/minio server /data
     */
    @Configuration
    public class BatchMinioToConsole {
        private static final Logger LOG = getLogger(BatchMinioToConsole.class);

        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        @Autowired
        public JobBuilderFactory jobBuilderFactory;

        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        @Autowired
        public StepBuilderFactory stepBuilderFactory;

        @Value("${minio.accesskey}")
        private String accesskey;

        @Value("${minio.secretkey}")
        private String secretkey;

        @Value("${minio.url}")
        private String miniourl;


        private InputStreamResource getFileFromMinio(String miniourl,
                                                     String accesskey,
                                                     String secretkey,
                                                     String bucket,
                                                     String file) throws InvalidPortException, InvalidEndpointException, IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, InternalException, NoResponseException, InvalidBucketNameException, XmlPullParserException, ErrorResponseException, InvalidArgumentException {
            MinioClient minioClient = new MinioClient(miniourl, accesskey, secretkey);
            minioClient.statObject(bucket, file);
            InputStream stream = minioClient.getObject(bucket, file);
            InputStreamResource res = new InputStreamResource(stream);
            return  res;
        }

        /**
         * NOTA : use the @StepScope Annotation in order
         * to use je @Value("#{jobParameters['fileName'].. in the ConsoleItemWriter
         * dont forget to add null to calling
         */
        @Bean
        @StepScope
        public FlatFileItemReader<Person> csvReaderMinio(@Value("#{jobParameters['bucket']}") String bucket,
                                                         @Value("#{jobParameters['file']}") String file)  {
            LOG.info("csvReaderMinio()");

            InputStreamResource inStreamRes = null;
            try {
                inStreamRes = getFileFromMinio(miniourl, accesskey, secretkey, bucket, file);
            } catch (InvalidPortException e) {
                e.printStackTrace();
            } catch (InvalidEndpointException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InsufficientDataException e) {
                e.printStackTrace();
            } catch (InternalException e) {
                e.printStackTrace();
            } catch (NoResponseException e) {
                e.printStackTrace();
            } catch (InvalidBucketNameException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (ErrorResponseException e) {
                e.printStackTrace();
            } catch (InvalidArgumentException e) {
                e.printStackTrace();
            }

            FlatFileItemReader<Person> reader = new FlatFileItemReader<Person>();
            reader.setResource(inStreamRes);
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

        @Bean
        public Job MinioCsvToConsoleJob(Step stepMinioCsvConsole, JobExecutionListener exitListener) {
            return jobBuilderFactory.get("BatchMinioCsvToConsole")
                    .incrementer(new RunIdIncrementer())
                    //.preventRestart()
                    .flow(stepMinioCsvConsole)
                    .end()
                    .listener(exitListener)
                    .build();
        }

        @Bean
        public Step stepMinioCsvConsole() {
            return stepBuilderFactory.get("step1")
                    .<Person, Person> chunk(10)
                    .reader(csvReaderMinio(null, null))
                    .processor(processor())
                    .writer(writer())
                    .build();
        }
    }
