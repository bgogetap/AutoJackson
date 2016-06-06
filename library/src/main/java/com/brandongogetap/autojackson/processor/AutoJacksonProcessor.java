package com.brandongogetap.autojackson.processor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.squareup.javapoet.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AutoService(AutoValueExtension.class)
public class AutoJacksonProcessor extends AutoValueExtension {

    private static final class Property {
        final String methodName;
        final String humanName;
        final ExecutableElement element;
        final TypeName type;
        final ImmutableSet<AnnotationMirror> annotations;

        Property(String humanName, ExecutableElement element) {
            this.methodName = element.getSimpleName().toString();
            this.humanName = humanName;
            this.element = element;
            type = TypeName.get(element.getReturnType());
            annotations = buildAnnotations(element);
        }

        private ImmutableSet<AnnotationMirror> buildAnnotations(ExecutableElement element) {

            ImmutableSet.Builder<AnnotationMirror> builder = ImmutableSet.builder();
            for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
                builder.add(annotation);
            }
            return builder.build();
        }
    }

    @Override
    public boolean applicable(Context context) {
        for (AnnotationMirror annotationMirror : context.autoValueClass().getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().asElement().getSimpleName().toString().equals("JsonDeserialize")) {
                if (context.autoValueClass().getTypeParameters().isEmpty()) {
                    return true;
                } else {
                    context.processingEnvironment().getMessager().printMessage(
                            Diagnostic.Kind.ERROR, "Cannot create Builder on classes with type parameters: "
                                    + context.autoValueClass().getSimpleName() + "<"
                                    + context.autoValueClass().getTypeParameters().toString() + ">");
                }
            }
        }
        return super.applicable(context);
    }

    @Override
    public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
        ImmutableList<Property> properties = readProperties(context.properties());

        List<? extends TypeParameterElement> typeParameterElements = context.autoValueClass().getTypeParameters();
        List<TypeVariableName> typeVariables = new ArrayList<>();
        for (TypeParameterElement typeParameterElement : typeParameterElements) {
            typeVariables.add(TypeVariableName.get(typeParameterElement));
        }

        @Deprecated

        TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.FINAL)
                .superclass(generateSuperType(context, classToExtend, typeVariables))
                .addTypeVariables(typeVariables)
                .addMethod(generateConstructor(properties))
                .addType(getBuilderTypeSpec(context, ClassName.get(context.packageName(), className), typeVariables));

        JavaFile javaFile = JavaFile.builder(context.packageName(), subclass.build()).build();
        return javaFile.toString();
    }

    private TypeName generateSuperType(Context context, String classToExtend, List<TypeVariableName> typeVariables) {
        ClassName superClass = ClassName.get(context.packageName(), classToExtend);
        TypeName superType;
        if (typeVariables.size() > 0) {
            //noinspection ConfusingArgumentToVarargsMethod
            superType = ParameterizedTypeName.get(superClass, typeVariables.toArray(new TypeVariableName[]{}));
        } else {
            superType = superClass;
        }
        return superType;
    }

    private TypeSpec getBuilderTypeSpec(Context context, ClassName className, List<TypeVariableName> typeVariableNames) {
        TypeSpec.Builder builder = TypeSpec.classBuilder("Builder")
                .addTypeVariables(typeVariableNames)
                .addModifiers(Modifier.STATIC);
        ImmutableList<Property> properties = readProperties(context.properties());
        List<ParameterSpec> args = new ArrayList<>();

        StringBuilder returnFormat = new StringBuilder();
        for (Property property : properties) {
            ParameterSpec spec = ParameterSpec.builder(property.type, property.humanName).build();
            args.add(spec);
            returnFormat.append("$N, ");
            builder.addField(FieldSpec.builder(property.type, property.humanName, Modifier.PRIVATE).build());
            builder.addMethod(addBuilderMethod(property, typeVariableNames));
        }
        String result = returnFormat.toString().substring(0, returnFormat.length() - 2);

        ClassName returnClass = ClassName.get(context.autoValueClass());
        TypeName returnType;
        if (typeVariableNames.size() > 0) {
            //noinspection ConfusingArgumentToVarargsMethod
            returnType = ParameterizedTypeName.get(returnClass, typeVariableNames.toArray(new TypeVariableName[]{}));
        } else {
            returnType = returnClass;
        }

        builder.addMethod(MethodSpec.methodBuilder("build")
                .returns(returnType)
                .addCode("return new $T(", className)
                .addCode(result + ");\n", args.toArray())
                .build());
        return builder.build();
    }

    private MethodSpec addBuilderMethod(Property property, List<TypeVariableName> typeVariableNames) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(property.humanName)
                .addParameter(property.type, property.humanName);
        boolean hasJsonPropertyAnnotation = false;
        for (AnnotationMirror annotation : property.annotations) {
            builder.addAnnotation(AnnotationSpec.get(annotation));
            if (annotation.getAnnotationType().asElement().getSimpleName().toString().equals("JsonProperty")) {
                hasJsonPropertyAnnotation = true;
            }
        }
        if (!hasJsonPropertyAnnotation) {
            builder.addAnnotation(AnnotationSpec.builder(JsonProperty.class)
                    .addMember("value", "$S", property.humanName)
                    .build());
        }
        ClassName returns = ClassName.bestGuess("Builder");
        TypeName returnType;
        if (typeVariableNames.size() > 0) {
            //noinspection ConfusingArgumentToVarargsMethod
            returnType = ParameterizedTypeName.get(returns, typeVariableNames.toArray(new TypeVariableName[]{}));
        } else {
            returnType = returns;
        }
        builder.returns(returnType)
                .addCode("\nthis.$N = $N;\nreturn this;\n", property.humanName, property.humanName);
        return builder.build();
    }

    private MethodSpec generateConstructor(ImmutableList<Property> properties) {
        List<ParameterSpec> params = Lists.newArrayListWithCapacity(properties.size());
        for (Property property : properties) {
            ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(property.type, property.humanName);
            params.add(parameterBuilder.build());
        }

        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addParameters(params);

        StringBuilder superFormat = new StringBuilder("super(");
        List<ParameterSpec> args = new ArrayList<>();
        for (int i = 0, n = params.size(); i < n; i++) {
            args.add(params.get(i));
            superFormat.append("$N");
            if (i < n - 1) superFormat.append(", ");
        }
        superFormat.append(")");
        builder.addStatement(superFormat.toString(), args.toArray());

        return builder.build();
    }

    private ImmutableList<Property> readProperties(Map<String, ExecutableElement> properties) {
        ImmutableList.Builder<Property> values = ImmutableList.builder();
        for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
            values.add(new Property(entry.getKey(), entry.getValue()));
        }
        return values.build();
    }
}
