package airport;

public class Gate {
    private final int gateId;
    private boolean occupied = false;

    public Gate(int id) { this.gateId = id; }

    public synchronized boolean isOccupied() { return occupied; }

    public synchronized void occupy(Plane plane) {
        occupied = true;
        AirportLogger.log(plane.getName(), "docked at Gate " + gateId);
    }

    public synchronized void release() {
        occupied = false;
        AirportLogger.log("Gate", gateId + " is now free.");
    }

    public int getGateId() { return gateId; }

    public void setOccupied(boolean b) {
        this.occupied = b;
    }
}
