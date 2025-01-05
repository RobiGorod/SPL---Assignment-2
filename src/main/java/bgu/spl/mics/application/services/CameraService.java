package bgu.spl.mics.application.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.objects.cameraCount;;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {

    private final Camera camera;
    private final CountDownLatch initializationLatch;
    private int needsToDetect;
    private StampedDetectedObjects lastStampedDetectedObjects;

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera , CountDownLatch initializationLatch, String name) {
        super(name);
        this.camera = camera;
        // this.statisticalFolder = statisticalFolder;
        this.initializationLatch = initializationLatch;
        this.needsToDetect = camera.getDetectedObjectsList().size();
        lastStampedDetectedObjects = null;

    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for sending
     * DetectObjectsEvents.
     */
    @Override
    protected void initialize() {
        try{
            System.out.println("Initializing "+Thread.currentThread().getName()+"...");

            // Subscribe to TickBroadcast
            subscribeBroadcast(TickBroadcast.class, tickBroadcast -> {
                int currentTick = tickBroadcast.getCurrentTick();
                if(needsToDetect == 0){
                    sendBroadcast(new TerminatedBroadcast("Camera"));
                    camera.setStatus(STATUS.DOWN);
                    terminate();
                }
                else{
                // Process detected objects
                processDetectedObjects(currentTick);
                }
            
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
                // List<StampedDetectedObjects> DetectedObjects = camera.getDetectedObjectsList();
                // int timestamp = StatisticalFolder.getInstance().getSystemRuntime()-1;
                // DetectedObjects.stream()
                // .filter(detected -> detected.getTime() == timestamp) 
                // .flatMap(detected -> detected.getDetectedObjects().stream()) 
                // .collect(Collectors.toList());
                CrashedBroadcast.updateLastCameraFrames("Camera " + camera.getId(), lastStampedDetectedObjects);

                terminate(); // Terminate the service due to the crash
            });

        } finally {
            initializationLatch.countDown(); // Signal that initialization is complete
        }

    }

    // Processes detected objects at the given tick and sends DetectObjectsEvents

 /**
 * @pre camera.isCameraActive() - The camera must be active to process detected objects.
 * @pre !camera.getDetectedObjectsList().isEmpty() - There must be detected objects in the list.
 *
 * @post The DetectObjectsEvent is sent for each valid detection.
 * @post StatisticalFolder.getDetectedObjectsCount() > 0 - Statistics must be updated with the count of detected objects.
 *
 * @inv camera.getDetectedObjectsList().forEach(object -> if object.getID().equals("ERROR") then crashedBroadcast sent 
 * @inv camera.isCameraActive() == !camera.getDetectedObjectsList().isEmpty() - The camera status must reflect its activity.
 */
    // changed from private to public to use in Camera Test
    public void processDetectedObjects(int currentTick) {

        // Get the list of stamped detected objects
        List<StampedDetectedObjects> detectedObjectsList = camera.getDetectedObjectsList();
        for (StampedDetectedObjects stampedObjects : detectedObjectsList) {
            if(stampedObjects.getTime()==currentTick){
                for(DetectedObject object: stampedObjects.getDetectedObjects()){
                    if (object.getId().equals("ERROR")) {
                        sendBroadcast(new CrashedBroadcast(object.getDescription(), "Camera" + camera.getId(),"Camera" ));
                        // camera.setStatus(STATUS.ERROR);
                        // terminate();
                        return;
                    }
                }
            }
            int  detectionTime = currentTick - camera.getFrequency();   
            if (stampedObjects.getTime() > detectionTime) {
                break;
            }
            else if(stampedObjects.getTime() == detectionTime && stampedObjects.getDetectedObjects() != null){
                // Create and send DetectObjectsEvent
                System.out.println("CameraService is sending DetectObjectsEvent...");
                sendEvent(new DetectObjectsEvent(stampedObjects, detectionTime));
                lastStampedDetectedObjects = stampedObjects;
                needsToDetect--; 

                // Update the statistics
                StatisticalFolder.getInstance().incrementDetectedObjects(stampedObjects.getDetectedObjects().size());
            }

        }   
    } 
}
