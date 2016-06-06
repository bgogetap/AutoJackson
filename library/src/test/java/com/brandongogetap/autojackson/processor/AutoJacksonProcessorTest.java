package com.brandongogetap.autojackson.processor;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

public final class AutoJacksonProcessorTest {

    @Test
    public void testBuilderClassGeneratedAsExpected() {
        JavaFileObject sampleActivity = JavaFileObjects.forSourceLines("com.example.Response",
                "package com.example;",
                "import com.fasterxml.jackson.annotation.JsonProperty;",
                "import com.google.auto.value.AutoValue;",
                "import com.fasterxml.jackson.databind.annotation.JsonDeserialize;",
                "@AutoValue @JsonDeserialize(builder = AutoValue_Response.Builder.class) public abstract class Response {",
                "  @JsonProperty(\"id\") public abstract long id();",
                "}"
        );
        JavaFileObject expectedSource = JavaFileObjects.forSourceLines("AutoValue_Response",
                "package com.example;",
                "",
                "import com.fasterxml.jackson.annotation.JsonProperty;",
                "",
                "final class AutoValue_Response extends $AutoValue_Response {",
                "  AutoValue_Response(long id) {",
                "    super(id);",
                "  }",
                "",
                "  static class Builder {",
                "    private long id;",
                "    @JsonProperty(\"id\")",
                "    Builder id(long id) {",
                "      this.id = id;",
                "      return this;",
                "    }",
                "    Response build() {",
                "      return new AutoValue_Response(id);",
                "    }",
                "  }",
                "}"
        );
        assertAbout(javaSource())
                .that(sampleActivity)
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedSource);
    }

    @Test
    public void testThatNonAnnotatedMethodsGetCopiedToBuilderAndHaveGenericJsonPropertyAnnotationAdded() {
        JavaFileObject sampleActivity = JavaFileObjects.forSourceLines("com.example.Response",
                "package com.example;",
                "import com.fasterxml.jackson.annotation.JsonProperty;",
                "import com.google.auto.value.AutoValue;",
                "import com.fasterxml.jackson.databind.annotation.JsonDeserialize;",
                "@AutoValue @JsonDeserialize(builder = AutoValue_Response.Builder.class) public abstract class Response {",
                "  @JsonProperty(\"id\") public abstract long id();",
                "  public abstract String name();",
                "}"
        );
        JavaFileObject expectedSource = JavaFileObjects.forSourceLines("AutoValue_Response",
                "package com.example;",
                "",
                "import com.fasterxml.jackson.annotation.JsonProperty;",
                "import java.lang.String;",
                "",
                "final class AutoValue_Response extends $AutoValue_Response {",
                "  AutoValue_Response(long id, String name) {",
                "    super(id, name);",
                "  }",
                "",
                "  static class Builder {",
                "    private long id;",
                "    private String name;",
                "    @JsonProperty(\"id\")",
                "    Builder id(long id) {",
                "      this.id = id;",
                "      return this;",
                "    }",
                "    @JsonProperty(\"name\")",
                "    Builder name(String name) {",
                "      this.name = name;",
                "      return this;",
                "    }",
                "    Response build() {",
                "      return new AutoValue_Response(id, name);",
                "    }",
                "  }",
                "}"
        );
        assertAbout(javaSource())
                .that(sampleActivity)
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedSource);
    }

    @Test
    public void testMultipleAnnotationsAreCopiedOver() {
        JavaFileObject sampleActivity = JavaFileObjects.forSourceLines("com.example.Response",
                "package com.example;",
                "import com.fasterxml.jackson.annotation.JsonProperty;",
                "import com.google.auto.value.AutoValue;",
                "import com.fasterxml.jackson.databind.annotation.JsonDeserialize;",
                "import java.lang.Deprecated;",
                "@AutoValue @JsonDeserialize(builder = AutoValue_Response.Builder.class) public abstract class Response {",
                "  @JsonProperty(\"id\") @Deprecated public abstract long id();",
                "}"
        );
        JavaFileObject expectedSource = JavaFileObjects.forSourceLines("AutoValue_Response",
                "package com.example;",
                "",
                "import com.fasterxml.jackson.annotation.JsonProperty;",
                "import java.lang.Deprecated;",
                "",
                "final class AutoValue_Response extends $AutoValue_Response {",
                "  AutoValue_Response(long id) {",
                "    super(id);",
                "  }",
                "",
                "  static class Builder {",
                "    private long id;",
                "    @JsonProperty(\"id\")",
                "    @Deprecated",
                "    Builder id(long id) {",
                "      this.id = id;",
                "      return this;",
                "    }",
                "    Response build() {",
                "      return new AutoValue_Response(id);",
                "    }",
                "  }",
                "}"
        );
        assertAbout(javaSource())
                .that(sampleActivity)
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedSource);
    }

    @Test
    public void testThatClassesWithTypeParametersAreRejected() {
        JavaFileObject sampleActivity = JavaFileObjects.forSourceLines("com.example.Response",
                "package com.example;",
                "import com.fasterxml.jackson.annotation.JsonProperty;",
                "import com.google.auto.value.AutoValue;",
                "import com.fasterxml.jackson.databind.annotation.JsonDeserialize;",
                "@AutoValue @JsonDeserialize(builder = AutoValue_Response.Builder.class) public abstract class Response<T> {",
                "  @JsonProperty(\"param\") public abstract T param();",
                "  @JsonProperty(\"id\") public abstract long id();",
                "}"
        );
        assertAbout(javaSource())
                .that(sampleActivity)
                .processedWith(new AutoValueProcessor())
                .failsToCompile();
    }
}
