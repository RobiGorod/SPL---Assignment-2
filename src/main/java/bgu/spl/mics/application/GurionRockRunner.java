package bgu.spl.mics.application;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import bgu.spl.mics.MessageBus;
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.LiDarDataBase;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.services.CameraService;
import bgu.spl.mics.application.services.FusionSlamService;
import bgu.spl.mics.application.services.LiDarService;
import bgu.spl.mics.application.services.PoseService;
import bgu.spl.mics.application.services.TimeService;

/**
 * The main entry point for the GurionRock Pro Max Ultra Over 9000 simulation.
 * <p>
 * This class initializes the system and starts the simulation by setting up
 * services, objects, and configurations.
 * </p>
 */
public class GurionRockRunner {

    /**
     * The main method of the simulation.
     * This method sets up the necessary components, parses configuration files,
     * initializes services, and starts the simulation.
     *
     * @param args Command-line arguments. The first argument is expected to be the path to the configuration file.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java GurionRockRunner <path-to-config-file>");
            return;
        }
        // Initialize MessageBus 
        MessageBus messageBus = MessageBusImpl.getInstance();


         // Initialize Statistical Folder
        StatisticalFolder statisticalFolder = new StatisticalFolder(0,0,0,0);
        
        String configPath = "C:\\Sapir\\לימודים\\SPL\\assignment 3\\example input\\configuration_file.json";
        try {
            // Parse configuration file
            JsonObject config = JsonParser.parseReader(new FileReader(configPath)).getAsJsonObject();
            int duration = config.get("Duration").getAsInt();
            int tickTime = config.get("TickTime").getAsInt();

            // Parse Cameras + services

            List<Camera> cameras = new ArrayList<>();
            List<MicroService> services = new ArrayList<>();
            String cameraDataPath = config.getAsJsonObject("Cameras").get("camera_datas_path").getAsString();
            config.getAsJsonArray("Cameras").forEach(cameraConfig -> {
                JsonObject cameraJson = cameraConfig.getAsJsonObject();
                Camera camera = new Camera(
                        cameraJson.get("id").getAsInt(),
                        cameraJson.get("frequency").getAsInt(),
                        STATUS.DOWN,
                        fromCameraJsonToDetectedObjects(cameraDataPath,cameraJson.get("camera_key").getAsString())
                      
                );
                
                cameras.add(camera);
                services.add(new CameraService(camera, statisticalFolder, cameraJson.get("camera_key").getAsString()));
            });

            LiDarDataBase liDarDataBase = LiDarDataBase.getInstance("lidar_data.json");

            // Parse LiDAR Workers
            List<LiDarWorkerTracker> lidarWorkers = new ArrayList<>();
            config.getAsJsonArray("LidarWorkers").forEach(lidarConfig -> {
                JsonObject lidarJson = lidarConfig.getAsJsonObject();
                LiDarWorkerTracker worker = new LiDarWorkerTracker(
                        lidarJson.get("id").getAsInt(),
                        lidarJson.get("frequency").getAsInt(),
                        STATUS.DOWN,
                        new ArrayList<>()
                );
                lidarWorkers.add(worker);
                services.add(new LiDarService(worker, statisticalFolder, liDarDataBase));
            });


            // Initialize Fusion Slam (Singleton)
            FusionSlam fusionSlam = FusionSlam.getInstance();
            services.add(new FusionSlamService(fusionSlam, statisticalFolder, cameras.size() + lidarWorkers.size()));

            // Initialize Pose
            String poseDataPath = config.get("poseJsonFile").getAsString();
            services.add(new PoseService(new GPSIMU(0, STATUS.UP, fromPoseJsonToPosesList(poseDataPath)))); // Add PoseService

            //Initialize time service
            services.add(new TimeService(tickTime, duration, statisticalFolder)); // Add TimeService

            // Start the Services
            services.forEach(service -> {
                Thread serviceThread = new Thread(service);
                serviceThread.start();
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to initialize simulation. Error: " + e.getMessage());
        }
    
    }
    public static List<StampedDetectedObjects> fromCameraJsonToDetectedObjects(String filePath, String cameraKey) {

        List<StampedDetectedObjects> stampedDetectedObjectsList = new ArrayList<>();
    
        try (FileReader reader = new FileReader(filePath)) {
            //read Json
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
    
            // check if camera key exists extract the detected objects array
            if (jsonObject.has(cameraKey)) {
                JsonArray cameraArray = jsonObject.getAsJsonArray(cameraKey);
    
                // extract for each camera the stamped detected objects
                for (JsonElement cameraEntry : cameraArray) {
                    JsonObject detectedEntry = cameraEntry.getAsJsonObject();
    
                    int time = detectedEntry.get("time").getAsInt();
                    JsonArray detectedObjectsJson = detectedEntry.getAsJsonArray("detectedObjects");
    
                    // creact detected objects list per each time
                    List<DetectedObject> detectedObjects = new ArrayList<>();
                    for (JsonElement objectElement : detectedObjectsJson) {
                        JsonObject objectJson = objectElement.getAsJsonObject();
                        String id = objectJson.get("id").getAsString();
                        String description = objectJson.get("description").getAsString();
                        detectedObjects.add(new DetectedObject(id, description));
                    }
    
                    // add to stamped detected list
                    stampedDetectedObjectsList.add(new StampedDetectedObjects(time, detectedObjects));
                }
            } else {
                System.err.println("Camera key '" + cameraKey + "' not found in the JSON file.");
            }
        } catch (Exception e) {
            System.err.println("Error reading or parsing the JSON file: " + e.getMessage());
        }
    
        return stampedDetectedObjectsList;
    }


    public static List<Pose> fromPoseJsonToPosesList(String filePath) {
        List<Pose> poses = new ArrayList<>();

        try {
            // Parse the JSON file
            JsonArray jsonArray = JsonParser.parseReader(new FileReader(filePath)).getAsJsonArray();
    
            // Iterate over each JSON object in the array
            for (JsonElement element : jsonArray) {
                JsonObject poseJson = element.getAsJsonObject();
    
                // Extract values
                int time = poseJson.get("time").getAsInt();
                float x = poseJson.get("x").getAsFloat();
                float y = poseJson.get("y").getAsFloat();
                float yaw = poseJson.get("yaw").getAsFloat();
                
    
                // Create Pose object and add it to the list
                poses.add(new Pose(x, y, yaw, time));
            }
        } catch (Exception e) {
            System.err.println("Error parsing pose JSON file: " + e.getMessage());
        }
    
        return poses;
    }
}
