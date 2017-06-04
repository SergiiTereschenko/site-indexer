package ua.devchallenge.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import difflib.Delta;
import io.vertx.core.json.DecodeException;

public class Json {

    private static ObjectMapper MAPPER = new ObjectMapper();
    private static ObjectMapper prettyMapper = new ObjectMapper();

    static {
        MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        prettyMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        prettyMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(Delta.class, new DeltaDeserializer());
        MAPPER.registerModule(module);
        prettyMapper.registerModule(module);
    }

    public static <T> T decodeValue(String str, Class<T> clazz) throws DecodeException {
        try {
            return MAPPER.readValue(str, clazz);
        } catch (Exception e) {
            throw new DecodeException("Failed to decode:" + e.getMessage(), e);
        }
    }
}
