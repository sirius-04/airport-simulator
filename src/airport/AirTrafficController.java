package airport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.PriorityBlockingQueue;

public class AirTrafficController {
    private final Semaphore runway = new Semaphore(1, true);
    private final PriorityBlockingQueue<Plane> waitingQueue = new PriorityBlockingQueue<>(10, (a, b) -> Boolean.compare(!a.isEmergency(), !b.isEmergency()));
    private final Object lock = new Object();

    public void requestToLand(Plane plane, GateManager gateManager) {
        synchronized (lock) {
            AirportLogger.log("ATC", plane.getName() + " requesting to land (emergency=" + plane.isEmergency() + ")");
            waitingQueue.add(plane);
            AirportLogger.log("ATC", "Waiting to land: " + queueSummary());
        }

        try {
            while (true) {
                synchronized (lock) {
                    Plane next = waitingQueue.peek();
                    boolean gateAvailable = gateManager.hasFreeGate();

                    if (next == plane && gateAvailable && runway.tryAcquire()) {
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


    public void releaseRunway(Plane plane) {
        AirportLogger.log("ATC", plane.getName() + " cleared from runway.");
        runway.release();
        synchronized (lock) { lock.notifyAll(); }
    }

    public void notifyEmergency(Plane plane, GateManager gateManager) {
        AirportLogger.log("ATC ALERT", plane.getName() + " declared emergency!");

        synchronized (lock) {
            // Log current airport state
            AirportLogger.log("ATC STATUS", "Runway available: " + runway.availablePermits());
            AirportLogger.log("ATC STATUS", gateManager.getGateStatusSummary());
            AirportLogger.log("ATC STATUS", "Landing queue BEFORE: " + queueSummary());

            // Reorder queue (emergency planes come first, preserve FCFS among them)
            if (waitingQueue.remove(plane)) {
                List<Plane> emergencies = new ArrayList<>();
                List<Plane> normals = new ArrayList<>();

                for (Plane p : waitingQueue) {
                    if (p.isEmergency()) emergencies.add(p);
                    else normals.add(p);
                }

                // Insert this plane after existing emergencies
                emergencies.add(plane);
                waitingQueue.clear();
                waitingQueue.addAll(emergencies);
                waitingQueue.addAll(normals);
            }

            // Log new queue order
            AirportLogger.log("ATC STATUS", "Landing queue AFTER: " + queueSummary());
            AirportLogger.log("ATC ACTION", plane.getName() + " moved to front (emergency priority).");

            lock.notifyAll(); // Wake up waiting planes to recheck queue
        }
    }

    private void printWaitingQueue() {
        StringBuilder sb = new StringBuilder("Waiting to land: ");
        if (waitingQueue.isEmpty()) sb.append("none");
        else waitingQueue.forEach(p -> sb.append(p.getName())
                .append(p.isEmergency() ? "(E) " : " "));
        AirportLogger.log("ATC", sb.toString());
    }

    public String queueSummary() {
        if (waitingQueue.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        for (Plane p : waitingQueue) {
            sb.append(p.getName());
            if (p.isEmergency()) sb.append("(E)");
            sb.append(" ");
        }
        return sb.toString().trim();
    }

}
