package testsupport;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/** JUnit entry point implementing the per-case reporting contract in NFR-04. */
public final class NamedTestRunner {
    private NamedTestRunner() { }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: testsupport.NamedTestRunner TEST_CLASS [TEST_CLASS ...]");
            System.exit(2);
            return;
        }
        Class<?>[] suites = new Class<?>[args.length];
        try {
            for (int i = 0; i < args.length; i++) {
                suites[i] = Class.forName(args[i]);
            }
        } catch (ClassNotFoundException ex) {
            System.err.println("Unknown test class: " + ex.getMessage());
            System.exit(2);
            return;
        }
        Result result = run(System.out, suites);
        System.exit(result.wasSuccessful() ? 0 : 1);
    }

    /** Execute all supplied suites, including cases following an assertion failure. */
    public static Result run(final PrintStream output, Class<?>... suites) {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener() {
            private final Set<Description> failed = new HashSet<Description>();
            private final Set<Description> skipped = new HashSet<Description>();

            private String name(Description description) {
                return description.getClassName() + "." + description.getMethodName();
            }

            @Override
            public void testFailure(Failure failure) {
                failed.add(failure.getDescription());
                output.println("[FAIL] " + name(failure.getDescription()));
                output.print(failure.getTrace());
            }

            @Override
            public void testAssumptionFailure(Failure failure) {
                skipped.add(failure.getDescription());
                output.println("[SKIP] " + name(failure.getDescription())
                        + " (assumption: " + failure.getMessage() + ")");
            }

            @Override
            public void testIgnored(Description description) {
                skipped.add(description);
                output.println("[SKIP] " + name(description) + " (ignored)");
            }

            @Override
            public void testFinished(Description description) {
                if (!failed.contains(description) && !skipped.contains(description)) {
                    output.println("[PASS] " + name(description));
                }
            }
        });
        Result result = junit.run(suites);
        output.println("JUnit result: " + result.getRunCount() + " executed, "
                + result.getFailureCount() + " failed, " + result.getIgnoreCount()
                + " ignored.");
        return result;
    }
}
