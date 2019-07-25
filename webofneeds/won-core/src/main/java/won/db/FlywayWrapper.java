package won.db;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.lang.invoke.MethodHandles;

/**
 * This Bean is used to determine whether or not an update/validation of the ddl
 * is done via flywaydb.
 */
public class FlywayWrapper implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String ddlStrategy = "";
    private DataSource dataSource;

    @Override
    public void afterPropertiesSet() throws Exception {
        if ("validate".equals(ddlStrategy.trim())) {
            Flyway flyway = new Flyway();
            flyway.setDataSource(dataSource);
            flyway.migrate();
        } else {
            logger.info("Flyway DB Migration ommitted due to non-validate DDL-Strategy: " + ddlStrategy);
        }
    }

    public String getDdlStrategy() {
        return ddlStrategy;
    }

    public void setDdlStrategy(String ddlStrategy) {
        this.ddlStrategy = ddlStrategy;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
