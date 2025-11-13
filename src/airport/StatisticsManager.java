package airport;
import java.util.*;

public class StatisticsManager {
    private final List<Long> waitingTimes = new ArrayList<>();
    private int planesServed = 0;
    private int totalPassengers = 0;

    public synchronized void recordWaitingTime(long t) {
        waitingTimes.add(t);
    }

    public synchronized void recordPlane(Plane plane) {
        planesServed++;
    }

    public synchronized void recordPassengers(int count) {
        totalPassengers += count;
    }

    public void printSummary(GateManager gateManager) {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║          AIRPORT SANITY CHECKS & STATISTICS           ║");
        System.out.println("╔════════════════════════════════════════════════════════╗");

        // SANITY CHECK: Verify all gates are empty
        System.out.println("\n--- SANITY CHECKS ---");
        boolean allGatesEmpty = gateManager.allGatesEmpty();

        if (allGatesEmpty) {
            System.out.println("✓ Gate Status Check: PASSED");
            System.out.println("  All gates are empty as expected.");
        } else {
            System.out.println("✗ Gate Status Check: FAILED");
            System.out.println("  WARNING: Some gates are still occupied!");
        }

        System.out.println("\nDetailed Gate Status:");
        System.out.println(gateManager.getGateStatusSummary());

        // STATISTICS
        System.out.println("--- OPERATIONAL STATISTICS ---");

        if (waitingTimes.isEmpty()) {
            System.out.println("No data recorded.");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            return;
        }

        long max = Collections.max(waitingTimes);
        long min = Collections.min(waitingTimes);
        double avg = waitingTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.println("Planes Served: " + planesServed + " / 6 expected");
        System.out.println("Total Passengers Boarded: " + totalPassengers + " passengers");
        System.out.println("\nWaiting Time Analysis:");
        System.out.println("  Maximum: " + formatTime(max));
        System.out.println("  Minimum: " + formatTime(min));
        System.out.println("  Average: " + formatTime((long)avg));

        System.out.println("\n╚════════════════════════════════════════════════════════╝");

        // Final validation
        if (allGatesEmpty && planesServed == 6) {
            System.out.println("✓✓✓ SIMULATION COMPLETED SUCCESSFULLY ✓✓✓");
        } else {
            System.out.println("⚠️  SIMULATION COMPLETED WITH ISSUES ⚠️");
        }
    }

    private String formatTime(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        } else {
            double seconds = millis / 1000.0;
            return String.format("%.2f seconds (%d ms)", seconds, millis);
        }
    }
}