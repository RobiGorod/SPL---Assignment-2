package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedDetectedObjects;




public class DetectObjectsEvent implements Event<Boolean> {
    private final StampedDetectedObjects detectedObjects; // List of detected objects 
    private final int time; // Time T when the objects were detected

    public DetectObjectsEvent(StampedDetectedObjects detectedObjects, int time) {
        this.detectedObjects = detectedObjects;
        this.time = time;
    }

    public StampedDetectedObjects getDetectedObjects() {
        return detectedObjects;
    }

    public int getTime() {
        return time;
    }
}

