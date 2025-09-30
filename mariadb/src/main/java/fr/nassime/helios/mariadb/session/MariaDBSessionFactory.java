package fr.nassime.helios.mariadb.session;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.sql.session.AbstractSqlSessionFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * MariaDB implementation of HeliosSessionFactory.
 */
@Slf4j
public class MariaDBSessionFactory extends AbstractSqlSessionFactory {
    
    public MariaDBSessionFactory(HeliosConfiguration configuration) {
        super(configuration);
        log.info("MariaDB session factory initialized");
    }
    
    @Override
    protected HeliosSession createSession() {
        return new MariaDBSession(getDataSource());
    }
}