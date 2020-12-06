package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.Shapes;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.annotation.Individual;
import won.shacl2java.annotation.Individuals;
import won.shacl2java.annotation.ShapeNode;
import won.shacl2java.sourcegen.typegen.mapping.IndividualClassNames;
import won.shacl2java.sourcegen.typegen.mapping.ProducerConsumerMap;
import won.shacl2java.sourcegen.typegen.TypesGenerator;
import won.shacl2java.util.NameUtils;

import java.util.*;
import java.util.stream.StreamSupport;

import static javax.lang.model.element.Modifier.*;
import static org.apache.jena.shacl.validation.VLib.focusNodes;

public class IndividualsGenerator implements TypesGenerator {
    private Shapes shapes;
    private Shacl2JavaConfig config;
    private ProducerConsumerMap.Producer individualClassNames;

    public IndividualsGenerator(Shapes shapes, Shacl2JavaConfig config,
                    IndividualClassNames.Producer individualClassNames) {
        this.shapes = shapes;
        this.config = config;
        this.individualClassNames = individualClassNames;
    }

    @Override
    public Set<TypeSpec> generate() {
        HashSet<TypeSpec> types = new HashSet<>();
        types.add(generateIndividuals(shapes, config));
        return types;
    }

    private TypeSpec generateIndividuals(Shapes shapes, Shacl2JavaConfig config) {
        TypeSpec.Builder individualsTypeBuilder = TypeSpec.classBuilder("Individuals")
                        .addModifiers(PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(Individuals.class).build());
        StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(shapes.iteratorAll(), Spliterator.ORDERED), false)
                        .distinct()
                        .forEach(shape -> {
                            // find focus nodes in the shapes graph itself
                            Collection<Node> focusNodes = focusNodes(shapes.getGraph(), shape);
                            for (Node focusNode : focusNodes) {
                                if (focusNode.isBlank() || focusNode.isLiteral()) {
                                    continue;
                                }
                                String name = NameUtils.enumConstantName(focusNode.getURI());
                                String typeName = NameUtils.classNameForShape(shape, config);
                                ClassName type = ClassName.get(config.getPackageName(),
                                                typeName);
                                // remember so we can inject individuals into shape-classes
                                individualClassNames.put(focusNode, type);
                                FieldSpec fieldSpec = FieldSpec.builder(type, name)
                                                .addModifiers(PUBLIC, STATIC, FINAL)
                                                .addAnnotation(
                                                                AnnotationSpec.builder(ClassName.get(Individual.class))
                                                                                .addMember("value", "$S",
                                                                                                focusNode.getURI())
                                                                                .build())
                                                .addAnnotation(
                                                                AnnotationSpec.builder(ShapeNode.class)
                                                                                .addMember("value",
                                                                                                "{ $S }",
                                                                                                shape.getShapeNode()
                                                                                                                .getURI())
                                                                                .build())
                                                .initializer("new $T()", type)
                                                .build();
                                individualsTypeBuilder.addField(fieldSpec);
                            }
                        });
        return individualsTypeBuilder.build();
    }
}
