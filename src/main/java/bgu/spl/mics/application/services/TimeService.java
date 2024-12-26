package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;

/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService {

    private final int TickTime; // Duration of each tick in milliseconds
    private final int Duration; // Total number of ticks before termination

    /**
     * Constructor for TimeService.
     *
     * @param TickTime  The duration of each tick in milliseconds.
     * @param Duration  The total number of ticks before the service terminates.
     */
    public TimeService(int TickTime, int Duration) {
        super("TimeService");
        this.TickTime = TickTime;
        this.Duration = Duration;
    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified duration.
     */
    @Override
    protected void initialize() {
         new Thread(() -> {
            try {
                for (int currentTick = 1; currentTick <= Duration; currentTick++) {
                    sendBroadcast(new TickBroadcast(currentTick)); // Send a TickBroadcast
                    Thread.sleep(TickTime * 1000); // Wait for the next tick
                }
                sendBroadcast(new TerminatedBroadcast()); // Send a TerminatedBroadcast
                terminate(); // Terminate this service
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println(getName() + " was interrupted.");
            }
        }).start();
    }
}
