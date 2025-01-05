import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.TrackedObject;
import bgu.spl.mics.application.objects.CloudPoint;
import bgu.spl.mics.application.objects.LandMark;


public class FusionSlamTest {

    private FusionSlam fusionSlam;

    @BeforeEach
    public void setUp() {
        // Initialize the FusionSlam singleton
        fusionSlam = FusionSlam.getInstance();
        fusionSlam.getLandmarks().clear(); // Clear existing landmarks for isolated testing
        fusionSlam.getPoses().clear(); // Clear existing poses for isolated testing
    }

    @Test
    public void testTransformTrackedObjectsToLandmarks_NewLandmark() {
        // Create a tracked object that will be added as a new landmark
        TrackedObject trackedObject = new TrackedObject("1", 10, "Tree", createSampleCloudPoints(3));
        Pose pose = new Pose(0, 0, 10, 0); // Pose at the origin with no rotation

        // Call the method to transform the tracked object
        fusionSlam.transformCoordinatesToGlobal(trackedObject, pose);

        // Add the landmark
        boolean isNewLandmark = fusionSlam.isNewLandmark(trackedObject);
        assertTrue(isNewLandmark, "The tracked object should be identified as a new landmark.");
        fusionSlam.addLandmark(trackedObject);

        // Verify that the landmark was added
        assertEquals(1, fusionSlam.getLandmarks().size(), "There should be exactly one landmark.");
        LandMark addedLandmark = fusionSlam.getLandmarks().get(0);
        assertEquals("1", addedLandmark.getId(), "The landmark ID should match the tracked object ID.");
        assertEquals("Tree", addedLandmark.getDescription(), "The landmark description should match the tracked object description.");
    }

    @Test
    public void testTransformTrackedObjectsToLandmarks_UpdateLandmark() {
        // Create an existing landmark
        TrackedObject trackedObject = new TrackedObject("1", 10, "Tree", createSampleCloudPoints(3));
        fusionSlam.addLandmark(trackedObject);

        // Create an updated version of the tracked object with new coordinates
        TrackedObject updatedObject = new TrackedObject("1", 15, "Tree", createSampleCloudPoints(5));
        fusionSlam.updateLandmark(updatedObject);

        // Verify the landmark was updated
        LandMark updatedLandmark = fusionSlam.getLandmarks().get(0);
        assertEquals(5, updatedLandmark.getCoordinates().size(), "The landmark should have updated coordinates.");
    }
    // Helper method to create a list of sample cloud points
    private List<CloudPoint> createSampleCloudPoints(int count) {
        List<CloudPoint> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            points.add(new CloudPoint(i * 10, i * 20));
        }
        return points;
    }
}
