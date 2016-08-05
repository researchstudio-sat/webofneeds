package won.matcher.solr.actor;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Created by Soheilk on 24.08.2015.
 *
 * Siren/Solr based matcher implementation for querying as well as indexing needs. Uses implementations defined in
 * AbstractSirenMatcherActor.
 */
@Component
@Scope("prototype")
public class SolrMatcherActor extends AbstractSolrMatcherActor
{
  // The solution with abstract class is due to Spring/Akka initialization having problems
  // when SirenMonitoringMatcherActor was extending non-abstract SirenMatcherActor.
}
