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

}