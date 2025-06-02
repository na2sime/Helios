package fr.nassime.helios.connection;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataSourceConfig {
    private String jdbcUrl;
    private String username;
    private String password;
    private String schema;


    @Builder.Default
    private int maxPoolSize = 10;
    @Builder.Default
    private int minIdle = 5;
    @Builder.Default
    private long connectionTimeout = 30000;
    @Builder.Default
    private long idleTimeout = 600000;
    @Builder.Default
    private long maxLifetime = 1800000;
}
