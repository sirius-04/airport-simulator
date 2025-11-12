package airport;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class Plane implements Runnable {
    private final int id;
    private final int passengerCount;
    private final AirTrafficController atc;
    private final GateManager gateManager;
    private final RefuelTruck refuelTruck;
    private final StatisticsManager stats;
    private final Random rand = new Random();

    private int fuelLevel;
    private boolean emergency = false;
    private long arrivalTime;

    public Plane(int id, AirTrafficController atc, GateManager gateManager,
                 RefuelTruck refuelTruck, StatisticsManager stats) {
        this.id = id;
        this.passengerCount = 20 + rand.nextInt(31); // between 20–50 passengers
        this.atc = atc;
        this.gateManager = gateManager;
        this.refuelTruck = refuelTruck;
        this.stats = stats;
        this.fuelLevel = 30 + rand.nextInt(71); // 30–100
    }

    public String getName() { return "Plane-" + id; }
    public boolean isEmergency() { return emergency; }

    @Override
    public void run() {
        arrivalTime = System.currentTimeMillis();
        System.out.println(getName() + " arrived with fuel=" + fuelLevel);

        Thread fuelMonitor = new Thread(this::monitorFuel);
        fuelMonitor.start();

        // Request permission to land (ATC checks gate availability)
        atc.requestToLand(this, gateManager);
        fuelMonitor.interrupt();

        long waitTime = System.currentTimeMillis() - arrivalTime;
        stats.recordWaitingTime(waitTime);

        // Land
        simulate("Landing", 1000);

        // Request gate (guaranteed free now)
        Gate gate = gateManager.requestGate(this);

        // Free runway for next plane
        atc.releaseRunway(this);

        AirportLogger.log(getName(), "Docked at Gate " + gate.getGateId());
        simulate("Taxi to Gate " + gate.getGateId(), 100);

        // MODIFIED: Handle passengers concurrently across planes
        handlePassengersConcurrently();

        refuelTruck.refuel(this);
        simulate("Preparing for takeoff", 1000);

        gateManager.releaseGate(gate);

        // Request runway for takeoff
        atc.requestToLand(this, gateManager);
        simulate("Takeoff", 1000);
        atc.releaseRunway(this);

        gateManager.releaseGate(gate);
        stats.recordPlane(this);
        stats.recordPassengers(passengerCount);
    }


    // ======================================
    // FUEL MONITOR
    // ======================================
    private void monitorFuel() {
        try {
            while (!Thread.currentThread().isInterrupted() && !emergency) {
                Thread.sleep(1000);
                fuelLevel -= 8 + rand.nextInt(15);

                if (fuelLevel <= 20 && !emergency) {
                    emergency = true;
                    System.out.println("⚠️ " + getName() + " LOW FUEL! Declaring emergency.");
                    atc.notifyEmergency(this, gateManager); // immediate alert to ATC
                } else {
                    System.out.println(getName() + " fuel remaining: " + fuelLevel);
                }
            }
        } catch (InterruptedException ignored) {}
    }

    // ======================================
    // CONCURRENT GATE ACTIVITIES (MODIFIED)
    // ======================================
    private void simulate(String action, int time) {
        AirportLogger.log(getName(), action);
        try { Thread.sleep(time + rand.nextInt(500)); } catch (InterruptedException ignored) {}
    }

    private void handlePassengersConcurrently() {
        // CountDownLatch to track when all 3 operations are done
        CountDownLatch latch = new CountDownLatch(3);

        // Start all three operations as independent threads
        Thread disembark = new Thread(() -> {
            try {
                // Add small random delays to create interleaving
                Thread.sleep(rand.nextInt(200));
                simulate("Passengers disembarking", 2000);
            } catch (InterruptedException ignored) {
            } finally {
                latch.countDown();
            }
        }, getName() + "-Disembark");

        Thread clean = new Thread(() -> {
            try {
                Thread.sleep(rand.nextInt(200));
                simulate("Cleaning aircraft", 2500);
            } catch (InterruptedException ignored) {
            } finally {
                latch.countDown();
            }
        }, getName() + "-Clean");

        Thread embark = new Thread(() -> {
            try {
                Thread.sleep(rand.nextInt(200));
                simulate("Passengers embarking", 2000);
            } catch (InterruptedException ignored) {
            } finally {
                latch.countDown();
            }
        }, getName() + "-Embark");

        // Start all threads immediately (this allows interleaving with other planes)
        disembark.start();
        clean.start();
        embark.start();

        // Wait for THIS plane's operations to complete before moving to refueling
        try {
            latch.await();
        } catch (InterruptedException ignored) {}

        AirportLogger.log(getName(), "All operations completed and waiting for refuel.");
    }
}