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
public class AtomModelMapper implements ModelMapper<Atom> {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Model toModel(Atom atom) {
        Model model = ModelFactory.createDefaultModel();
        Resource atomResource = model.createResource(atom.getAtomURI().toString(), WON.Atom);
        // set creation date
        Literal creationDate = DateTimeUtils.toLiteral(atom.getCreationDate(), model);
        if (creationDate != null) {
            model.add(model.createStatement(atomResource, DCTerms.created, creationDate));
        }
        // set modified date
        Literal lastUpdate = DateTimeUtils.toLiteral(atom.getLastUpdate(), model);
        if (lastUpdate != null) {
            model.add(model.createStatement(atomResource, DCTerms.modified, lastUpdate));
        }
        // set state
        model.add(model.createStatement(atomResource, WON.atomState, WON.toResource(atom.getState())));
        // We don't add the atom owner's endpoint here as this is confidential
        // information
        return model;
    }

    @Override
    public Atom fromModel(Model model) {
        Atom atom = new Atom();
        ResIterator atomIt = model.listSubjectsWithProperty(RDF.type, WON.Atom);
        if (!atomIt.hasNext())
            throw new IllegalArgumentException("at least one RDF node must be of type won:Atom");
        Resource atomRes = atomIt.next();
        logger.debug("processing atom resource {}", atomRes.getURI());
        atom.setAtomURI(URI.create(atomRes.getURI()));
        Statement dateStat = atomRes.getProperty(DCTerms.created);
        if (dateStat != null && dateStat.getObject().isLiteral()) {
            atom.setCreationDate(DateTimeUtils.toDate(dateStat.getObject().asLiteral(), model));
            logger.debug("found atomCreationDate literal value '{}'", dateStat.getObject().asLiteral().getString());
        } else {
            logger.debug("no atomCreationDate property found for atom resource {}", atomRes.getURI());
        }
        Statement stateStat = atomRes.getProperty(WON.atomState);
        if (stateStat != null && stateStat.getObject().isResource()) {
            URI uri = URI.create(stateStat.getResource().getURI());
            atom.setState(AtomState.parseString(uri.getFragment()));
            logger.debug("found atomState literal value '{}'", stateStat.getObject().asResource().getURI());
        } else {
            logger.debug("no atomState property found for atom resource {}", atomRes.getURI());
        }
        atom.setWonNodeURI(URI.create(atomRes.getPropertyResourceValue(WON.wonNode).toString()));
        Date lastUpdate = DateTimeUtils.parse(atomRes.getProperty(DCTerms.modified).getString(), model);
        atom.setLastUpdate(lastUpdate);
        return atom;
    }
}
