package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the fusion of sensor data for simultaneous localization and mapping (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam exists.
 */
public class FusionSlam {
    private static class FusionSlamHolder{
    // Singleton instance holder
    private static FusionSlam instance = new FusionSlam();
    }

    // Fields
    private final List<LandMark> landmarks; // Represents the map of the environment
    private final List<Pose> poses;        // Represents previous poses needed for calculations

    
    private FusionSlam() {
        this.landmarks = new ArrayList<>();
        this.poses = new ArrayList<>();
    }

   
    // Provides access to the single instance of FusionSlam.
  
    public static FusionSlam getInstance() {
        return FusionSlamHolder.instance;
    }

   // Retrieves the list of landmarks in the environment map.

   public List<LandMark> getLandmarks() {
       return landmarks;
   }

   // Retrieves the list of previous poses needed for SLAM calculations.
    
   public List<Pose> getPoses() {
       return poses;
   }

   public Pose getPoseAt(int time) {
    for (Pose pose : poses) {
        if (pose.getTime() == time) {
            return pose;
        }
    }
    return null; // Return null if no pose matches the time
}

public void updatePose(Pose newPose) {
    poses.add(newPose);
}

public void transformCoordinatesToGlobal(TrackedObject trackedObject, Pose pose) {
    // Transform each cloud point in the tracked object to the global coordinate system
    double yawRadians = Math.toRadians(pose.getYaw());
    trackedObject.getCoordinates().forEach(point -> {
        double globalX = pose.getX() + point.getX() * Math.cos(yawRadians) - point.getY() * Math.sin(yawRadians);
        double globalY = pose.getY() + point.getX() * Math.sin(yawRadians) + point.getY() * Math.cos(yawRadians);
        point.setX(globalX);
        point.setY(globalY);
    });
}

public boolean isNewLandmark(TrackedObject trackedObject) {
    return landmarks.stream().noneMatch(landmark -> landmark.getId().equals(trackedObject.getId()));
}

public void addLandmark(TrackedObject trackedObject) {
    LandMark newLandmark = new LandMark(trackedObject.getId(), trackedObject.getDescription(), new ArrayList<>(trackedObject.getCoordinates()));
    landmarks.add(newLandmark);
}

public void updateLandmark(TrackedObject trackedObject) {
    for (LandMark landmark : landmarks) {
        if (landmark.getId().equals(trackedObject.getId())) {
            List<CloudPoint> newCoordinates = trackedObject.getCoordinates();
            List<CloudPoint> existingCoordinates = landmark.getCoordinates();

            // Update existing coordinates and add new ones
            for (int i = 0; i < Math.min(existingCoordinates.size(), newCoordinates.size()); i++) {
                CloudPoint existing = existingCoordinates.get(i);
                CloudPoint newPoint = newCoordinates.get(i);
                existing.setX((existing.getX() + newPoint.getX()) / 2);
                existing.setY((existing.getY() + newPoint.getY()) / 2);
            }

            // Add coordinates that exist in the new list but not in the existing list
            for (int i = existingCoordinates.size(); i < newCoordinates.size(); i++) {
                existingCoordinates.add(newCoordinates.get(i));
            }
            return;
        }
    }
}

}