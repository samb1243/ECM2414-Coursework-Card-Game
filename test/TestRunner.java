import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.PrintWriter;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

//runs the whole test suite with the JUnit Platform launcher and prints a summary. Exits with status 1 if any test fails.

public final class TestRunner {

    private static final Class<?>[] SUITE = {
        CardTest.class,
        CardDeckTest.class,
        PackTest.class,
        PlayerTest.class,
        CardGameTest.class,
    };

    private TestRunner() {
    }

    public static void main(String[] args) {
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
        for (Class<?> testClass : SUITE) {
            builder.selectors(selectClass(testClass));
        }
        LauncherDiscoveryRequest request = builder.build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(request, listener);

        TestExecutionSummary summary = listener.getSummary();
        PrintWriter out = new PrintWriter(System.out, true);
        summary.printFailuresTo(out, 25);
        summary.printTo(out);
        System.exit(summary.getTotalFailureCount() == 0 ? 0 : 1);
    }
}
