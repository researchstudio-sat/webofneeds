package won.shacl2java.sourcegen.typegen.mapping;

import com.squareup.javapoet.TypeSpec;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import won.shacl2java.sourcegen.typegen.UpdateableTypeSpecMapping;
import won.shacl2java.sourcegen.typegen.support.ProducerConsumerMap;

import static won.shacl2java.util.CollectionUtils.addToMultivalueMap;

public class ShapeTypeImplTypes extends ProducerConsumerMap<URI, Set<TypeSpec>> implements UpdateableTypeSpecMapping {
    @Override
    public void applyChanges(Map<TypeSpec, TypeSpec> changedTypes) {
        this.internalMap = internalMap.entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                        e -> e.getKey(),
                                        e -> e.getValue()
                                                        .stream()
                                                        .map(ts -> changedTypes.getOrDefault(ts, ts))
                                                        .collect(Collectors.toSet())));
    }

    public ShapeTypeImplTypes.Producer producer() {
        return new ShapeTypeImplTypes.Producer();
    }

    public class Producer extends ProducerConsumerMap.Producer {
        public Producer() {
        }

        public void add(URI shapeTypeUri, TypeSpec implementingClass) {
            addToMultivalueMap(internalMap, shapeTypeUri, implementingClass);
        }
    }
}
