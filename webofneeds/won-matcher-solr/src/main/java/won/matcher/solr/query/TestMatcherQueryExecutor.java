package won.matcher.solr.query;

import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by hfriedrich on 12.08.2016.
 */
@Component
public class TestMatcherQueryExecutor extends DefaultMatcherQueryExecuter {
  @PostConstruct
  private void init() {
    solrClient = new HttpSolrClient.Builder(config.getSolrEndpointUri(true)).build();
  }
}
