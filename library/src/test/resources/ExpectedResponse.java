import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import java.io.IOException;
import java.lang.Override;

public final class ResponseDeserializer extends JsonDeserializer {
    @Override
    public Response deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        Long id;if (parser.getCurrentToken() == null) {
            parser.nextToken();
        }
        if (parser.getCurrentToken != JsonToken.START_OBJECT) {
            parser.skipChildren();
            return null;
        }
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName;
            parser.nextToken();
            id = AutoJacksonUtils.parseField<Long>(parser);
        }
        return new AutoValue_Response(id);}
}
