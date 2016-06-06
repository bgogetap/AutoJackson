package com.brandongogetap.autojackson.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

public class Demo {

    public static void main(String[] args) {
        System.out.println("Hello");
    }

    @AutoValue
    @JsonDeserialize(builder = AutoValue_Demo_Response.Builder.class)
    static abstract class Response {

        @JsonProperty("id")
        public abstract Long id();

        public abstract String name();
    }

//    @AutoValue
//    @JsonDeserialize(using = StreamingResponseDeserializer.class)
//    static abstract class StreamingResponse {
//
//        public abstract Long id();
//    }
}
