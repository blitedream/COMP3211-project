package model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

/** Standard-library test runner: named independent cases, all failures reported. */
final class TestSupport {
    private TestSupport() { }

    static void runSuite(Class<?> suite) {
        Method[] methods = suite.getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));
        int passed = 0;
        int failed = 0;
        for (Method method : methods) {
            if (!method.getName().startsWith("test") || method.getParameterCount() != 0) continue;
            try {
                method.setAccessible(true);
                method.invoke(null);
                System.out.println("PASS " + suite.getSimpleName() + "." + method.getName());
                passed++;
            } catch (ReflectiveOperationException failure) {
                Throwable cause = failure instanceof InvocationTargetException
                        ? failure.getCause() : failure;
                System.err.println("FAIL " + suite.getSimpleName() + "." + method.getName());
                cause.printStackTrace();
                failed++;
            }
        }
        System.out.println(suite.getSimpleName() + ": " + passed + " passed, " + failed + " failed");
        if (passed + failed == 0 || failed != 0) throw new AssertionError("Suite failed: " + suite.getName());
    }
}
