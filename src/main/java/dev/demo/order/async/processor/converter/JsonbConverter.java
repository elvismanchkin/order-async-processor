package dev.demo.order.async.processor.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

@Slf4j
public class JsonbConverter {

    @WritingConverter
    public static class StringToJsonConverter implements Converter<String, Json> {
        @Override
        public Json convert(String source) {
            return source == null ? null : Json.of(source);
        }
    }

    @ReadingConverter
    public static class JsonToStringConverter implements Converter<Json, String> {
        @Override
        public String convert(Json source) {
            return source == null ? null : source.asString();
        }
    }
}