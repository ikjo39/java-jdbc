package com.interface21.jdbc.core;

import com.interface21.dao.DataAccessException;
import com.interface21.dao.DataNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcTemplate {

    private static final Logger log = LoggerFactory.getLogger(JdbcTemplate.class);

    private final DataSource dataSource;

    public JdbcTemplate(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void update(String query, Object... parameters) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {
            log.debug("query : {}", query);
            setParameters(pstmt, parameters);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new DataAccessException("데이터 접근 과정에서 문제가 발생하였습니다.", e);
        }
    }

    public <T> T queryForObject(String query, RowMapper<T> mapper, Object... parameters) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {
            log.debug("query : {}", query);

            setParameters(pstmt, parameters);
            ResultSet resultSet = pstmt.executeQuery();

            if (!resultSet.next()) {
                throw new DataNotFoundException("데이터가 존재하지 않습니다.");
            }

            return mapper.map(resultSet);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new DataAccessException("데이터 접근 과정에서 문제가 발생하였습니다.");
        }
    }

    public <T> List<T> queryForList(String query, RowMapper<T> mapper, Object... parameters) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {
            log.debug("query : {}", query);

            setParameters(pstmt, parameters);
            ResultSet resultSet = pstmt.executeQuery();

            List<T> results = new ArrayList<>();
            while (resultSet.next()) {
                results.add(mapper.map(resultSet));
            }
            return results;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new DataAccessException("데이터 접근 과정에서 문제가 발생하였습니다.");
        }
    }

    private void setParameters(PreparedStatement preparedStatement, Object[] parameters) throws SQLException {
        if (parameters == null) {
            return;
        }
        for (int index = 0; index < parameters.length; index++) {
            Object parameter = parameters[index];
            parameter = convertDataSourceTime(parameter);
            preparedStatement.setObject(index + 1, parameter);
        }
    }

    private Object convertDataSourceTime(Object parameter) {
        if (parameter instanceof LocalDateTime localDateTime) {
            parameter = Timestamp.valueOf(localDateTime);
        }
        return parameter;
    }
}
