package won.shacl2java.sourcegen.typegen.mapping;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Map;
import java.util.stream.Collectors;
import won.shacl2java.sourcegen.typegen.UpdateableTypeSpecMapping;
import won.shacl2java.sourcegen.typegen.support.ProducerConsumerMap;

public class TypeSpecNames extends ProducerConsumerMap<TypeName, TypeSpec> implements UpdateableTypeSpecMapping {
    @Override
    public void applyChanges(Map<TypeSpec, TypeSpec> changedTypes) {
        this.internalMap = internalMap.entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                        e -> e.getKey(),
                                        e -> changedTypes.getOrDefault(e.getValue(), e.getValue())));
    }
}
