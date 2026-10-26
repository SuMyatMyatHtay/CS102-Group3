package autograder.autotester;

import java.lang.reflect.Array;
import java.util.Arrays;

final class Literals {
    private Literals() { }

    static String source(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Long number) return number + "L";
        if (value instanceof Double number) {
            if (number.isNaN()) return "Double.NaN";
            if (number == Double.POSITIVE_INFINITY) return "Double.POSITIVE_INFINITY";
            if (number == Double.NEGATIVE_INFINITY) return "Double.NEGATIVE_INFINITY";
            return number.toString();
        }
        if (value instanceof Integer || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof int[] values) {
            return "new int[] {" + join(values) + "}";
        }
        if (value instanceof String[] values) {
            return "new String[] {" + Arrays.stream(values).map(Literals::quote)
                    .collect(java.util.stream.Collectors.joining(", ")) + "}";
        }
        throw new IllegalArgumentException("value type cannot be written as a Java literal: "
                + value.getClass().getTypeName());
    }

    static String show(Object value) {
        if (value == null) return "null";
        if (value instanceof int[] values) return Arrays.toString(values);
        if (value instanceof String[] values) return Arrays.toString(values);
        if (value instanceof String text) return quote(text);
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            StringBuilder display = new StringBuilder("[");
            for (int index = 0; index < length; index++) {
                if (index > 0) display.append(", ");
                display.append(Array.get(value, index));
            }
            return display.append(']').toString();
        }
        return value.toString();
    }

    static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                default -> {
                    if (Character.isISOControl(character)) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static String join(int[] values) {
        return Arrays.stream(values).mapToObj(Integer::toString)
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
