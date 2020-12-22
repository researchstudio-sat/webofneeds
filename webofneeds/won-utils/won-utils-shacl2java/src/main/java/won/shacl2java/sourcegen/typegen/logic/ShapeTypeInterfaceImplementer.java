package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.vocabulary.RDF;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.sourcegen.typegen.TypesPostprocessor;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeImplTypes;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeInterfaceTypes;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeSpecs;

public class ShapeTypeInterfaceImplementer implements TypesPostprocessor {
    private Shacl2JavaConfig config;
    private Shapes shapes;
    private ShapeTypeInterfaceTypes.Consumer shapeTypeInterfaceTypes;
    private ShapeTypeSpecs.Consumer shapeTypeSpecs;
    private ShapeTypeImplTypes.Producer shapeTypeImplTypes;

    public ShapeTypeInterfaceImplementer(Shapes shapes,
                    Shacl2JavaConfig config,
                    ShapeTypeSpecs.Consumer shapeTypeSpecs,
                    ShapeTypeInterfaceTypes.Consumer shapeTypeInterfaceTypes,
                    ShapeTypeImplTypes.Producer shapeTypeImplTypes) {
        this.config = config;
        this.shapes = shapes;
        this.shapeTypeInterfaceTypes = shapeTypeInterfaceTypes;
        this.shapeTypeSpecs = shapeTypeSpecs;
        this.shapeTypeImplTypes = shapeTypeImplTypes;
    }

    @Override
    public Map<TypeSpec, TypeSpec> postprocess(Set<TypeSpec> typeSpecs) {
        if (!config.isInterfacesForRdfTypes()) {
            return Collections.emptyMap();
        }
        return StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(shapes.iteratorAll(), Spliterator.ORDERED), false)
                        .distinct()
                        .filter(s -> s.isNodeShape())
                        .map(shape -> {
                            TypeSpec impl = shapeTypeSpecs.get(shape).get();
                            TypeSpec.Builder implBuilder = impl.toBuilder();
                            Set<URI> shapeTypesImplemented = new HashSet<>();
                            shape.getShapeGraph()
                                            .find(shape.getShapeNode(), RDF.type.asNode(), null)
                                            .toSet().stream()
                                            .map(Triple::getObject)
                                            .map(Node::getURI)
                                            .filter(u -> !u.startsWith(SHACL.getURI()))
                                            .map(u -> URI.create(u))
                                            .forEach(u -> {
                                                TypeSpec iFace = shapeTypeInterfaceTypes.get(u).get();
                                                implBuilder.addSuperinterface(
                                                                ClassName.get(config.getPackageName(), iFace.name));
                                                shapeTypesImplemented.add(u);
                                            });
                            if (shapeTypesImplemented.isEmpty()) {
                                return null;
                            }
                            TypeSpec modifiedImpl = implBuilder.build();
                            shapeTypesImplemented.forEach(typeUri -> shapeTypeImplTypes.add(typeUri, modifiedImpl));
                            return new ImmutablePair<TypeSpec, TypeSpec>(impl, modifiedImpl);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(p -> p.getLeft(), p -> p.getRight()));
    }
}
