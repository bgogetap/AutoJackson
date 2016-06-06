package com.brandongogetap.autojackson.processor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

import static com.squareup.javapoet.ClassName.bestGuess;

//@AutoService(Processor.class)
public final class AutoJacksonDeserializer extends AbstractProcessor {

    private static final String PARSER_VARIABLE_NAME = "parser";

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;
    private ImmutableMap.Builder<String, ClassName> argMap;
    private StringBuilder returnArgumentStringBuilder;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(JsonDeserialize.class)) {
            try {
                String classPackage = getPackageName(element);
                ImmutableList<Element> methods = ImmutableList.copyOf(element.getEnclosedElements());
                TypeSpec.Builder deserializerBuilder = TypeSpec.classBuilder(element.getSimpleName() + "Deserializer")
                        .superclass(ParameterizedTypeName.get(ClassName.get(JsonDeserializer.class), ClassName.get(element.asType())))
                        .addModifiers(Modifier.FINAL);

                returnArgumentStringBuilder = new StringBuilder();
                argMap = new ImmutableMap.Builder<>();

                for (Element method : methods) {
                    String elementName = method.getSimpleName().toString();
                    if (elementName.equals("<init>")) {
                        continue;
                    }
                    ClassName className = ClassName.bestGuess(method.asType().toString().substring(method.asType().toString().lastIndexOf("" +
                            ".") + 1));
                    deserializerBuilder.addField(bestGuess(className.simpleName()), method.getSimpleName().toString());
                    argMap.put(elementName, className);
                    returnArgumentStringBuilder.append(elementName + ", ");
                }
                deserializerBuilder.addMethod(generateDeserializeMethod(element, methods))
                        .addMethod(generateParseMethod(methods));
                JavaFile javaFile = JavaFile.builder(classPackage, deserializerBuilder.build()).build();
                javaFile.writeTo(filer);
            } catch (IOException e) {
                error(element, "Unable to create deserializer method\n\n%s", e.getMessage());
            }
        }
        return true;
    }

    private String getPackageName(Element element) {
        return elementUtils.getPackageOf(element).getQualifiedName().toString();
    }

    private MethodSpec generateDeserializeMethod(Element element, ImmutableList<Element> methods) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("deserialize")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(JsonParser.class, PARSER_VARIABLE_NAME).build())
                .addParameter(ParameterSpec.builder(DeserializationContext.class, "context").build())
                .addException(IOException.class)
                .returns(ClassName.get(element.asType()))
                .beginControlFlow("if ($L.getCurrentToken() == null)", PARSER_VARIABLE_NAME)
                .addStatement("$L.nextToken()", PARSER_VARIABLE_NAME)
                .endControlFlow()
                .beginControlFlow("if ($L.getCurrentToken() != $T.START_OBJECT)", PARSER_VARIABLE_NAME, JsonToken.class)
                .addStatement("$L.skipChildren()", PARSER_VARIABLE_NAME)
                .addStatement("return null")
                .endControlFlow()
                .beginControlFlow("while ($L.nextToken() != $T.END_OBJECT)", PARSER_VARIABLE_NAME, JsonToken.class)
                .addStatement("String fieldName = $L.getCurrentName()", PARSER_VARIABLE_NAME)
                .addStatement("$L.nextToken()", PARSER_VARIABLE_NAME)
                .addStatement("parse(fieldName, $L)", PARSER_VARIABLE_NAME)
                .addStatement("$L.skipChildren()", PARSER_VARIABLE_NAME)
                .endControlFlow();
        String returnClass = getReturnedClassName(element.getSimpleName().toString(), element.getEnclosingElement());
        String args = returnArgumentStringBuilder.substring(0, returnArgumentStringBuilder.length() - 2);
        builder.addCode("return new AutoValue_$L(" + args, returnClass);
        builder.addCode(");\n");
        return builder.build();
    }

    private String getReturnedClassName(String base, Element enclosingElement) {
        if (enclosingElement.getKind().isClass()) {
            base = enclosingElement.getSimpleName().toString() + "_" + base;
            return getReturnedClassName(base, enclosingElement.getEnclosingElement());
        }
        return base;
    }

    private MethodSpec generateParseMethod(ImmutableList<Element> methods) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("parse")
                .addParameter(ParameterSpec.builder(String.class, "fieldName").build())
                .addParameter(ParameterSpec.builder(JsonParser.class, "parser").build())
                .addException(IOException.class);

        //TODO
        ImmutableMap<String, ClassName> map = argMap.build();
        boolean firstStatement = true;
        for (String arg : map.keySet()) {
            String ifString = firstStatement ? "if " : "else if ";
            if (firstStatement) {
                firstStatement = false;
            }
            builder.beginControlFlow("$L($S.equals(fieldName))", ifString, arg)
                    .addStatement("$L = parser.$L", arg, getParserMethod(map.get(arg)))
                    .endControlFlow();
        }

        return builder.build();
    }

    private String getParserMethod(ClassName argClass) {
        switch (argClass.simpleName()) {
            case ("Long"):
                return "getValueAsLong()";
            case ("String"):
                return "getValueAsString()";
            default:
                return null;
        }
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(AutoValue.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
