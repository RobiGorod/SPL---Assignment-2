package bgu.spl.mics.application.messages;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.mics.Broadcast;

public class CrashedBroadcast implements Broadcast{
    private final String errorDescription;
    private final String faultySensor;
    private static final Map<Integer, Object> lastCameraFrames = new ConcurrentHashMap<>();
    private static final Map<Integer, Object> lastLiDarFrames = new ConcurrentHashMap<>();

    public CrashedBroadcast(String errorDescription, String faultySensor) {
        this.errorDescription = errorDescription;
        this.faultySensor = faultySensor;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public String getFaultySensor() {
        return faultySensor;
    }

    public static Map<Integer, Object> getLastCameraFrames() {
        return lastCameraFrames;
    }

    public static Map<Integer, Object> getLastLiDarFrames() {
        return lastLiDarFrames;
    }

    public static void updateLastCameraFrames(int cameraId, Object frameData) {
        lastCameraFrames.put(cameraId, frameData);
    }

    public static void updateLastLiDarFrames(int lidarId, Object frameData) {
        lastLiDarFrames.put(lidarId, frameData);
    }

}
