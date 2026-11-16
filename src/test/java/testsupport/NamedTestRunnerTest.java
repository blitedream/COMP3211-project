package testsupport;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Result;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

/** Reporting checks are test infrastructure, excluded from model case counts/coverage. */
public final class NamedTestRunnerTest {
    @Test
    public void passingCaseReportsItsName() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Result result = NamedTestRunner.run(new PrintStream(bytes), PassingFixture.class);
        assertTrue(result.wasSuccessful());
        assertEquals(1, result.getRunCount());
        assertTrue(bytes.toString().contains("[PASS] " + PassingFixture.class.getName() + ".example"));
        assertFalse(bytes.toString().contains("[FAIL]"));
    }

    @Test
    public void failureIsReportedAndLaterCaseStillRuns() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Result result = NamedTestRunner.run(new PrintStream(bytes), FailureFixture.class);
        String log = bytes.toString();
        assertFalse(result.wasSuccessful());
        assertEquals(1, result.getFailureCount());
        assertEquals(2, result.getRunCount());
        int failure = log.indexOf("[FAIL] " + FailureFixture.class.getName() + ".aFailure");
        int laterPass = log.indexOf("[PASS] " + FailureFixture.class.getName() + ".bLaterCase");
        assertTrue(failure >= 0);
        assertTrue(laterPass > failure);
        assertFalse(log.contains("[PASS] " + FailureFixture.class.getName() + ".aFailure"));
    }

    @Test
    public void skippedCasesAreNeverReportedAsPassed() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Result result = NamedTestRunner.run(new PrintStream(bytes), SkippedFixture.class);
        String log = bytes.toString();
        assertEquals(0, result.getFailureCount());
        assertEquals(1, result.getIgnoreCount());
        assertTrue(log.contains("[SKIP] " + SkippedFixture.class.getName() + ".ignored"));
        assertTrue(log.contains("[SKIP] " + SkippedFixture.class.getName() + ".assumption"));
        assertFalse(log.contains("[PASS]"));
    }

    public static final class PassingFixture {
        @Test public void example() { assertEquals(4, 2 + 2); }
    }

    @FixMethodOrder(MethodSorters.NAME_ASCENDING)
    public static final class FailureFixture {
        @Test public void aFailure() { fail("deliberate reporting fixture"); }
        @Test public void bLaterCase() { assertTrue(true); }
    }

    public static final class SkippedFixture {
        @Ignore("deliberate reporting fixture")
        @Test public void ignored() { fail("ignored case must not run"); }
        @Test public void assumption() { Assume.assumeTrue(false); }
    }
}
