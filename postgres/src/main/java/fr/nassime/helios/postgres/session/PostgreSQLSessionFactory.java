package fr.nassime.helios.postgres.session;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.sql.session.AbstractSqlSessionFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL implementation of HeliosSessionFactory.
 */
@Slf4j
public class PostgreSQLSessionFactory extends AbstractSqlSessionFactory {
    
    public PostgreSQLSessionFactory(HeliosConfiguration configuration) {
        super(configuration);
        log.info("PostgreSQL session factory initialized");
    }
    
    @Override
    protected HeliosSession createSession() {
        return new PostgreSQLSession(getDataSource());
    }
}