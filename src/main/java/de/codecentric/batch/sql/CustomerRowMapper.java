    package de.codecentric.batch.sql;

    import de.codecentric.batch.vo.Person;
    import org.springframework.jdbc.core.RowMapper;

    import java.sql.ResultSet;
    import java.sql.SQLException;

    public class CustomerRowMapper implements RowMapper<Person> {
        @Override
        public Person mapRow(ResultSet resultSet, int i) throws SQLException {
            return  new Person(
                    resultSet.getString("first_name"),
                    resultSet.getString("last_name")
            );
        }
    }
