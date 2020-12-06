package won.shacl2java.sourcegen.typegen.support;

import com.squareup.javapoet.*;
import won.shacl2java.util.NameUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static javax.lang.model.element.Modifier.PUBLIC;

public class TypegenUtils {
    public static MethodSpec generateSetter(FieldSpec field) {
        return MethodSpec
                        .methodBuilder(NameUtils.setterNameForField(field))
                        .addParameter(field.type, field.name)
                        .addModifiers(PUBLIC)
                        .addStatement("this.$N = $N", field, field)
                        .build();
    }

    public static MethodSpec generateAdder(FieldSpec field) {
        TypeName baseType = ((ParameterizedTypeName) field.type).typeArguments.get(0);
        return MethodSpec
                        .methodBuilder(NameUtils.adderNameForFieldNameInPlural(field.name))
                        .addParameter(baseType, "toAdd")
                        .addModifiers(PUBLIC)
                        .beginControlFlow("if (this.$N == null)", field)
                        .addStatement("    this.$N = new $T()", field, ClassName.get(HashSet.class))
                        .endControlFlow()
                        .addStatement("this.$N.add($N)", field, "toAdd")
                        .build();
    }

    public static MethodSpec generateGetter(FieldSpec field) {
        MethodSpec.Builder mb = MethodSpec.methodBuilder(NameUtils.getterNameForField(field))
                        .returns(field.type)
                        .addModifiers(PUBLIC);
        if ((field.type instanceof ParameterizedTypeName)) {
            if (((ParameterizedTypeName) field.type).rawType.equals(ClassName.get(Set.class))) {
                mb.beginControlFlow("if (this.$N == null)", field.name)
                                .addStatement("return $T.emptySet()", ClassName.get(Collections.class))
                                .endControlFlow()
                                .addStatement("return $T.unmodifiableSet($N)", Collections.class, field.name);
                return mb.build();
            }
        }
        mb.addStatement("return this.$N", field.name);
        return mb.build();
    }
}
