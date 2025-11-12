package airport;

import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.List;

public class GateManager {
    private final Semaphore gateSlots;
    private final List<Gate> gates = new ArrayList<>();

    public GateManager(int gateCount) {
        this.gateSlots = new Semaphore(gateCount, true);
        for (int i = 1; i <= gateCount; i++) gates.add(new Gate(i));
    }

    public Gate requestGate(Plane plane) {
        AirportLogger.log("GateManager", plane.getName() + " requesting gate...");
        try {
            gateSlots.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        synchronized (gates) {
            for (Gate g : gates) {
                if (!g.isOccupied()) {
                    g.setOccupied(true);
                    AirportLogger.log("GateManager",
                            "Gate " + g.getGateId() + " assigned to " + plane.getName());
                    AirportLogger.log("GateManager", getGateStatusSummary());
                    return g;
                }
            }
        }

        // Should never reach here due to semaphore control
        throw new IllegalStateException("No free gates after semaphore acquire");
    }

    public void releaseGate(Gate gate) {
        gate.release();
        AirportLogger.log("GateManager", "Gate " + gate.getGateId() + " released.");
        printGateStatus();
        gateSlots.release();
    }

    private void printGateStatus() {
        synchronized (gates) {
            StringBuilder sb = new StringBuilder("Current Gates: ");
            for (Gate g : gates) {
                sb.append("[Gate ").append(g.getGateId()).append(": ").append(g.isOccupied() ? "OCCUPIED" : "FREE").append("] ");
            }
            AirportLogger.log("GateManager", sb.toString());
        }
    }

    public boolean hasFreeGate() {
        synchronized (gates) {
            for (Gate g : gates) {
                if (!g.isOccupied()) return true;
            }
        }
        return false;
    }

    public String getGateStatusSummary() {
        synchronized (gates) {
            StringBuilder sb = new StringBuilder("Gates: ");
            for (Gate g : gates) {
                sb.append("[")
                        .append(g.getGateId())
                        .append(": ")
                        .append(g.isOccupied() ? "OCC" : "FREE")
                        .append("] ");
            }
            return sb.toString().trim();
        }
    }

}
