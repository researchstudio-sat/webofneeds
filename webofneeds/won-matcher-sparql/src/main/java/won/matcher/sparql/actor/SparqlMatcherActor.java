package won.matcher.sparql.actor;

import java.io.IOException;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.github.jsonldjava.core.JsonLdError;

import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import scala.concurrent.duration.Duration;
import won.matcher.service.common.event.BulkNeedEvent;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.sparql.config.SparqlMatcherConfig;

/**
 * Siren/Solr based abstract matcher with all implementations for querying as well as indexing needs.
 */
@Component
@Scope("prototype")
public class SparqlMatcherActor extends UntypedActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    
    @Autowired
    private SparqlMatcherConfig config;

    @Override
    public void onReceive(final Object o) throws Exception {

        if (o instanceof NeedEvent) {
            NeedEvent needEvent = (NeedEvent) o;
            if (needEvent.getEventType().equals(NeedEvent.TYPE.ACTIVE)) {
                processActiveNeedEvent(needEvent);
            } else if (needEvent.getEventType().equals(NeedEvent.TYPE.INACTIVE)) {
                processInactiveNeedEvent(needEvent);
            } else {
                unhandled(o);
            }
        } else if (o instanceof BulkNeedEvent) {
            log.info("received bulk need event, processing {} need events ...", ((BulkNeedEvent) o).getNeedEvents().size());
            for (NeedEvent event : ((BulkNeedEvent) o).getNeedEvents()) {
                processActiveNeedEvent(event);
            }
        } else {
            unhandled(o);
        }
    }

    protected void processInactiveNeedEvent(NeedEvent needEvent) throws IOException, JsonLdError {
    	log.info("Received inactive need.");
    }

    protected void processActiveNeedEvent(NeedEvent needEvent)
            throws IOException, JsonLdError {
    	
    	log.info("Received active need.");
    	
    	String queryString = "SELECT ?title WHERE {\n" +
                " ?needUri a won:Need . \n" +
    			" ?needUri won:is ?isUri . \n" +
                " ?isUri dc:title ?title . \n" +
                "}\n";
    	
    	ParameterizedSparqlString query = new ParameterizedSparqlString(queryString);
    	query.setIri("needUri", needEvent.getUri());
    	query.setNsPrefix("won", "http://purl.org/webofneeds/model#");
        query.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");
    	
    	QueryExecution execution = QueryExecutionFactory.sparqlService(config.getSparqlEndpoint(), query.asQuery());
    	ResultSet result = execution.execSelect();
    	while(result.hasNext()) {
    		QuerySolution solution = result.nextSolution();
    		log.info("Found result: " + solution.get("title").asLiteral().getString());
    	}
    	execution.close();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {

        SupervisorStrategy supervisorStrategy = new OneForOneStrategy(
                0, Duration.Zero(), new Function<Throwable, SupervisorStrategy.Directive>() {

            @Override
            public SupervisorStrategy.Directive apply(Throwable t) throws Exception {

                log.warning("Actor encountered error: {}", t);
                // default behaviour
                return SupervisorStrategy.escalate();
            }
        });

        return supervisorStrategy;
    }

}
