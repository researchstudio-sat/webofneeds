package won.shacl2java.runtime;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.runtime.model.GraphEntity;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URI;

public class ToRdfUtils {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static Node getAsNode(Object value) {
        if (value instanceof GraphEntity) {
            GraphEntity entity = (GraphEntity) value;
            return getNodeSetToBlankNodeIfNull(entity);
        }
        if (value instanceof Boolean || value instanceof Number || value instanceof String
                        || value instanceof XSDDateTime) {
            return NodeFactory.createLiteralByValue(value, TypeMapper.getInstance().getTypeByValue(value));
        }
        if (value instanceof URI) {
            return NodeFactory.createURI(value.toString());
        }
        if (value instanceof Enum) {
            // if we created it, it has a getValue() method -> use that
            try {
                Method getValue = value.getClass().getMethod("getValue");
                if (getValue != null) {
                    return getAsNode(getValue.invoke(value));
                }
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Error converting value {} to Node", value, e);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Could not convert value {} to Node", value);
        }
        return null;
    }

    public static Node getNodeSetToBlankNodeIfNull(GraphEntity entity) {
        Node node = entity.getNode();
        if (node == null) {
            node = NodeFactory.createBlankNode();
            entity.setNode(node);
        }
        return node;
    }
}
