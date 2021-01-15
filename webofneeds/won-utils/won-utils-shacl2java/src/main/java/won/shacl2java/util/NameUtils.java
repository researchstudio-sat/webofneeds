package won.shacl2java.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathVisitorBase;
import won.shacl2java.Shacl2JavaConfig;

public class NameUtils {
    public static String setterNameForField(FieldSpec field) {
        return setterNameForField(field.name);
    }

    public static String setterNameForField(String fieldName) {
        return "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    public static String getterNameForField(FieldSpec field) {
        return getterNameForField(field.name);
    }

    public static String getterNameForField(String fieldName) {
        return "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    public static String adderNameForField(FieldSpec field) {
        return adderNameForFieldNameInPlural(field.name);
    }

    public static String adderNameForFieldNameInSingular(String fieldNameInSingular) {
        return "add" + fieldNameInSingular.substring(0, 1).toUpperCase() + fieldNameInSingular.substring(1);
    }

    public static String adderNameForFieldNameInPlural(String fieldNameInPlural) {
        String nameForm = singular(fieldNameInPlural);
        return "add" + nameForm.substring(0, 1).toUpperCase() + nameForm.substring(1);
    }

    public static String nameForShape(Shape shape) {
        return shape.getShapeNode().isBlank()
                        ? shape.getShapeNode().getBlankNodeLabel()
                        : shape.getShapeNode().getLocalName();
    }

    public static ClassName javaPoetClassNameForShape(Shape shape, Shacl2JavaConfig config) {
        return ClassName.get(config.getPackageName(), classNameForShape(shape, config));
    }

    public static String classNameForShape(Shape shape, Shacl2JavaConfig config) {
        if (shape.getShapeNode().isBlank()) {
            throw new IllegalArgumentException("Cannot generate classname for anonymous (blank node) shape");
        }
        String name = shape.getShapeNode().getLocalName();
        return classNameForShapeName(name, config);
    }

    public static String classNameForShapeURI(URI shapeURI, Shacl2JavaConfig config) {
        String s = shapeURI.getFragment();
        if (s == null) {
            s = shapeURI.getPath().substring(shapeURI.getPath().lastIndexOf("/"));
        }
        return classNameForShapeName(s, config);
    }

    public static String classNameForShapeName(String name, Shacl2JavaConfig config) {
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        if (config.getClassNameRegex() != null) {
            Matcher m = config.getClassNameRegex().matcher(name);
            return m.replaceAll(config.getClassNameReplacement());
        }
        return name;
    }

    public static Optional<String> propertyNameForPropertyShape(PropertyShape shape) {
        Path path = shape.getPath();
        return propertyNameForPath(path);
    }

    public static Optional<String> propertyNameForPath(Path path) {
        StringBuilder propertyName = new StringBuilder();
        path.visit(new PathVisitorBase() {
            @Override
            public void visit(P_Link pathNode) {
                propertyName.append(pathNode.getNode().getLocalName());
            }

            public void visit(P_Inverse pathNode) {
                Path uninverted = pathNode.getSubPath();
                if (uninverted instanceof P_Link) {
                    visit((P_Link) uninverted);
                    propertyName.append("Inv");
                }
            }
        });
        String ret = propertyName.toString();
        if (ret.length() == 0) {
            return Optional.empty();
        }
        return Optional.of(ret);
    }

    public static String plural(String name) {
        if (name.endsWith("s")) {
            return name + "es";
        } else {
            return name + "s";
        }
    }

    public static String singular(String plural) {
        if (plural.endsWith("ses")) {
            return plural.substring(0, plural.length() - 2);
        } else if (plural.endsWith("s")) {
            return plural.substring(0, plural.length() - 1);
        }
        return plural;
    }

    /**
     * Generates the name for a java enum constant.
     * 
     * @param value unless special handling is applied, its <code>toString()</code>
     * method is used to generate the string.
     * @return
     */
    public static String enumConstantName(Object value) {
        String s = null;
        if (value instanceof Node) {
            if (((Node) value).isURI()) {
                value = ((Node) value).getURI();
            } else if (((Node) value).isLiteral()) {
                value = ((Node) value).getLiteralLexicalForm();
            }
        }
        if (value instanceof URI) {
            URI u = (URI) value;
            s = u.getFragment();
            if (s == null) {
                s = u.getPath().substring(u.getPath().lastIndexOf("/"));
            }
        } else {
            s = value.toString();
            if (s.matches("\\w+://[\\w\\.]+(:\\d+)?(/\\w+)*/?(#[\\w/]+)?")) {
                int index = s.lastIndexOf("#");
                if (index >= 0) {
                    s = s.substring(index + 1);
                } else {
                    index = s.lastIndexOf("/");
                    if (index > 0) {
                        s = s.substring(index + 1);
                    }
                }
            }
        }
        s = s.replaceAll("[^A-Za-z0-9_]", "")
                        .replaceFirst("^(\\d)", "d$1")
                        .replaceAll("(\\p{Lower})(\\p{Upper})", "$1_$2");
        return s.toUpperCase();
    }
}
