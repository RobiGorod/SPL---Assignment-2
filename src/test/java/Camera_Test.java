import static org.junit.jupiter.api.Assertions.*;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collections;

public class Camera_Test {
    private Camera Testcamera;
    private StatisticalFolder TeststatisticalFolder;
    private CameraService TestcameraService;

 //(!!!) need to change cameraService.processDetectedObjects to public & create setDetectedObjectsList in camera service

    @BeforeEach
    public void setUp() {
        // Initialize StatisticalFolder with zeroed values
        TeststatisticalFolder = new StatisticalFolder(0, 0, 0, 0);
        
        // Initialize Camera with an empty list of detected objects
        Testcamera = new Camera(1, 2, STATUS.DOWN, Collections.emptyList());
        
        // Initialize CameraService with the above objects
        TestcameraService = new CameraService(Testcamera, TeststatisticalFolder);
    }

    @Test
    public void testProcessDetectedObjects_NoObjects() {
        // Set detected objects list <--- empty list
        Testcamera = new Camera (1, 2, STATUS.UP, Collections.emptyList());

        // Call processDetectedObjects with a valid tick
        TestcameraService.processDetectedObjects(3);

        // Verify no objects were detected and no statistics were updated
        assertEquals(0, TeststatisticalFolder.getNumDetectedObjects());
        assertEquals(STATUS.UP, Testcamera.getStatus());
    }

    @Test
    public void testProcessDetectedObjects_ValidObjectsAndTick() {
        // Create a list of detected objects with valid timestamps
        Testcamera.setDetectedObjectsList(List.of(
            new StampedDetectedObjects(1, List.of(new DetectedObject("1", "Object1"))),
            new StampedDetectedObjects(3, List.of(new DetectedObject("2", "Object2")))
        ));

        // Call processDetectedObjects with a valid tick
        TestcameraService.processDetectedObjects(3);

        // Verify that statistics were updated correctly
        assertEquals(2, TeststatisticalFolder.getNumDetectedObjects());
        assertEquals(STATUS.UP, Testcamera.getStatus());
    }

    @Test
    public void testProcessDetectedObjects_InvalidTick() {
        // Create a list of detected objects with timestamps that do not match the tick
        Testcamera.setDetectedObjectsList(List.of(
            new StampedDetectedObjects(1, List.of(new DetectedObject("1", "Object1")))
        ));

        // Call processDetectedObjects with an invalid tick
        TestcameraService.processDetectedObjects(4);

        // Verify that no statistics were updated
        assertEquals(0, TeststatisticalFolder.getNumDetectedObjects());
    }
}
