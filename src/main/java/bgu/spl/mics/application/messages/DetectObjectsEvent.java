package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.DetectedObject;

import java.util.List;


public class DetectObjectsEvent implements Event<Boolean> {
    private final List<DetectedObject> detectedObjects; // List of detected objects 
    private final int time; // Time T when the objects were detected

    public DetectObjectsEvent(List<DetectedObject> detectedObjects, int time) {
        this.detectedObjects = detectedObjects;
        this.time = time;
    }

    public List<DetectedObject> getDetectedObjects() {
        return detectedObjects;
    }

    public int getTime() {
        return time;
    }
}

