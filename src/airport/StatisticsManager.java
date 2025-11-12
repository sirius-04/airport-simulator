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

    public void printSummary() {
        System.out.println("\n===== Airport Statistics =====");
        if (waitingTimes.isEmpty()) {
            System.out.println("No data recorded.");
            return;
        }

        long max = Collections.max(waitingTimes);
        long min = Collections.min(waitingTimes);
        double avg = waitingTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.println("Planes served: " + planesServed);
        System.out.println("Total passengers boarded: " + totalPassengers);
        System.out.println("Max waiting time: " + max + " ms");
        System.out.println("Min waiting time: " + min + " ms");
        System.out.println("Avg waiting time: " + String.format("%.2f", avg) + " ms");
        System.out.println("==============================");
    }
}
