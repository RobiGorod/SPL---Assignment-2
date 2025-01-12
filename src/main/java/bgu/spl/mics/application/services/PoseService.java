package bgu.spl.mics.application.services;

import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.STATUS;

/**
 * PoseService is responsible for maintaining the robot's current pose (position and orientation)
 * and broadcasting PoseEvents at every tick.
 */
public class PoseService extends MicroService {

     private final GPSIMU gpsimu;
     private final CountDownLatch initializationLatch;

    /**
     * Constructor for PoseService.
     *
     * @param gpsimu The GPSIMU object that provides the robot's pose data.
     */
    public PoseService(GPSIMU gpsimu, CountDownLatch initializationLatch) {
        super("PoseService");
        this.gpsimu = gpsimu;
        this.initializationLatch = initializationLatch;
    }

    /**
     * Initializes the PoseService.
     * Subscribes to TickBroadcast and sends PoseEvents at every tick based on the current pose.
     */
    @Override
    protected void initialize() {
        try{
            // Subscribe to TickBroadcast
            subscribeBroadcast(TickBroadcast.class, tickBroadcast -> {
                // Update the current tick in GPSIMU
                gpsimu.setCurrentTick(tickBroadcast.getCurrentTick());

                // Retrieve the current pose from the pose list
                Pose currentPose = gpsimu.getPoseAt(gpsimu.getCurrentTick());
                    
                if(currentPose!=null){
                // Send a PoseEvent with the current pose
                sendEvent(new PoseEvent(currentPose));
                }
            });

            // Subscribe to TerminatedBroadcast
            subscribeBroadcast(TerminatedBroadcast.class, terminatedBroadcast -> {
                if(terminatedBroadcast.getSender() == "Time Service"){
                    gpsimu.setStatus(STATUS.DOWN);
                    terminate();
                }
            });

            // Subscribe to CrashedBroadcast
            subscribeBroadcast(CrashedBroadcast.class, crashedBroadcast -> {
                gpsimu.setStatus(STATUS.ERROR);
                terminate(); // Terminate the service due to a crash
            });

        } finally {
            initializationLatch.countDown(); // Signal that initialization is complete
        }
    }
}
