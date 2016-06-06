package com.brandongogetap.autojackson.demo;

import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.fail;

public final class JacksonTest {

    String apiResponse = "{\"id\":2,\"name\":\"brandon\"}";

    @Test
    public void testJacksonParsesIntoReponseObject() {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Demo.Response response = objectMapper.readValue(apiResponse, Demo.Response.class);
            assertEquals((long)response.id(), 2L);
            TestCase.assertEquals(response.name(), "brandon");
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
