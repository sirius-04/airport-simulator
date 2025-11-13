import airport.*;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Asia Pacific Airport Simulation Start ===");

        AirTrafficController atc = new AirTrafficController();
        GateManager gateManager = new GateManager(3);
        RefuelTruck refuelTruck = new RefuelTruck();
        StatisticsManager stats = new StatisticsManager();

        Random rand = new Random();
        Thread[] planes = new Thread[6];

        for (int i = 0; i < planes.length; i++) {
            int delay;
            if (i < 4) {
                delay = 300; // first 4 planes arrive almost simultaneously
            } else {
                delay = 1500 + rand.nextInt(1000); // remaining 2 slower (to show clear queue)
            }
            Plane plane = new Plane(i + 1, atc, gateManager, refuelTruck, stats);
            planes[i] = new Thread(plane, "Plane-" + (i + 1));
            try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
            planes[i].start();
        }

        // wait all planes to finish
        for (Thread t : planes) {
            try { t.join(); } catch (InterruptedException ignored) {}
        }

        System.out.println("\n=== Simulation End ===");
        stats.printSummary(gateManager);
    }
}