package won.shacl2java.sourcegen.typegen.mapping;

import com.squareup.javapoet.TypeSpec;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.sourcegen.typegen.UpdateableTypeSpecMapping;
import won.shacl2java.sourcegen.typegen.support.ProducerConsumerMap;

/**
 * For shapes that have additional <code>rdf:type</code> statements, interfaces
 * are generated and the classes corresponding to the shape implement the
 * respecting interface. This maps the rdf type to the interface spec.
 */
public class ShapeTypeInterfaceTypes extends ProducerConsumerMap<URI, TypeSpec> implements UpdateableTypeSpecMapping {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void applyChanges(Map<TypeSpec, TypeSpec> changedTypes) {
        logger.debug("applying {} changes to {}", changedTypes.size(), this);
        this.internalMap = internalMap.entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                        e -> e.getKey(),
                                        e -> changedTypes.getOrDefault(e.getValue(), e.getValue())));
    }
}
