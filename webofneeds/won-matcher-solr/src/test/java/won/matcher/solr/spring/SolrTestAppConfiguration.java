package won.matcher.solr.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import won.matcher.solr.config.SolrMatcherConfig;

/**
 * The main application configuration.
 */
@Configuration
@ImportResource("classpath:spring/component/needproducer/needproducer-mixed.xml")
@PropertySource({"file:${WON_CONFIG_DIR}/matcher-solr.properties",
                 "file:${WON_CONFIG_DIR}/cluster-node.properties",
                 "file:${WON_CONFIG_DIR}/mail-dir-bot.properties",
                 "file:${WON_CONFIG_DIR}/need-dir-bot.properties"})
@ComponentScan({"won.matcher.service.common.config", "won.matcher.service.common.service.http",
                "won.matcher.solr.config", "won.matcher.solr.index", "won.matcher.solr.hints"})
public class SolrTestAppConfiguration
{
  @Autowired
  private SolrMatcherConfig matcherConfig;
}
