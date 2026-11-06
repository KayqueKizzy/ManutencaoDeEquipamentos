package br.com.manutencao.config;

import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrientDbConfiguration {
    @Bean(destroyMethod = "close")
    public OrientDB orientDB(@Value("${orientdb.server-url}") String serverUrl) {
        return new OrientDB(serverUrl, OrientDBConfig.defaultConfig());
    }

    @Bean(destroyMethod = "close")
    public ODatabasePool orientDbPool(
            OrientDB orientDB,
            @Value("${orientdb.database}") String database,
            @Value("${orientdb.username}") String username,
            @Value("${orientdb.password}") String password) {
        return new ODatabasePool(orientDB, database, username, password);
    }
}
