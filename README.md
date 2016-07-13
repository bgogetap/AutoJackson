[ ![Download](https://api.bintray.com/packages/bgogetap/android/auto-jackson/images/download.svg) ](https://bintray.com/bgogetap/android/auto-jackson/_latestVersion) [![Build Status](https://travis-ci.org/bgogetap/AutoJackson.svg?branch=master)](https://travis-ci.org/bgogetap/AutoJackson)
# AutoJackson
## An Auto Value Extension that allows minimal setup of Auto Value classes when using Jackson

To use, annotate your @AutoValue class with @JsonDeserialize(builder = AutoValue_{YourClass}.Builder.class)
Include only the abstract methods in your class body. The Builder class will be generated for you.

Example:
```java
    @AutoValue
    @JsonDeserialize(builder = AutoValue_Response.Builder.class)
    public abstract class Response {

        @JsonProperty("id")
        public abstract Long id();

        @JsonProperty("name")
        public abstract String name();
    }
```
Annotating the abstract methods with @JsonProperty is optional. If left out, the annotation will be added for you in the Builder class using the method name as the value.

## Setup
```groovy
buildscript {
    repositories {
        jcenter()
    }
}

dependencies {
    apt 'com.brandongogetap:auto-jackson:0.1'
}
```
(Using the [android-apt](https://bitbucket.org/hvisser/android-apt) plugin)
