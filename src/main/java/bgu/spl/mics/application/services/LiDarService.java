package bgu.spl.mics.application.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.LiDarDataBase;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedCloudPoints;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.objects.TrackedObject;
import bgu.spl.mics.application.objects.cameraCount;
import bgu.spl.mics.application.objects.CloudPoint;
import bgu.spl.mics.application.objects.DetectedObject;
/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarWorkerTracker object to retrieve and process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
public class LiDarService extends MicroService {

    private final LiDarWorkerTracker LiDarWorkerTracker;
    // private final StatisticalFolder statisticalFolder;
    private final LiDarDataBase liDarDataBase;
    private final CountDownLatch initializationLatch;
    private  List<DetectObjectsEvent> eventsInHold = new ArrayList<>();
    private int currentTick = 0;

    /**
     * Constructor for LiDarService.
     *
     * @param LiDarWorkerTracker A LiDAR Tracker worker object that this service will use to process data.
     */
    public LiDarService(LiDarWorkerTracker LiDarWorkerTracker,  LiDarDataBase liDarDataBase, CountDownLatch initializationLatch) {
        super("LiDarWorkerService_" + LiDarWorkerTracker.getId());
        this.LiDarWorkerTracker = LiDarWorkerTracker;
        // this.statisticalFolder = statisticalFolder;
        this.liDarDataBase = LiDarDataBase.getInstance();
        this.initializationLatch = initializationLatch;
    }

    /**
     * Initializes the LiDarService.
     * Registers the service to handle DetectObjectsEvents and TickBroadcasts,
     * and sets up the necessary callbacks for processing data.
     */
    @Override
    protected void initialize() {
        try{
            System.out.println("Initializing "+Thread.currentThread().getName()+"...");

            // Subscribe to TickBroadcast
            subscribeBroadcast(TickBroadcast.class, tickBroadcast -> {
                currentTick = tickBroadcast.getCurrentTick();
                for( StampedCloudPoints CP : liDarDataBase.getCloudPoints()){
                    if(CP.getTime() > currentTick){
                        break;
                    }
                    else if(CP.getTime() == currentTick){
                        if(CP.getId() == "ERROR"){
                            sendBroadcast(new CrashedBroadcast(
                                    "LiDAR sensor disconnected",
                                    "LiDarWorkerTracker"
                                ));
                                LiDarWorkerTracker.setStatus(STATUS.ERROR);
                                terminate();
                                return;
                        }
                    }
                    List<Integer> processedIndexes = new ArrayList(); 
                    if(eventsInHold.size() > 0){
                        for(DetectObjectsEvent e : eventsInHold){
                            if(currentTick >= (e.getTime()) + LiDarWorkerTracker.getFrequency()){
                                processDetectedObjectsEvent(e);
                                processedIndexes.add(eventsInHold.indexOf(e));
                            }
                        }
                        for(int index : processedIndexes){
                            eventsInHold.remove(index);
                        }
                    }
                }
                if(eventsInHold.size() <= 0 && cameraCount.getInstance().getCameraCount() <= 0){
                    sendBroadcast(new TerminatedBroadcast("Lidar"));
                    LiDarWorkerTracker.setStatus(STATUS.DOWN); 
                    terminate();
                }
                
            });

            // Subscribe to TerminatedBroadcast
            subscribeBroadcast(TerminatedBroadcast.class, terminatedBroadcast -> {
                if(terminatedBroadcast.getSender() == "Camera"){
                    cameraCount.getInstance().decrementCameraCount();
                }
                if(terminatedBroadcast.getSender() == "Time Service"){
                    LiDarWorkerTracker.setStatus(STATUS.DOWN); 
                    terminate();
                }
            });

            // Subscribe to CrashedBroadcast
            subscribeBroadcast(CrashedBroadcast.class, crashedBroadcast -> {
                LiDarWorkerTracker.setStatus(STATUS.ERROR); 

                // Capture last frames (last tracked objects)
                List<TrackedObject> lastTrackedObjects = LiDarWorkerTracker.getLastTrackedObjects();
                CrashedBroadcast.updateLastLiDarFrames(LiDarWorkerTracker.getId(), lastTrackedObjects);

                terminate(); // Terminate the service due to the crash
            });

            // Subscribe to DetectObjectsEvent
            subscribeEvent(DetectObjectsEvent.class, detectObjectsEvent -> {
                try {

                    // Check if the event should be processed at this tick
                    if ((currentTick >= (detectObjectsEvent.getTime()) + LiDarWorkerTracker.getFrequency())) {
                        processDetectedObjectsEvent(detectObjectsEvent);
                    }

                    else{
                        eventsInHold.add(detectObjectsEvent);
                    }
                
                } catch (Exception e) {
                    LiDarWorkerTracker.setStatus(STATUS.ERROR);
                    sendBroadcast(new CrashedBroadcast(
                        "LiDAR sensor disconnected",
                        "LiDarWorkerTracker"
                    ));
                    terminate();
                    complete(detectObjectsEvent, false); // Respond with failure to the Camera
                }
            });
        } finally {
            initializationLatch.countDown(); // Signal that initialization is complete
        }
    }
    public void processDetectedObjectsEvent(DetectObjectsEvent e){
        //  Initialize Tracked Objects
        List<TrackedObject> trackedObjects = new ArrayList<>();

        // Match Detected Object with Cloud Points
        for (DetectedObject detectedObject : e.getDetectedObjects().getDetectedObjects()) {
            
            // Retrieve cloud points for the object
            List<StampedCloudPoints> matchingPoints = liDarDataBase.getCloudPoints().stream()
                    .filter(point -> point.getId().equals(detectedObject.getId()))
                    .filter(point -> point.getTime() == e.getTime()) // Ensure the timestamp is valid
                    .collect(Collectors.toList());
         

            // Create a TrackedObject for each matching cloud point
            for (StampedCloudPoints stampedPoint : matchingPoints) {
                // Convert List<List<Double>> to List<CloudPoint>
                List<CloudPoint> cloudPoints = stampedPoint.getCloudPoints().stream()
                    .map(coord -> new CloudPoint(coord.get(0).intValue(), coord.get(1).intValue())) // Assuming CloudPoint(x, y)
                    .collect(Collectors.toList());

                trackedObjects.add(new TrackedObject(
                    detectedObject.getId(),
                    stampedPoint.getTime(),
                    detectedObject.getDescription(),
                    cloudPoints
                ));
            }
        }

        // Update worker's last tracked objects
        LiDarWorkerTracker.getLastTrackedObjects().clear();
        LiDarWorkerTracker.getLastTrackedObjects().addAll(trackedObjects);

        // Send a TrackedObjectsEvent to Fusion-SLAM
        System.out.println("LiDarService is sending TrackedObjectsEvent...");
        sendEvent(new TrackedObjectsEvent(trackedObjects));


        // Update statistical folder
        StatisticalFolder.getInstance().incrementTrackedObjects(trackedObjects.size());
    

        // Respond to Camera with True result
        complete(e, true);
     
    }

    
}
