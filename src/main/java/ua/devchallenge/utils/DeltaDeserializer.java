package ua.devchallenge.utils;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import difflib.ChangeDelta;
import difflib.Chunk;
import difflib.DeleteDelta;
import difflib.Delta;
import difflib.InsertDelta;
import lombok.SneakyThrows;

public class DeltaDeserializer extends StdDeserializer<Delta> {

    public DeltaDeserializer() {
        this(null);
    }

    public DeltaDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Delta deserialize(JsonParser jp, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = jp.getCodec().readTree(jp);
        String type = node.get("type").textValue();

        Chunk revised = getChuck(mapper, node.get("revised"));
        Chunk original = getChuck(mapper, node.get("original"));

        switch (Delta.TYPE.valueOf(type.toUpperCase())) {
            case CHANGE:
                return new ChangeDelta(original, revised);
            case INSERT:
                return new InsertDelta(original, revised);
            case DELETE:
                return new DeleteDelta(original, revised);
        }
        return null;
    }

    @SneakyThrows
    private Chunk getChuck(ObjectMapper objectMapper, JsonNode jsonNode) {
        if (!jsonNode.has("position")) {
            return null;
        }
        int position = jsonNode.get("position").asInt();
        List lines = objectMapper.treeToValue(jsonNode.get("lines"), List.class);
        return new Chunk(position, lines);
    }
}
