package won.shacl2java.sourcegen.typegen.support;

import com.squareup.javapoet.*;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.util.NameUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    public static MethodSpec generateSetterForBuilder(FieldSpec field, TypeName builderType, String productFieldName) {
        String setterName = NameUtils.setterNameForField(field);
        return MethodSpec
                        .methodBuilder(setterName)
                        .addParameter(field.type, field.name)
                        .addModifiers(PUBLIC)
                        .returns(builderType)
                        .addStatement(String.format("this.%s.%s($N)", productFieldName, setterName), field)
                        .addStatement("return this")
                        .build();
    }

    public static MethodSpec generateSetterByBuilderForBuilder(FieldSpec field, TypeName builderType,
                    String productFieldName, Shacl2JavaConfig config) {
        String setterName = NameUtils.setterNameForField(field);
        ClassName fieldBuilderClass = ClassName.get(config.getPackageName(), field.type.toString() + ".Builder");
        return MethodSpec
                        .methodBuilder(setterName)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class), fieldBuilderClass),
                                        "builderConsumer")
                        .addModifiers(PUBLIC)
                        .returns(builderType)
                        .addStatement("$T subBuilder = $T.builder()", fieldBuilderClass, field.type)
                        .addStatement("builderConsumer.accept(subBuilder)")
                        .addStatement(String.format("this.%s.%s(subBuilder.build())", productFieldName, setterName),
                                        field)
                        .addStatement("return this")
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

    public static MethodSpec generateAdderForBuilder(FieldSpec field, TypeName builderTypeName,
                    String productFieldName) {
        TypeName baseType = ((ParameterizedTypeName) field.type).typeArguments.get(0);
        String adderName = NameUtils.adderNameForFieldNameInPlural(field.name);
        return MethodSpec
                        .methodBuilder(adderName)
                        .addParameter(baseType, "toAdd")
                        .addModifiers(PUBLIC)
                        .returns(builderTypeName)
                        .addStatement(String.format("this.%s.%s(toAdd)", productFieldName, adderName))
                        .addStatement("return this")
                        .build();
    }

    public static MethodSpec generateAdderByBuilderForBuilder(FieldSpec field, TypeName builderTypeName,
                    String productFieldName, Shacl2JavaConfig config) {
        TypeName baseType = ((ParameterizedTypeName) field.type).typeArguments.get(0);
        String adderName = NameUtils.adderNameForFieldNameInPlural(field.name);
        ClassName fieldBuilderClass = ClassName.get(config.getPackageName(), baseType.toString() + ".Builder");
        return MethodSpec
                        .methodBuilder(adderName)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class), fieldBuilderClass),
                                        "builderConsumer")
                        .addModifiers(PUBLIC)
                        .returns(builderTypeName)
                        .addStatement("$T subBuilder = $T.builder()", fieldBuilderClass, baseType)
                        .addStatement("builderConsumer.accept(subBuilder)")
                        .addStatement(String.format("this.%s.%s(subBuilder.build())", productFieldName, adderName))
                        .addStatement("return this")
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

    /**
     * Finds the common superclass. Taken from
     * https://stackoverflow.com/questions/9797212/finding-the-nearest-common-superclass-or-superinterface-of-a-collection-of-cla
     * 
     * @param classes
     * @return
     */
    public static List<Class<?>> commonSuperClass(Class<?>... classes) {
        // start off with set from first hierarchy
        Set<Class<?>> rollingIntersect = new LinkedHashSet<Class<?>>(
                        getClassesBfs(classes[0]));
        // intersect with next
        for (int i = 1; i < classes.length; i++) {
            rollingIntersect.retainAll(getClassesBfs(classes[i]));
        }
        return new LinkedList<Class<?>>(rollingIntersect);
    }

    /**
     * @param clazz
     * @return
     */
    private static Set<Class<?>> getClassesBfs(Class<?> clazz) {
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        Set<Class<?>> nextLevel = new LinkedHashSet<Class<?>>();
        nextLevel.add(clazz);
        do {
            classes.addAll(nextLevel);
            Set<Class<?>> thisLevel = new LinkedHashSet<Class<?>>(nextLevel);
            nextLevel.clear();
            for (Class<?> each : thisLevel) {
                Class<?> superClass = each.getSuperclass();
                if (superClass != null && superClass != Object.class) {
                    nextLevel.add(superClass);
                }
                for (Class<?> eachInt : each.getInterfaces()) {
                    nextLevel.add(eachInt);
                }
            }
        } while (!nextLevel.isEmpty());
        return classes;
    }

    /**
     * Finds the common superclass. Adapted from
     * https://stackoverflow.com/questions/9797212/finding-the-nearest-common-superclass-or-superinterface-of-a-collection-of-cla
     *
     * @return
     */
    public static TypeName findCommonSuperclassOrSuperinterface(Set<FieldSpec> fieldSpecs,
                    Map<TypeName, TypeSpec> typeSpecs, Shacl2JavaConfig config) {
        // start off with set from first hierarchy
        Set<TypeName> rollingIntersect = null;
        for (FieldSpec fieldSpec : fieldSpecs) {
            TypeSpec ts = typeSpecs.get(fieldSpec.type);
            if (ts == null && fieldSpec.type instanceof ParameterizedTypeName) {
                TypeName typeArg = ((ParameterizedTypeName) fieldSpec.type).typeArguments.get(0);
                ts = typeSpecs.get(typeArg);
            }
            if (ts == null) {
                // failed to find the field spec. if all fields have the same value, use that,
                // otherwise, use object
                if (fieldSpecs.stream().allMatch(fs -> fs.type.equals(fieldSpec.type))) {
                    return fieldSpec.type;
                } else {
                    return ClassName.OBJECT;
                }
            }
            if (rollingIntersect == null) {
                rollingIntersect = new LinkedHashSet<TypeName>(getSupertypesBfs(ts, typeSpecs, config));
            } else {
                rollingIntersect.retainAll(getSupertypesBfs(ts, typeSpecs, config));
            }
        }
        return rollingIntersect.stream().findFirst().orElse(ClassName.OBJECT);
    }

    private static List<TypeName> getSupertypesBfs(TypeSpec typeSpec, Map<TypeName, TypeSpec> typeSpecs,
                    Shacl2JavaConfig config) {
        Set<TypeSpec> classes = new LinkedHashSet<TypeSpec>();
        Set<TypeSpec> nextLevel = new LinkedHashSet<TypeSpec>();
        nextLevel.add(typeSpec);
        do {
            classes.addAll(nextLevel);
            Set<TypeSpec> thisLevel = new LinkedHashSet<TypeSpec>(nextLevel);
            nextLevel.clear();
            for (TypeSpec each : thisLevel) {
                TypeName superClass = each.superclass;
                if (superClass != null && !TypeName.OBJECT.equals(superClass) && typeSpecs.containsKey(superClass)) {
                    nextLevel.add(typeSpecs.get(superClass));
                }
                for (TypeName eachInt : each.superinterfaces) {
                    if (eachInt != null && typeSpecs.containsKey(eachInt)) {
                        nextLevel.add(typeSpecs.get(eachInt));
                    }
                }
            }
        } while (!nextLevel.isEmpty());
        return classes
                        .stream()
                        .filter(Objects::nonNull)
                        .map(ts -> ClassName.get(config.getPackageName(), ts.name))
                        .collect(Collectors.toList());
    }
}
