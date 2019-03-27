package won.matcher.solr.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
 * The main application configuration.
 */
@Configuration
@ImportResource({ "classpath:spring/component/solrMatcherEvaluation.xml",
                "classpath:spring/component/solrMatcherEvaluation.xml" })
@PropertySource({ "file:${WON_CONFIG_DIR}/matcher-solr.properties", "file:${WON_CONFIG_DIR}/cluster-node.properties" })
@ComponentScan({ "won.matcher.service.common.config", "won.matcher.service.common.service.http",
                "won.matcher.solr.config", "won.matcher.solr.index", "won.matcher.solr.hints",
                "won.matcher.solr.evaluation", "won.matcher.solr.query" })
public class SolrTestAppConfiguration {
}
