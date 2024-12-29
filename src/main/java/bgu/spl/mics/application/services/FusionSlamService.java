package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.objects.TrackedObject;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 * 
 * This service receives TrackedObjectsEvents from LiDAR workers and PoseEvents from the PoseService,
 * transforming and updating the map with new landmarks.
 */
public class FusionSlamService extends MicroService {

    private final FusionSlam fusionSlam;
    private final StatisticalFolder statisticalFolder;
    private int currentTick = 0;
    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam, StatisticalFolder statisticalFolder) {
        super("FusionSlamService");
        this.fusionSlam = fusionSlam;
        this.statisticalFolder = statisticalFolder;
    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle TrackedObjectsEvents, PoseEvents, and TickBroadcasts,
     * and sets up callbacks for updating the global map.
     */
    @Override
    protected void initialize() {
         // Subscribe to TickBroadcast
        subscribeBroadcast(TickBroadcast.class, tickBroadcast -> {
            currentTick = tickBroadcast.getCurrentTick();
        });

        // Subscribe to TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, terminatedBroadcast -> {
            terminate();
        });

        // Subscribe to CrashedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, crashedBroadcast -> {
            terminate(); // Terminate the service due to a crash
        });

        // Subscribe to TrackedObjectsEvent
        subscribeEvent(TrackedObjectsEvent.class, trackedObjectsEvent -> {
            try {
                for (TrackedObject trackedObject : trackedObjectsEvent.getTrackedObjects()) {
                    // Retrieve the pose at the detection timestamp
                    Pose poseAtDetectionTime = fusionSlam.getPoseAt(trackedObject.getTime());
                    if (poseAtDetectionTime != null) {
                        // Transform the object's coordinates to the global coordinate system
                        fusionSlam.transformCoordinatesToGlobal(trackedObject, poseAtDetectionTime);

                        // Update the map in FusionSLAM
                        if (fusionSlam.isNewLandmark(trackedObject)) {
                            fusionSlam.addLandmark(trackedObject);
                            statisticalFolder.incrementNewLandmarks(); // Track new landmarks
                        } else {
                            fusionSlam.updateLandmark(trackedObject);
                            statisticalFolder.incrementUpdatedLandmarks(); // Track updated landmarks
                        }
                    }
                }
                // Complete the event successfully
                complete(trackedObjectsEvent, true);
            } catch (Exception e) {
                complete(trackedObjectsEvent, false); // Mark the event as failed if an error occurs
            }
        });

        // Subscribe to PoseEvent to update the robot's pose
        subscribeEvent(PoseEvent.class, poseEvent -> {
            try {
                fusionSlam.updatePose(poseEvent.getPose());
                complete(poseEvent, true); // Complete the event successfully
            } catch (Exception e) {
                complete(poseEvent, false); // Mark the event as failed if an error occurs
            }
        });
    }

    }
}
