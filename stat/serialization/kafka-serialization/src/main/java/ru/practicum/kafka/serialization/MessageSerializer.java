package ru.practicum.kafka.serialization;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MessageSerializer implements Serializer<SpecificRecordBase> {

    private BinaryEncoder encoder;

    @Override
    public byte[] serialize(String topic, SpecificRecordBase record) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            encoder = EncoderFactory.get().binaryEncoder(outputStream, encoder);
            SpecificDatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(record.getSchema());
            writer.write(record, encoder);
            encoder.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Serialization error for topic [" + topic + "]", e);
        }
    }
}
