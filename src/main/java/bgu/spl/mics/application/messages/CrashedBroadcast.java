package bgu.spl.mics.application.messages;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.TrackedObject;

public class CrashedBroadcast implements Broadcast{
    private final String errorDescription;
    private final String faultySensor;
    private final String sender;
    private static final Map<String, StampedDetectedObjects> lastCameraFrames = new ConcurrentHashMap<>();
    private static final Map<String, List<TrackedObject>> lastLiDarFrames = new ConcurrentHashMap<>();

    public CrashedBroadcast(String errorDescription, String faultySensor, String sender) {
        this.errorDescription = errorDescription;
        this.faultySensor = faultySensor;
        this.sender = sender;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public String getFaultySensor() {
        return faultySensor;
    }

    public String getSender(){
        return sender;
    }

    public static Map<String, StampedDetectedObjects> getLastCameraFrames() {
        return lastCameraFrames;
    }

    public static Map<String, List<TrackedObject>> getLastLiDarFrames() {
        return lastLiDarFrames;
    }

    public static void updateLastCameraFrames(String cameraId, StampedDetectedObjects frameData) {
        lastCameraFrames.put(cameraId, frameData);

    }

    public static void updateLastLiDarFrames(String lidarId, List<TrackedObject> frameData) {
        lastLiDarFrames.put(lidarId, frameData);
    }

}
