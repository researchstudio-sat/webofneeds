package won.node.camel.processor.socket.reviewSocket;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.SocketMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.WONMSG;
import won.protocol.vocabulary.WXREVIEW;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Map;

/**
 * User: MS Date: 12.12.2018
 */
@Component
@SocketMessageProcessor(socketType = WXREVIEW.ReviewSocketString, direction = WONMSG.FromExternalString, messageType = WONMSG.ConnectMessageString)
public class ConnectFromNodeReviewSocketImpl extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void process(final Exchange exchange) {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI connectionUri = wonMessage.getRecipientURI();
        // Connection con = connectionRepository.findOneByConnectionURI(connectionUri);
        try {
            Map<Property, String> reviewData = WonRdfUtils.MessageUtils.getReviewContent(wonMessage);
            if (reviewData != null) {
                addReviewToAtom(reviewData, connectionUri);
            } else {
                logger.debug("No review data found in message: {}", wonMessage);
            }
        } catch (IllegalArgumentException e) {
            logger.debug("{}: for {}", e, wonMessage);
        }
    }

    private void addReviewToAtom(Map<Property, String> reviewData, URI connectionUri) throws IllegalArgumentException {
        String aboutAtomURI = reviewData.get(SCHEMA.ABOUT);
        Double rating = Double.parseDouble(reviewData.get(SCHEMA.RATING_VALUE)) > 0.0
                        ? Double.parseDouble(reviewData.get(SCHEMA.RATING_VALUE))
                        : 0.0;
        Atom aboutAtom = atomRepository.findOneByAtomURI(URI.create(aboutAtomURI));
        Dataset atomDataset = aboutAtom.getDatatsetHolder().getDataset();
        Model derivationModel = atomDataset.getNamedModel(aboutAtom.getAtomURI() + "#derivedData");
        Resource aboutAtomResource = derivationModel.getResource(aboutAtomURI);
        Resource conRes = derivationModel.getResource(connectionUri.toString());
        Statement reviewdConnectionsProperty = derivationModel.getProperty(aboutAtomResource,
                        WXREVIEW.reviewedConnection);
        if (reviewdConnectionsProperty == null) {
            derivationModel.add(aboutAtomResource, WXREVIEW.reviewedConnection, conRes);
            reviewdConnectionsProperty = derivationModel.getProperty(aboutAtomResource, WXREVIEW.reviewedConnection);
        } else {
            Property ratedConnections = derivationModel.getProperty(WXREVIEW.reviewedConnection.toString());
            if (derivationModel.contains(aboutAtomResource, ratedConnections, conRes)) {
                logger.debug("Connection already reviewed {}", connectionUri.toString());
                throw new IllegalArgumentException("Connection already reviewed");
            } else {
                derivationModel.add(aboutAtomResource, ratedConnections, conRes);
            }
        }
        Statement aggregateRatingProperty = derivationModel.getProperty(aboutAtomResource, SCHEMA.AGGREGATE_RATING);
        if (aggregateRatingProperty == null) {
            derivationModel.addLiteral(aboutAtomResource, SCHEMA.AGGREGATE_RATING, rating);
            aggregateRatingProperty = derivationModel.getProperty(aboutAtomResource, SCHEMA.AGGREGATE_RATING);
        }
        int ratingCount = 0;
        Statement reviewCountProperty = derivationModel.getProperty(aboutAtomResource, SCHEMA.REVIEW_COUNT);
        if (reviewCountProperty != null) {
            ratingCount = reviewCountProperty.getInt();
        } else {
            derivationModel.addLiteral(aboutAtomResource, SCHEMA.REVIEW_COUNT, 0.0);
            reviewCountProperty = derivationModel.getProperty(aboutAtomResource, SCHEMA.REVIEW_COUNT);
        }
        Double aggregateRating = aggregateRatingProperty.getDouble();
        Double ratingSum = 0.0;
        if (ratingCount < 1) {
            ratingSum = aggregateRating;
        } else {
            ratingSum = (aggregateRating * ratingCount) + rating;
        }
        ratingCount++;
        Double newAggregateRating = ratingSum / ratingCount;
        aggregateRatingProperty.changeLiteralObject(newAggregateRating);
        reviewCountProperty.changeLiteralObject(ratingCount);
        aboutAtom.getDatatsetHolder().setDataset(atomDataset);
        atomRepository.save(aboutAtom);
    }
}
