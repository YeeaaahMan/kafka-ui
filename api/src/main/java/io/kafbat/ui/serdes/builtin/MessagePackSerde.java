package io.kafbat.ui.serdes.builtin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.util.Map;
import java.util.Optional;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.msgpack.value.Value;


public class MessagePackSerde implements BuiltInSerde {
  public static final String NAME = "MessagePack";

  public static final String FAILED_TO_DESERIALIZE_MSGPACK_PAYLOAD = "Failed to deserialize MessagePack payload";
  public static final String FAILED_TO_SERIALIZE_JSON_PAYLOAD = "Failed to parse JSON payload";

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
  private static final ObjectMapper MSGPACK_MAPPER = new ObjectMapper(new MessagePackFactory());

  public static String name() {
    return NAME;
  }

  @Override
  public Optional<String> getDescription() {
    return Optional.empty();
  }

  @Override
  public Optional<SchemaDescription> getSchema(String topic, Serde.Target type) {
    return Optional.empty();
  }

  @Override
  public boolean canDeserialize(String topic, Serde.Target type) {
    return true;
  }

  @Override
  public Serde.Deserializer deserializer(String topic, Serde.Target type) {
    return (headers, data) -> {
      try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
        Value value = unpacker.unpackValue();
        JsonNode node = MSGPACK_MAPPER.readTree(value.toJson());
        return new DeserializeResult(node.toString(), DeserializeResult.Type.STRING, Map.of());
      } catch (Exception e) {
        throw new IllegalArgumentException(FAILED_TO_DESERIALIZE_MSGPACK_PAYLOAD, e);
      }
    };
  }

  @Override
  public boolean canSerialize(String topic, Serde.Target type) {
    return true;
  }

  @Override
  public Serde.Serializer serializer(String topic, Serde.Target type) {
    return inputString -> {
      try {
        JsonNode node = JSON_MAPPER.readTree(inputString);
        if (node.isMissingNode()) {
          throw new IllegalArgumentException(FAILED_TO_SERIALIZE_JSON_PAYLOAD);
        }
        return MSGPACK_MAPPER.writeValueAsBytes(node);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException(FAILED_TO_SERIALIZE_JSON_PAYLOAD, e);
      }
    };
  }
}
