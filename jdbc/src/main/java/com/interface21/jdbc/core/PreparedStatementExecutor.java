package com.interface21.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PreparedStatementExecutor<T> {

    T execute(PreparedStatement pstmt) throws SQLException;
}
