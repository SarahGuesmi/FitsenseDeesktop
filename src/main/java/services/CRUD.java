package services;

import java.sql.SQLException;
import java.util.List;

public interface CRUD<X> {
    void create(X x) throws SQLException;
    List<X> read() throws SQLException;
    void update(X x) throws SQLException;
    void delete(X x) throws SQLException;
    void createPrepared(X x) throws SQLException;
}
