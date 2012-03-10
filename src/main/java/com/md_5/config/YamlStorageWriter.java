package com.md_5.config;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;

public class YamlStorageWriter {

    private static Pattern NON_WORD_PATTERN = Pattern.compile("\\W");
    private PrintWriter writer;
    private final static Yaml YAML = new Yaml();

    public YamlStorageWriter(PrintWriter writer) {
        this.writer = writer;
    }

    public void save(AnnotatedConfig object) {
        try {
            writeToFile(object, 0, object.getClass());
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void writeToFile(Object object, int depth, Class<?> clazz) throws IllegalAccessException {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (!Modifier.isTransient(field.getModifiers())) {
                Object data = field.get(object);
                if (writeKey(field, depth, data)) {
                    continue;
                }
                if (data instanceof AnnotatedConfig) {
                    writer.println();
                    writeToFile(data, depth + 1, data.getClass());
                } else if (data instanceof Map) {
                    writeMap((Map<Object, Object>) data, depth + 1);
                } else if (data instanceof Collection) {
                    writeCollection((Collection<Object>) data, depth + 1);
                } else {
                    writeScalar(data);
                    writer.println();
                }
            }
        }
    }

    private boolean writeKey(Field field, int depth, Object data) {
        boolean commented = writeConfigComment(field, depth);
        if (data == null && !commented) {
            return true;
        }
        writeIndention(depth);
        if (data == null && commented) {
            writer.print('#');
        }
        String name = field.getName();
        writer.print(name);
        writer.print(": ");
        if (data == null && commented) {
            writer.println();
            return true;
        }
        return false;
    }

    private boolean writeConfigComment(Field field, int depth) {
        boolean commented = field.isAnnotationPresent(ConfigComment.class);
        if (commented) {
            for (String comment : field.getAnnotation(ConfigComment.class).value()) {
                if (!(comment = comment.trim()).isEmpty()) {
                    writeIndention(depth);
                    writer.print("# ");
                    writer.print(comment);
                    writer.println();
                }
            }
        }
        return commented;
    }

    private void writeCollection(Collection<Object> data, int depth) throws IllegalAccessException {
        writer.println();
        if (data.isEmpty()) {
            writer.println();
        }
        for (Object entry : data) {
            if (entry != null) {
                writeIndention(depth);
                writer.print("- ");
                if (entry instanceof AnnotatedConfig) {
                    writer.println();
                    writeToFile(entry, depth + 1, entry.getClass());
                } else {
                    writeScalar(entry);
                }
            }
        }
        writer.println();
    }

    private void writeMap(Map<Object, Object> data, int depth) throws IllegalArgumentException, IllegalAccessException {
        writer.println();
        if (data.isEmpty()) {
            writer.println();
        }
        for (Entry<Object, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                writeIndention(depth);
                writeKey(entry.getKey());
                writer.print(": ");
                if (value instanceof AnnotatedConfig) {
                    writer.println();
                    writeToFile(value, depth + 1, value.getClass());
                } else if (value instanceof Collection) {
                    writeCollection((Collection<Object>) value, depth + 1);
                } else {
                    writeScalar(value);
                    writer.println();
                }
            }
        }
    }

    private void writeIndention(int depth) {
        for (int i = 0; i < depth; i++) {
            writer.print("  ");
        }
    }

    private void writeScalar(Object data) {
        if (data instanceof String || data instanceof Boolean || data instanceof Number) {
            YAML.dumpAll(Collections.singletonList(data).iterator(), writer);
        }
    }

    private void writeKey(Object data) {
        if (data instanceof String || data instanceof Boolean || data instanceof Number) {
            String output = data.toString();
            if (NON_WORD_PATTERN.matcher(output).find()) {
                writer.print('"');
                writer.print(output.replace("\"", "\\\""));
                writer.print('"');
            } else {
                writer.print(output);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
