package airport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class AirTrafficController {
    private final Semaphore runway = new Semaphore(1, true);
    private final PriorityBlockingQueue<PlaneWrapper> waitingQueue = new PriorityBlockingQueue<>(10,
            (a, b) -> {
                // First priority: emergency status
                boolean aEmerg = a.emergencySequence >= 0;
                boolean bEmerg = b.emergencySequence >= 0;

                if (aEmerg != bEmerg) {
                    return Boolean.compare(!aEmerg, !bEmerg); // Emergency first
                }

                // Both emergency: use emergency declaration order
                if (aEmerg && bEmerg) {
                    return Integer.compare(a.emergencySequence, b.emergencySequence);
                }

                // Both normal: use arrival order (FIFO)
                return Integer.compare(a.arrivalSequence, b.arrivalSequence);
            });
    private final Object lock = new Object();
    private final AtomicInteger arrivalCounter = new AtomicInteger(0);
    private final AtomicInteger emergencyCounter = new AtomicInteger(0);

    // Wrapper class to track both arrival and emergency order
    private static class PlaneWrapper {
        final Plane plane;
        final int arrivalSequence;
        int emergencySequence = -1; // -1 means not emergency

        PlaneWrapper(Plane plane, int arrivalSequence) {
            this.plane = plane;
            this.arrivalSequence = arrivalSequence;
        }

        void declareEmergency(int emergSeq) {
            this.emergencySequence = emergSeq;
        }
    }

    public void requestToLand(Plane plane, GateManager gateManager) {
        PlaneWrapper wrapper;
        synchronized (lock) {
            wrapper = new PlaneWrapper(plane, arrivalCounter.getAndIncrement());
            AirportLogger.log("ATC", plane.getName() + " requesting to land (emergency=" + plane.isEmergency() + ")");
            waitingQueue.add(wrapper);
            AirportLogger.log("ATC", "Waiting to land: " + queueSummary());
        }

        try {
            while (true) {
                synchronized (lock) {
                    PlaneWrapper next = waitingQueue.peek();
                    boolean gateAvailable = gateManager.hasFreeGate();

                    if (next != null && next.plane == plane && gateAvailable && runway.tryAcquire()) {
                        waitingQueue.poll();
                        AirportLogger.log("ATC", plane.getName() + " granted runway access.");
                        AirportLogger.log("ATC", "Waiting to land: " + queueSummary());
                        return;
                    }
                }

                // Periodically recheck every 300ms
                Thread.sleep(300);
            }
        } catch (InterruptedException ignored) {}
    }

    public void requestToTakeoff(Plane plane) {
        AirportLogger.log("ATC", plane.getName() + " requesting runway for takeoff");
        try {
            runway.acquire(); // Wait for runway to be free
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void releaseRunway(Plane plane) {
        AirportLogger.log("ATC", plane.getName() + " cleared from runway.");
        runway.release();
        synchronized (lock) { lock.notifyAll(); }
    }

    public void notifyEmergency(Plane plane, GateManager gateManager) {
        AirportLogger.log("ATC ALERT", plane.getName() + " declared emergency!");

        synchronized (lock) {
            // Log current airport state (BEFORE modifying wrapper)
            AirportLogger.log("ATC STATUS", "Runway available: " + runway.availablePermits());
            AirportLogger.log("ATC STATUS", gateManager.getGateStatusSummary());
            AirportLogger.log("ATC STATUS", "Landing queue BEFORE: " + queueSummary());

            // Find the wrapper for this plane
            PlaneWrapper targetWrapper = null;
            for (PlaneWrapper w : waitingQueue) {
                if (w.plane == plane) {
                    targetWrapper = w;
                    break;
                }
            }

            if (targetWrapper != null && targetWrapper.emergencySequence < 0) {
                // Remove from queue
                waitingQueue.remove(targetWrapper);

                // Mark as emergency with new sequence
                targetWrapper.declareEmergency(emergencyCounter.getAndIncrement());

                // Re-add - the comparator will now prioritize it
                waitingQueue.add(targetWrapper);
            }

            // Log new queue order
            AirportLogger.log("ATC STATUS", "Landing queue AFTER: " + queueSummary());
            AirportLogger.log("ATC ACTION", plane.getName() + " moved to front (emergency priority).");

            lock.notifyAll(); // Wake up waiting planes to recheck queue
        }
    }

    public String queueSummary() {
        if (waitingQueue.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();

        // Create a sorted list to show current priority order
        List<PlaneWrapper> sortedList = new ArrayList<>(waitingQueue);
        sortedList.sort(waitingQueue.comparator());

        for (PlaneWrapper w : sortedList) {
            sb.append(w.plane.getName());
            if (w.emergencySequence >= 0) sb.append("(E)");
            sb.append(" ");
        }
        return sb.toString().trim();
    }
}