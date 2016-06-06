package com.brandongogetap.autojackson.processor;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Ignore;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

@Ignore("Work in progress")
public final class DeserializerTest {

    @Test
    public void testDeserializer() {
        JavaFileObject sampleActivity = JavaFileObjects.forSourceLines("com.example.Response",
                "package com.example;",
                "import com.fasterxml.jackson.annotation.JsonProperty;",
                "import com.fasterxml.jackson.databind.annotation.JsonDeserialize;",
                "import com.google.auto.value.AutoValue;",
                "@AutoValue @JsonDeserialize(using = ResponseDeserializer.class) public abstract class Response {",
                "  @JsonProperty(\"id\") public abstract Long id();",
                "  @JsonProperty(\"name\") public abstract String name();",
                "}"
        );
        JavaFileObject expectedSource = JavaFileObjects.forSourceLines("com.example.ResponseDeserializer",
                "package com.example;",
                "import com.fasterxml.jackson.annotation.JsonProperty;",
                "import com.fasterxml.jackson.databind.annotation.JsonDeserialize;",
                "import com.google.auto.value.AutoValue;",
                "@AutoValue @JsonDeserialize(using = ResponseDeserializer.class) public class ResponseDeserializer {",
                "  @JsonProperty(\"id\") public abstract Long id();",
                "  @JsonProperty(\"name\") public abstract String name();",
                "}");

        assertAbout(javaSource())
                .that(sampleActivity)
                .processedWith(new AutoValueProcessor(), new AutoJacksonDeserializer())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedSource);
    }

    @Test public void testJacksonDeserializesCorrectly() {

    }
}
