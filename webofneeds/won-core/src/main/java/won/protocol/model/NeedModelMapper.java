package won.protocol.model;

import java.net.URI;
import java.util.Date;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.util.DateTimeUtils;
import won.protocol.util.ModelMapper;
import won.protocol.vocabulary.WON;

/**
 * User: gabriel Date: 09.04.13 Time: 15:36
 */
public class NeedModelMapper implements ModelMapper<Need> {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Model toModel(Need need) {

        Model model = ModelFactory.createDefaultModel();
        Resource needResource = model.createResource(need.getNeedURI().toString(), WON.NEED);

        // set creation date
        Literal creationDate = DateTimeUtils.toLiteral(need.getCreationDate(), model);
        if (creationDate != null) {
            model.add(model.createStatement(needResource, DCTerms.created, creationDate));
        }

        // set modified date
        Literal lastUpdate = DateTimeUtils.toLiteral(need.getLastUpdate(), model);
        if (lastUpdate != null) {
            model.add(model.createStatement(needResource, DCTerms.modified, lastUpdate));
        }

        // set state
        model.add(model.createStatement(needResource, WON.IS_IN_STATE, WON.toResource(need.getState())));

        // We don't add the need owner's endpoint here as this is confidential information
        return model;
    }

    @Override
    public Need fromModel(Model model) {
        Need need = new Need();

        ResIterator needIt = model.listSubjectsWithProperty(RDF.type, WON.NEED);
        if (!needIt.hasNext())
            throw new IllegalArgumentException("at least one RDF node must be of type won:Need");

        Resource needRes = needIt.next();
        logger.debug("processing need resource {}", needRes.getURI());

        need.setNeedURI(URI.create(needRes.getURI()));

        Statement dateStat = needRes.getProperty(DCTerms.created);
        if (dateStat != null && dateStat.getObject().isLiteral()) {
            need.setCreationDate(DateTimeUtils.toDate(dateStat.getObject().asLiteral(), model));
            logger.debug("found needCreationDate literal value '{}'", dateStat.getObject().asLiteral().getString());
        } else {
            logger.debug("no needCreationDate property found for need resource {}", needRes.getURI());
        }

        Statement stateStat = needRes.getProperty(WON.IS_IN_STATE);
        if (stateStat != null && stateStat.getObject().isResource()) {
            URI uri = URI.create(stateStat.getResource().getURI());
            need.setState(NeedState.parseString(uri.getFragment()));
            logger.debug("found isInState literal value '{}'", stateStat.getObject().asResource().getURI());
        } else {
            logger.debug("no isInState property found for need resource {}", needRes.getURI());
        }
        need.setWonNodeURI(URI.create(needRes.getPropertyResourceValue(WON.HAS_WON_NODE).toString()));

        Date lastUpdate = DateTimeUtils.parse(needRes.getProperty(DCTerms.modified).getString(), model);
        need.setLastUpdate(lastUpdate);

        return need;
    }
}
