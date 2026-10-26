package autograder.autotester;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** instructor question definition and answer key contract */
public abstract class Question {
    private static final int MAX_PARAMETERS = 100;
    private static final int MAX_TEST_CASES = 1_000;
    private String className;
    private String methodName;
    private Class<?> returnType;
    private String description = "";
    private final List<Parameter> parameters = new ArrayList<>();
    private final List<RandomTestCase> testCases = new ArrayList<>();

    protected final void signature(String className, String methodName, Class<?> returnType) {
        if (this.className != null) {
            throw new IllegalStateException("question signature is already defined");
        }
        requireIdentifier(className, "class name");
        requireIdentifier(methodName, "method name");
        if (returnType == null || returnType == void.class || !isSupportedType(returnType)) {
            throw new IllegalArgumentException("unsupported or missing return type");
        }
        this.className = className;
        this.methodName = methodName;
        this.returnType = returnType;
    }

    protected final void describe(String text) {
        description = text == null ? "" : text.strip();
    }

    protected final void param(String name, Class<?> type) {
        if (parameters.size() >= MAX_PARAMETERS) {
            throw new IllegalArgumentException("question exceeds the " + MAX_PARAMETERS + " parameter limit");
        }
        requireIdentifier(name, "parameter name");
        if (type == null || !isSupportedType(type)) {
            throw new IllegalArgumentException("unsupported parameter type for " + name);
        }
        for (Parameter parameter : parameters) {
            if (parameter.name().equals(name)) {
                throw new IllegalArgumentException("duplicate parameter name: " + name);
            }
        }
        parameters.add(new Parameter(name, type));
    }

    protected final void addTestCase(String id, Generator<?>... generators) {
        if (testCases.size() >= MAX_TEST_CASES) {
            throw new IllegalArgumentException("question exceeds the " + MAX_TEST_CASES + " test case limit");
        }
        if (id == null || id.isBlank() || generators == null) {
            throw new IllegalArgumentException("test case id and generators are required");
        }
        if (generators.length != parameters.size()) {
            throw new IllegalArgumentException("test case " + id + " has " + generators.length
                    + " generators for " + parameters.size() + " parameters");
        }
        List<Generator<?>> copy = List.of(generators.clone());
        for (RandomTestCase existing : testCases) {
            if (existing.id().equals(id)) {
                throw new IllegalArgumentException("duplicate test case id: " + id);
            }
        }
        testCases.add(new RandomTestCase(id, copy));
    }

    public abstract Object answerKey(List<Object> arguments) throws Exception;

    public final String getClassName() { return className; }
    public final String getMethodName() { return methodName; }
    public final Class<?> getReturnType() { return returnType; }
    public final String getDescription() { return description; }
    public final List<Parameter> getParameters() { return List.copyOf(parameters); }
    public final List<RandomTestCase> getTestCases() { return List.copyOf(testCases); }

    public final void validate() {
        if (className == null || methodName == null || returnType == null) {
            throw new IllegalArgumentException("question signature is incomplete");
        }
        if (testCases.isEmpty()) {
            throw new IllegalArgumentException("question must define at least one test case");
        }
        if (Modifier.isAbstract(getClass().getModifiers())) {
            throw new IllegalArgumentException("question implementation must be concrete");
        }
    }

    final void validateArguments(List<?> arguments) {
        if (arguments.size() != parameters.size()) {
            throw new IllegalArgumentException("generated argument count does not match the question signature");
        }
        for (int index = 0; index < arguments.size(); index++) {
            Object value = arguments.get(index);
            Class<?> parameterType = parameters.get(index).type();
            if (value == null ? parameterType.isPrimitive() : !boxed(parameterType).isInstance(value)) {
                throw new IllegalArgumentException("generated value does not match parameter "
                        + parameters.get(index).name() + " (" + parameterType.getTypeName() + ")");
            }
        }
    }

    final void validateExpected(Object value) {
        if (!isSupportedValue(value)) {
            throw new IllegalArgumentException("answer key returned an unsupported value type");
        }
        if (value == null ? returnType.isPrimitive() : !boxed(returnType).isInstance(value)) {
            throw new IllegalArgumentException("answer key result does not match declared return type "
                    + returnType.getTypeName());
        }
    }

    static boolean isSupportedType(Class<?> type) {
        return type == int.class || type == Integer.class || type == boolean.class
                || type == Boolean.class || type == long.class || type == Long.class
                || type == double.class || type == Double.class || type == String.class
                || type == int[].class || type == String[].class;
    }

    private Class<?> boxed(Class<?> type) {
        if (type == int.class) return Integer.class;
        if (type == boolean.class) return Boolean.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        return type;
    }

    static boolean isSupportedValue(Object value) {
        if (value == null) {
            return true;
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            Class<?> component = type.getComponentType();
            return (component.isPrimitive() && component != char.class && component != float.class
                    && component != short.class && component != byte.class)
                    || component == String.class;
        }
        return value instanceof Serializable && isSupportedType(type);
    }

    static void requireIdentifier(String value, String label) {
        if (value == null || !value.matches("[A-Za-z_$][A-Za-z0-9_$]*")
                || JAVA_KEYWORDS.contains(value)) {
            throw new IllegalArgumentException("invalid Java " + label + ": " + value);
        }
    }

    private static final java.util.Set<String> JAVA_KEYWORDS = java.util.Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null", "var", "yield", "record", "sealed",
            "permits", "non-sealed");

    public record Parameter(String name, Class<?> type) { }

    public record RandomTestCase(String id, List<Generator<?>> generators) {
        public RandomTestCase {
            generators = List.copyOf(generators);
        }
    }
}
