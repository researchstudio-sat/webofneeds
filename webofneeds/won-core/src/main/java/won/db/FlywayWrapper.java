package won.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

import javax.sql.DataSource;

/**
 * Created by fsuda on 13.01.2017.
 */
public class FlywayWrapper {
    private Flyway flyway = new Flyway();
    private String ddlStrategy = "";
    private DataSource dataSource;

    public int migrate() throws FlywayException {
        if("validate".equals(ddlStrategy)) {
            flyway.setDataSource(dataSource);
            return flyway.migrate();
        }
        return 0;
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
