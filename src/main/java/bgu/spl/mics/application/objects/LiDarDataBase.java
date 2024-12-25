package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase {

    // Singleton instance
    private static LiDarDataBase instance;

    private List<StampedCloudPoints> cloudPoints;


    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @param filePath The path to the LiDAR data file.
     * @return The singleton instance of LiDarDataBase.
     */
    //(!!!) needs to figure this parcing
    public static LiDarDataBase getInstance(String filePath) {
        if (instance == null) {
            instance = new LiDarDataBase();
            instance.loadData(filePath);
        }
        return instance;
    }


    private LiDarDataBase() {
        this.cloudPoints = new ArrayList<>();
    }

    public List<StampedCloudPoints> getCloudPoints() {
        return cloudPoints;
    }
}
