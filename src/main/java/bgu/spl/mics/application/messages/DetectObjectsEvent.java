package bgu.spl.mics.application.messages;

import java.util.List;

public class DetectObjectsEvent {
    public class DetectObjectsEvent implements Event<Boolean> {
    private final List<String> detectedObjects; // List of detected object names
    private final int time; // Time T when the objects were detected

    public DetectObjectsEvent(List<String> detectedObjects, int time) {
        this.detectedObjects = detectedObjects;
        this.time = time;
    }

    public List<String> getDetectedObjects() {
        return detectedObjects;
    }

    public int getTime() {
        return time;
    }
}

}
