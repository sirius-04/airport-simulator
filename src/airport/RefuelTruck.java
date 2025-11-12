package airport;
import java.util.concurrent.Semaphore;

public class RefuelTruck {
    private final Semaphore refuelLock = new Semaphore(1, true);

    public void refuel(Plane plane) {
        try {
            AirportLogger.log("RefuelTruck", plane.getName() + " waiting for refuel truck...");
            refuelLock.acquire();
            AirportLogger.log("RefuelTruck", plane.getName() + " now refueling.");
            Thread.sleep(1500);
            AirportLogger.log("RefuelTruck", plane.getName() + " refueling complete.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            refuelLock.release();
        }
    }
}
