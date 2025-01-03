package bgu.spl.mics.application.services;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {

    private final Camera camera;
    private final StatisticalFolder statisticalFolder;
    private final CountDownLatch initializationLatch;

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera, StatisticalFolder statisticalFolder, CountDownLatch initializationLatch, String name) {
        super(name);
        this.camera = camera;
        this.statisticalFolder = statisticalFolder;
        this.initializationLatch = initializationLatch;
    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for sending
     * DetectObjectsEvents.
     */
    @Override
    protected void initialize() {
        try{
            // Subscribe to TickBroadcast
            subscribeBroadcast(TickBroadcast.class, tickBroadcast -> {
                int currentTick = tickBroadcast.getCurrentTick();

                // Process detected objects
                processDetectedObjects(currentTick);
            });

            // Subscribe to TerminatedBroadcast
            subscribeBroadcast(TerminatedBroadcast.class, terminatedBroadcast -> {
                if(terminatedBroadcast.getSender() == "Time Service"){
                    camera.setStatus(STATUS.DOWN);
                    terminate(); // Terminate the service
                }
            });

            // Subscribe to CrashedBroadcast
            subscribeBroadcast(CrashedBroadcast.class, crashedBroadcast -> {
                camera.setStatus(STATUS.ERROR);

                // Capture last frames (last detected objects)
                List<StampedDetectedObjects> lastDetectedObjects = camera.getDetectedObjectsList();
                CrashedBroadcast.updateLastCameraFrames(camera.getId(), lastDetectedObjects);

                terminate(); // Terminate the service due to the crash
            });

        } finally {
            initializationLatch.countDown(); // Signal that initialization is complete
        }

    }

    // Processes detected objects at the given tick and sends DetectObjectsEvents

    //Robi changed from private to public to use in Camera Test
    public void processDetectedObjects(int currentTick) {

        // Set the Camera status to UP during processing
        camera.setStatus(STATUS.UP);

        // Get the list of stamped detected objects
        List<StampedDetectedObjects> detectedObjectsList = camera.getDetectedObjectsList();

        for (StampedDetectedObjects stampedObjects : detectedObjectsList) {
            // Check if the camera is in error state
            for(DetectedObject object: stampedObjects.getDetectedObjects()){
                if (object.getId() == "ERROR") {
                    sendBroadcast(new CrashedBroadcast(
                        object.getDescription(),
                        "Cameras"
                ));
                sendBroadcast(new TerminatedBroadcast("Camera"));
                terminate();
                return;
                }
            }
                
            if (isDetectionTimeValid(stampedObjects, currentTick)) {
                // Create and send DetectObjectsEvent
                sendEvent(new DetectObjectsEvent(stampedObjects, currentTick));

                // Update the statistics
                statisticalFolder.incrementDetectedObjects(stampedObjects.getDetectedObjects().size());
            }
        }
    }


    // Checks if a detection is valid for the current tick based on the camera frequency
    private boolean isDetectionTimeValid(StampedDetectedObjects stampedObjects, int currentTick) {
        return ((stampedObjects.getTime() <= currentTick) & (camera.getFrequency()!=0) &&
               ((currentTick - stampedObjects.getTime()) % camera.getFrequency() == 0));
    }


}
