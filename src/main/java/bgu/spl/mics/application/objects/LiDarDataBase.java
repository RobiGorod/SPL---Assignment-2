package bgu.spl.mics.application.objects;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase {
    private static class LiDarDataBaseHolder{
    // Singleton instance
    private static LiDarDataBase instance = new LiDarDataBase();
    }

    private List<StampedCloudPoints> cloudPoints;

    private LiDarDataBase() {
        this.cloudPoints = new CopyOnWriteArrayList<>();
    }


    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @param filePath The path to the LiDAR data file.
     * @return The singleton instance of LiDarDataBase.
     */
    public static LiDarDataBase getInstance(String filePath) {
        LiDarDataBase instance = LiDarDataBaseHolder.instance;
        instance.loadData(filePath);
        return instance;
    }


    private void loadData(String filePath) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            Type listType = new TypeToken<List<StampedCloudPoints>>() {}.getType();
            List<StampedCloudPoints> loadedCloudPoints = gson.fromJson(reader, listType);
            if (loadedCloudPoints != null) {
                this.cloudPoints = loadedCloudPoints;
            }
        } catch (IOException e) {
            System.err.println("Error loading LiDAR data: " + e.getMessage());
        }
    }


    public List<StampedCloudPoints> getCloudPoints() {
        return new ArrayList<>(cloudPoints); // Return a copy for immutability
    }

    public static LiDarDataBase getInstance() {
        LiDarDataBase instance = LiDarDataBaseHolder.instance;
        return instance;
    };
}
