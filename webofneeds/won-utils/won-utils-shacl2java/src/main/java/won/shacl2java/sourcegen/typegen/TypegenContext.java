package won.shacl2java.sourcegen.typegen;

import com.squareup.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TypegenContext {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    Set<TypeSpec> typeSpecs = new HashSet<>();
    Set<UpdateableTypeSpecMapping> mappings = new HashSet<>();

    public void applyPostpropcessor(TypesPostprocessor postprocessor) {
        logger.debug("running typesPostprocessor {}", postprocessor);
        Map<TypeSpec, TypeSpec> changes = postprocessor.postprocess(typeSpecs);
        logger.debug("got {} changes from postprocessor {}", changes.size(), postprocessor);
        typeSpecs = typeSpecs
                        .stream()
                        .map(ts -> changes.getOrDefault(ts, ts))
                        .collect(Collectors.toSet());
        for (UpdateableTypeSpecMapping mapping : mappings) {
            mapping.applyChanges(changes);
        }
    }

    public void applyGenerator(TypesGenerator generator) {
        logger.debug("running typesGenerator {}", generator);
        Set<TypeSpec> newTypeSpecs = generator.generate();
        typeSpecs.addAll(newTypeSpecs);
        logger.debug("got {} new types from generator {}", newTypeSpecs.size(), generator);
    }

    public void manageMapping(UpdateableTypeSpecMapping mapping) {
        this.mappings.add(mapping);
    }

    public Set<TypeSpec> getTypeSpecs() {
        return Collections.unmodifiableSet(typeSpecs);
    }
}
