
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.services.CameraService;

public class CameraTest  {
    private Camera Testcamera;
    private StatisticalFolder TeststatisticalFolder;
    private CameraService TestcameraService;

    @BeforeEach
    public void setUp() {
        // Initialize CountDownLatch with a count of 1 for the service under test
        CountDownLatch initializationLatch = new CountDownLatch(1);
        // Initialize StatisticalFolder with zeroed values
        TeststatisticalFolder = StatisticalFolder.getInstance();
        TeststatisticalFolder.reset();
        // Initialize Camera with an empty list of detected objects
        Testcamera = new Camera(1, 2, STATUS.UP, Collections.emptyList());
        
        // Initialize CameraService with the above objects
        TestcameraService = new CameraService(Testcamera, initializationLatch, "Canera1");
    }

    @Test
    public void testProcessDetectedObjects_NoObjects() {
        // Set detected objects list <--- empty list
        Testcamera = new Camera (1, 2, STATUS.UP,  new ArrayList<>());

        // Call processDetectedObjects with a valid tick
        TestcameraService.processDetectedObjects(3);

        // Verify no objects were detected and no statistics were updated
        assertEquals(0, TeststatisticalFolder.getNumDetectedObjects());
        assertEquals(STATUS.UP, Testcamera.getStatus());
    }

    @Test
    public void testProcessDetectedObjects_ValidObjectsAndTick() {
        // Create a list of detected objects with valid timestamps
        List<StampedDetectedObjects> listToCheck = new ArrayList<>();

        listToCheck.add (new StampedDetectedObjects(1, Arrays.asList(new DetectedObject("1", "Object1"))));
        // listToCheck.add (new StampedDetectedObjects(3, Arrays.asList(new DetectedObject("2", "Object2"))));

        Testcamera.setDetectedObjectsList(listToCheck);

        // Call processDetectedObjects with a valid tick
        TestcameraService.processDetectedObjects(3);

        // Verify that statistics were updated correctly
        assertEquals(1, TeststatisticalFolder.getNumDetectedObjects());
        assertEquals(STATUS.UP, Testcamera.getStatus());
    }

    @Test
    public void testProcessDetectedObjects_InvalidTick() {
        // Create a list of detected objects with timestamps that do not match the tick
        Testcamera.setDetectedObjectsList(Arrays.asList(
            new StampedDetectedObjects(1, Arrays.asList(new DetectedObject("1", "Object1")))
        ));

        // Call processDetectedObjects with an invalid tick
        TestcameraService.processDetectedObjects(4);

        // Verify that no statistics were updated
        assertEquals(0, TeststatisticalFolder.getNumDetectedObjects());
    }
}
