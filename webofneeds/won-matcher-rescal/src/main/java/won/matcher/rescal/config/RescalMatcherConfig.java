package won.matcher.rescal.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Created by hfriedrich on 15.09.2015.
 */
@Configuration
@PropertySource("file:${WON_CONFIG_DIR}/matcher-rescal.properties")
public class RescalMatcherConfig {
    @Value("${matcher.rescal.uri.sparql.endpoint}")
    private String sparqlEndpoint;
    @Value("${matcher.rescal.uri.public}")
    private String publicMatcherUri;
    @Value("${matcher.rescal.executionDir}")
    private String executionDirectory;
    @Value("${matcher.rescal.pythonScriptDir}")
    private String pythonScriptDirectory;
    @Value("${matcher.rescal.executionDurationMinutes}")
    private long executionDuration;
    @Value("${matcher.rescal.threshold}")
    private double rescalThreshold;
    @Value("${matcher.rescal.rank}")
    private long rescalRank;

    public String getSparqlEndpoint() {
        return sparqlEndpoint;
    }

    public void setSparqlEndpoint(final String sparqlEndpoint) {
        this.sparqlEndpoint = sparqlEndpoint;
    }

    public String getPublicMatcherUri() {
        return publicMatcherUri;
    }

    public void setPublicMatcherUri(final String publicMatcherUri) {
        this.publicMatcherUri = publicMatcherUri;
    }

    public String getExecutionDirectory() {
        return executionDirectory;
    }

    public void setExecutionDirectory(final String executionDirectory) {
        this.executionDirectory = executionDirectory;
    }

    public String getPythonScriptDirectory() {
        return pythonScriptDirectory;
    }

    public void setPythonScriptDirectory(final String pythonScriptDirectory) {
        this.pythonScriptDirectory = pythonScriptDirectory;
    }

    public FiniteDuration getExecutionDuration() {
        return Duration.create(executionDuration, TimeUnit.MINUTES);
    }

    public void setExecutionDuration(final long executionDuration) {
        this.executionDuration = executionDuration;
    }

    public double getRescalThreshold() {
        return rescalThreshold;
    }

    public void setRescalThreshold(final double rescalThreshold) {
        this.rescalThreshold = rescalThreshold;
    }

    public long getRescalRank() {
        return rescalRank;
    }

    public void setRescalRank(final long rescalRank) {
        this.rescalRank = rescalRank;
    }
}
