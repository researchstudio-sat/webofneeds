package won.node.camel.processor.facet.reviewFacet;

import java.net.URI;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FacetMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Need;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: MS Date: 12.12.2018
 */
@Component
@FacetMessageProcessor(facetType = WON.REVIEW_FACET_STRING, direction = WONMSG.TYPE_FROM_EXTERNAL_STRING, messageType = WONMSG.TYPE_CONNECT_STRING)
public class ConnectFromNodeReviewFacetImpl extends AbstractCamelProcessor {
    @Override
    public void process(final Exchange exchange) {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI connectionUri = wonMessage.getReceiverURI();
        // Connection con = connectionRepository.findOneByConnectionURI(connectionUri);

        try {
            Map<Property, String> reviewData = WonRdfUtils.MessageUtils.getReviewContent(wonMessage);
            if (reviewData != null) {
                addReviewToNeed(reviewData, connectionUri);
            } else {
                logger.debug("No review data found in message: {}", wonMessage);
            }
        } catch (IllegalArgumentException e) {
            logger.debug("{}: for {}", e, wonMessage);
        }
    }

    private void addReviewToNeed(Map<Property, String> reviewData, URI connectionUri) throws IllegalArgumentException {

        String aboutNeedURI = reviewData.get(SCHEMA.ABOUT);
        Double rating = Double.parseDouble(reviewData.get(SCHEMA.RATING_VALUE)) > 0.0
                ? Double.parseDouble(reviewData.get(SCHEMA.RATING_VALUE)) : 0.0;

        Need aboutNeed = needRepository.findOneByNeedURI(URI.create(aboutNeedURI));
        Dataset needDataset = aboutNeed.getDatatsetHolder().getDataset();
        Model derivationModel = needDataset.getNamedModel(aboutNeed.getNeedURI() + "#derivedData");
        Resource aboutNeedResource = derivationModel.getResource(aboutNeedURI);
        Resource conRes = derivationModel.getResource(connectionUri.toString());

        Statement reviewdConnectionsProperty = derivationModel.getProperty(aboutNeedResource, WON.REVIEWED_CONNECTION);
        if (reviewdConnectionsProperty == null) {
            derivationModel.add(aboutNeedResource, WON.REVIEWED_CONNECTION, conRes);
            reviewdConnectionsProperty = derivationModel.getProperty(aboutNeedResource, WON.REVIEWED_CONNECTION);
        } else {
            Property ratedConnections = derivationModel.getProperty(WON.REVIEWED_CONNECTION.toString());
            if (derivationModel.contains(aboutNeedResource, ratedConnections, conRes)) {
                logger.debug("Connection already reviewed {}", connectionUri.toString());
                throw new IllegalArgumentException("Connection already reviewed");
            } else {
                derivationModel.add(aboutNeedResource, ratedConnections, conRes);
            }
        }

        Statement aggregateRatingProperty = derivationModel.getProperty(aboutNeedResource, SCHEMA.AGGREGATE_RATING);
        if (aggregateRatingProperty == null) {
            derivationModel.addLiteral(aboutNeedResource, SCHEMA.AGGREGATE_RATING, rating);
            aggregateRatingProperty = derivationModel.getProperty(aboutNeedResource, SCHEMA.AGGREGATE_RATING);
        }

        int ratingCount = 0;
        Statement reviewCountProperty = derivationModel.getProperty(aboutNeedResource, SCHEMA.REVIEW_COUNT);
        if (reviewCountProperty != null) {
            ratingCount = reviewCountProperty.getInt();
        } else {
            derivationModel.addLiteral(aboutNeedResource, SCHEMA.REVIEW_COUNT, 0.0);
            reviewCountProperty = derivationModel.getProperty(aboutNeedResource, SCHEMA.REVIEW_COUNT);
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

        aboutNeed.getDatatsetHolder().setDataset(needDataset);
        needRepository.save(aboutNeed);
    }
}
