package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService {

    private final int TickTime; // Duration of each tick in milliseconds
    private final int Duration; // Total number of ticks before termination
    private final StatisticalFolder statisticalFolder;

    /**
     * Constructor for TimeService.
     *
     * @param TickTime  The duration of each tick in milliseconds.
     * @param Duration  The total number of ticks before the service terminates.
     */
    public TimeService(int TickTime, int Duration, StatisticalFolder statisticalFolder) {
        super("TimeService");
        this.TickTime = TickTime * 1000;  // Convertion to milliseconds
        this.Duration = Duration;
        this.statisticalFolder = statisticalFolder;
    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified duration.
     */
    @Override
    protected void initialize() {
            try {
                for (int currentTick = 1; currentTick <= Duration; currentTick++) {
                    statisticalFolder.incrementSystemRuntime(1); // Update runtime
                    sendBroadcast(new TickBroadcast(currentTick)); // Send a TickBroadcast

                    Thread.sleep(TickTime); // Wait for the next tick
                }
                sendBroadcast(new TerminatedBroadcast("Time Service")); // Send a TerminatedBroadcast
                terminate(); // Terminate this service
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println(getName() + " was interrupted.");
            }

    }
}
