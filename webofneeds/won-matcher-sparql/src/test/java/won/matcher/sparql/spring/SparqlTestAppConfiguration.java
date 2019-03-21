package won.matcher.sparql.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
 * The main application configuration.
 */
@Configuration
@ImportResource({"classpath:spring/component/sparqlMatcherEvaluation.xml",
                 "classpath:spring/component/sparqlMatcherEvaluation.xml"})
@PropertySource({"file:${WON_CONFIG_DIR}/matcher-sparql.properties",
                 "file:${WON_CONFIG_DIR}/cluster-node.properties"})
@ComponentScan({"won.matcher.service.common.config", "won.matcher.service.common.service.http",
                "won.matcher.sparql.config", "won.matcher.sparql.index", "won.matcher.sparql.hints",
                "won.matcher.sparql.evaluation", "won.matcher.sparql.query"})
public class SparqlTestAppConfiguration
{
}
