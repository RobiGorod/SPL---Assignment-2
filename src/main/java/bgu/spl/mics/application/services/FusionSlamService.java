package bgu.spl.mics.application.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;

// import bgu.spl.mics.MessageBus;
// import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
// import bgu.spl.mics.application.messages.TickBroadcast;
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
    private final AtomicInteger activeSensors;
    private final CountDownLatch initializationLatch;
    private String errorDescription = null;
    private String faultySensor = null;
    private final Map<String, Object> lastFrames = new ConcurrentHashMap<>();
    private final String configPath;
    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam,  CountDownLatch initializationLatch, int activeSensors, String configPath) {
        super("FusionSlamService");
        this.fusionSlam = fusionSlam;
        this.activeSensors = new AtomicInteger(activeSensors);
        this.initializationLatch = initializationLatch;
        this.configPath = configPath;

    }

    

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle TrackedObjectsEvents, PoseEvents, and TickBroadcasts,
     * and sets up callbacks for updating the global map.
     */
    @Override
    protected void initialize() {
        try{
            // Subscribe to TerminatedBroadcast
            subscribeBroadcast(TerminatedBroadcast.class, terminatedBroadcast -> {
                if(terminatedBroadcast.getSender() == "Time Service"){
                    outputFinalState();
                    terminate();
                }
                else if(terminatedBroadcast.getSender() == "Camera" || terminatedBroadcast.getSender() == "Lidar"){
                    int remainingSensors = activeSensors.decrementAndGet();
                    System.out.println("Current state of active sensors: " + remainingSensors);
                    if (remainingSensors == 0) {
                        FusionSlam.getInstance().terminateFusionSlam();
                        outputFinalState();
                        terminate();
                    }
                    
                }
            });

            // Subscribe to CrashedBroadcast
            subscribeBroadcast(CrashedBroadcast.class, crashedBroadcast -> {
                
                fusionSlam.terminateFusionSlam();
                // if(crashedBroadcast.getSender() == "Camera" || crashedBroadcast.getSender() == "Lidar"){
                //     int remainingSensors = activeSensors.decrementAndGet();
                
                //     if (remainingSensors == 0){  
                    errorDescription = crashedBroadcast.getErrorDescription();
                    faultySensor = crashedBroadcast.getFaultySensor();
                    // Capture last frames from sensors
                    lastFrames.put("cameras", CrashedBroadcast.getLastCameraFrames());
                    lastFrames.put("LiDarWorkers", CrashedBroadcast.getLastLiDarFrames());
                    FusionSlam.getInstance().terminateFusionSlam();
                    outputFinalState();
                    terminate(); // Terminate the service due to a crash
                    }
        //         }
        // }
        );

            // Subscribe to TrackedObjectsEvent
            subscribeEvent(TrackedObjectsEvent.class, trackedObjectsEvent -> {
                try {
                    for (TrackedObject trackedObject : trackedObjectsEvent.getTrackedObjects()) {
                        // Retrieve the pose at the detection timestamp
                        Pose poseAtDetectionTime = fusionSlam.getPoseAt(trackedObject.getTime());
                        if (poseAtDetectionTime == null) {
                            complete(trackedObjectsEvent, null); // Skip if necessary data is missing
                            return;
                        }
                        // Transform the object's coordinates to the global coordinate system
                        fusionSlam.transformCoordinatesToGlobal(trackedObject, poseAtDetectionTime);

                        // Update the map in FusionSLAM
                        if (fusionSlam.isNewLandmark(trackedObject)) {
                            fusionSlam.addLandmark(trackedObject);
                            StatisticalFolder.getInstance().incrementLandmarks(1); // Track new landmarks
                        } else {
                            fusionSlam.updateLandmark(trackedObject);
                        }
                    }
                    // Complete the event successfully
                    complete(trackedObjectsEvent, null);
                } catch (Exception e) {
                    complete(trackedObjectsEvent, null); // Mark the event as failed if an error occurs
                }
            });

            // Subscribe to PoseEvent to update the robot's pose
            subscribeEvent(PoseEvent.class, poseEvent -> {
                try {
                    fusionSlam.updatePose(poseEvent.getPose());
                    complete(poseEvent, null); // Complete the event successfully
                } catch (Exception e) {
                    complete(poseEvent, null); // Mark the event as failed if an error occurs
                }
            });
        } finally {
            initializationLatch.countDown(); // Signal that initialization is complete
        }
    }


    // Outputs the final state of the system to a JSON file.
    private void outputFinalState() {
        System.out.println("Writing final state to JSON...");
        Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();
        
        try {
            File configFile = new File(configPath);
            String configDir = configFile.getParent();
            File outputFile = new File(configDir, "output_file.json");
            
            try (FileWriter writer = new FileWriter(outputFile)) {
                System.out.println("---------------Output file parameters check---------------");
                System.out.println("Statistics: Num Detected-" + StatisticalFolder.getInstance().getNumDetectedObjects() +
                                " Num landmarks- " + StatisticalFolder.getInstance().getNumLandmarks() +
                                " Num tracked: " + StatisticalFolder.getInstance().getNumTrackedObjects());
                System.out.println("run time " + StatisticalFolder.getInstance().getSystemRuntime());
        
                if (errorDescription != null && faultySensor != null) {
                    // Error case
                    System.out.println("Error: " + errorDescription);
                    System.out.println("Faulty Sensor: " + faultySensor);
                    System.out.println("Last Frames: " + lastFrames);
                    System.out.println("Poses: " + fusionSlam.getPoses());
    
                    Map<String, Object> errorOutput = new LinkedHashMap<>();
                    errorOutput.put("error", errorDescription);
                    errorOutput.put("faultySensor", faultySensor);
                    errorOutput.put("lastCamerasFrame", CrashedBroadcast.getLastCameraFrames());
                    errorOutput.put("lastLiDarWorkerTrackersFrame", CrashedBroadcast.getLastLiDarFrames());
                    errorOutput.put("poses", fusionSlam.getPoses());
                    
                    Map<String, Object> statistics = createStatisticsMap();
                    errorOutput.put("statistics", statistics);
                    errorOutput.put("landMarks", fusionSlam.getLandmarks());
    
                    // Write the first part without pretty printing
                    writer.write(gson.toJson(errorOutput));
                } else {
                    // Successful run
                    System.out.println("Landmarks: " + fusionSlam.getLandmarks());
    
                    // Create the first part of the JSON (statistics)
                    Map<String, Object> statistics = createStatisticsMap();
                    String statsJson = gson.toJson(statistics);
                    // Remove the closing brace
                    statsJson = statsJson.substring(0, statsJson.length() - 1);
                    writer.write(statsJson);
                    
                    // Add landmarks section with custom formatting
                    writer.write(",\n\"landMarks\":{\n");
                    
                    // Convert landmarks to map
                    Map<String, Object> landmarksMap = convertLandmarksToMap(fusionSlam.getLandmarks());
                    boolean first = true;
                    for (Map.Entry<String, Object> entry : landmarksMap.entrySet()) {
                        if (!first) {
                            writer.write(",\n");
                        }
                        writer.write("    \"" + entry.getKey() + "\":" + gson.toJson(entry.getValue()));
                        first = false;
                    }
                    
                    writer.write("\n    }\n}");
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing output file: " + e.getMessage());
        }
    }

private Map<String, Object> createStatisticsMap() {
    Map<String, Object> statistics = new LinkedHashMap<>();
    statistics.put("systemRuntime", StatisticalFolder.getInstance().getSystemRuntime());
    statistics.put("numDetectedObjects", StatisticalFolder.getInstance().getNumDetectedObjects());
    statistics.put("numTrackedObjects", StatisticalFolder.getInstance().getNumTrackedObjects());
    statistics.put("numLandmarks", StatisticalFolder.getInstance().getNumLandmarks());
    return statistics;
}

    private Map<String, Object> convertLandmarksToMap(List<LandMark> landmarks) {
        Map<String, Object> landmarksMap = new LinkedHashMap<>();
    for (LandMark landmark : landmarks) {
        Map<String, Object> landmarkDetails = new LinkedHashMap<>();
        landmarkDetails.put("id", landmark.getId());
        landmarkDetails.put("description", landmark.getDescription());
        landmarkDetails.put("coordinates", landmark.getCoordinates());
        landmarksMap.put(landmark.getId(), landmarkDetails);
    }
    return landmarksMap;
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

    public FinalState( List<LandMark> landmarks, String error, String faultySensor, Map<String, Object> lastFrames, List<Pose> poses) {
        this.statistics = StatisticalFolder.getInstance();
        this.landmarks = landmarks;
        this.error = error;
        this.faultySensor = faultySensor;
        this.lastFrames = lastFrames;
        this.poses = poses;
    }
}



