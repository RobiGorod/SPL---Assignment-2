package bgu.spl.mics.application.services;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
import bgu.spl.mics.application.objects.LandMark;

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
    private final AtomicInteger activeSensors;
    private int currentTick = 0;
    private String errorDescription = null;
    private String faultySensor = null;
    private final Map<String, Object> lastFrames = new HashMap<>();
    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam, StatisticalFolder statisticalFolder, int activeSensors) {
        super("FusionSlamService");
        this.fusionSlam = fusionSlam;
        this.statisticalFolder = statisticalFolder;
        this.activeSensors = new AtomicInteger(activeSensors);
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
            if (activeSensors.decrementAndGet() == 0) {
                outputFinalState();
            terminate();
            }
        });

        // Subscribe to CrashedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, crashedBroadcast -> {
            errorDescription = crashedBroadcast.getErrorDescription();
            faultySensor = crashedBroadcast.getFaultySensor();

            // Capture last frames from sensors
            lastFrames.put("cameras", crashedBroadcast.getLastCameraFrames());
            lastFrames.put("LiDarWorkers", crashedBroadcast.getLastLiDarFrames());
            outputFinalState();
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
                            statisticalFolder.incrementLandmarks(1); // Track new landmarks
                        } else {
                            fusionSlam.updateLandmark(trackedObject);
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

     // Outputs the final state of the system to a JSON file.
    private void outputFinalState() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter("output_file.json")) {
            gson.toJson(new FinalState(statisticalFolder, fusionSlam.getLandmarks(), errorDescription, faultySensor, lastFrames, fusionSlam.getPoses()), writer);
        } catch (IOException e) {
            System.err.println("Error writing output file: " + e.getMessage());
        }
    }

    }

// A helper class representing the final state of the system for JSON output.
class FinalState {
    private final StatisticalFolder statistics;
    private final List<LandMark> landmarks;
    private final String error;
    private final String faultySensor;
    private final Map<String, Object> lastFrames;
    private final List<Pose> poses;

    public FinalState(StatisticalFolder statistics, List<LandMark> landmarks, String error, String faultySensor, Map<String, Object> lastFrames, List<Pose> poses) {
        this.statistics = statistics;
        this.landmarks = landmarks;
        this.error = error;
        this.faultySensor = faultySensor;
        this.lastFrames = lastFrames;
        this.poses = poses;
    }
   }


