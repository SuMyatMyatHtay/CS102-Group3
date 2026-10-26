import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RandomizedStudentRunner {
    private RandomizedStudentRunner() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) throw new IllegalArgumentException("runner arguments are incomplete");
        List<Object[]> inputs = readInputs(Path.of(args[2]));
        Method method;
        Object receiver;
        try {
            method = findMethod(args);
            receiver = createReceiver(method);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            writeOutcomes(Path.of(args[3]), errorOutcomes(inputs.size(), describe(failure)));
            return;
        }
        List<Object[]> outcomes = invokeAll(method, receiver, inputs);
        writeOutcomes(Path.of(args[3]), outcomes);
    }

    private static Method findMethod(String[] args) throws ReflectiveOperationException {
        Class<?>[] parameterTypes = new Class<?>[args.length - 4];
        for (int index = 4; index < args.length; index++) {
            parameterTypes[index - 4] = resolveType(args[index]);
        }
        return Class.forName(args[0]).getMethod(args[1], parameterTypes);
    }

    private static Object createReceiver(Method method) throws ReflectiveOperationException {
        return Modifier.isStatic(method.getModifiers()) ? null : method.getDeclaringClass().getConstructor().newInstance();
    }

    private static List<Object[]> readInputs(Path inputPath) throws Exception {
        List<Object[]> inputs = new ArrayList<>();
        try (ObjectInputStream input = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(inputPath)))) {
            input.setObjectInputFilter(RandomizedStudentRunner::filter);
            Object value = input.readObject();
            if (!(value instanceof List<?> testInputs)) {
                throw new IllegalArgumentException("invalid randomized input payload");
            }
            for (Object testInput : testInputs) {
                if (!(testInput instanceof Object[] arguments)) {
                    throw new IllegalArgumentException("invalid randomized test input");
                }
                inputs.add(arguments);
            }
        }
        return inputs;
    }

    private static List<Object[]> invokeAll(Method method, Object receiver, List<Object[]> inputs) {
        List<Object[]> outcomes = new ArrayList<>();
        for (Object[] arguments : inputs) outcomes.add(invoke(method, receiver, arguments));
        return outcomes;
    }

    private static List<Object[]> errorOutcomes(int count, String reason) {
        List<Object[]> outcomes = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            outcomes.add(new Object[] {Boolean.FALSE, reason});
        }
        return outcomes;
    }

    private static Object[] invoke(Method method, Object receiver, Object[] arguments) {
        try {
            Object result = method.invoke(receiver, arguments);
            if (result != null && !isSupportedValue(result)) {
                throw new IllegalArgumentException("unsupported return value type: "
                        + result.getClass().getTypeName());
            }
            return new Object[] {Boolean.TRUE, result};
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            return new Object[] {Boolean.FALSE, describe(cause)};
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return new Object[] {Boolean.FALSE, describe(exception)};
        }
    }

    private static void writeOutcomes(Path outputPath, List<Object[]> outcomes) throws Exception {
        try (ObjectOutputStream output = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(outputPath)))) {
            output.writeObject(outcomes);
        }
    }

    private static Class<?> resolveType(String name) throws ClassNotFoundException {
        return switch (name) {
            case "int" -> int.class;
            case "boolean" -> boolean.class;
            case "long" -> long.class;
            case "double" -> double.class;
            case "int[]" -> int[].class;
            case "java.lang.String[]" -> String[].class;
            default -> Class.forName(name);
        };
    }

    private static ObjectInputFilter.Status filter(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 20 || info.references() > 100_000 || info.streamBytes() > 64L * 1024 * 1024
                || info.arrayLength() > 100_000) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) return ObjectInputFilter.Status.UNDECIDED;
        if (type == String.class || type == Integer.class || type == Boolean.class || type == Long.class
                || type == Double.class || type == Number.class || type == ArrayList.class
                || type == Object[].class || type == int[].class || type == String[].class) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        return ObjectInputFilter.Status.REJECTED;
    }

    private static boolean isSupportedValue(Object value) {
        Class<?> type = value.getClass();
        return type == String.class || type == Integer.class || type == Boolean.class
                || type == Long.class || type == Double.class || type == int[].class
                || type == String[].class;
    }

    private static String describe(Throwable failure) {
        String message = failure.getMessage();
        String text = failure.getClass().getName() + (message == null ? "" : ": " + message);
        return text.length() > 2_000 ? text.substring(0, 2_000) : text;
    }
}
