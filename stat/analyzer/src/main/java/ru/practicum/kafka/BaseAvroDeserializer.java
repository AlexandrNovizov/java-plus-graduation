package ru.practicum.kafka;

import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;

public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final DecoderFactory decoderFactory;
    private final DatumReader<T> reader;

    public BaseAvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = decoderFactory;
        reader = new SpecificDatumReader<>(schema);
    }

    public BaseAvroDeserializer(Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    public T deserialize(String topic, byte[] data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            Decoder decoder = decoderFactory.binaryDecoder(inputStream, null);
            return reader.read(null, decoder);
        } catch (Exception e) {
            throw new SerializationException("Deserialization error for data from topic [" + topic + "]");
        }
    }
}