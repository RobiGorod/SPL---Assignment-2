package bgu.spl.mics.application.objects;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks identified.
 */
public class StatisticalFolder {
    private final AtomicInteger systemRuntime;
    private final AtomicInteger numDetectedObjects;
    private final AtomicInteger numTrackedObjects;
    private final AtomicInteger numLandmarks;


    private static class StatisticalFolderHolder{
        // Singleton instance holder
        private static StatisticalFolder instance = new StatisticalFolder(0,0,0,0);
        }
        
    public static StatisticalFolder getInstance() {
        return StatisticalFolderHolder.instance;
        }
    
        private StatisticalFolder(int systemRuntime, int numDetectedObjects, int numTrackedObjects, int numLandmarks) {
            this.systemRuntime =  new AtomicInteger(systemRuntime);
            this.numDetectedObjects =  new AtomicInteger(numDetectedObjects);
            this.numTrackedObjects =  new AtomicInteger(numTrackedObjects);
            this.numLandmarks =  new AtomicInteger(numLandmarks);
        }

        // Getters
    
        public int getSystemRuntime() {
            return systemRuntime.get();
        }
    
        public int getNumDetectedObjects() {
            return numDetectedObjects.get();
        }
    
        public int getNumTrackedObjects() {
            return numTrackedObjects.get();
        }
    
        public int getNumLandmarks() {
            return numLandmarks.get();
        }

        // Setters

        public void incrementSystemRuntime(int increment) {
            systemRuntime.addAndGet(increment);
        }

        public void incrementDetectedObjects(int increment) {
            numDetectedObjects.addAndGet(increment);
        }

        public void incrementTrackedObjects(int increment) {
            numTrackedObjects.addAndGet(increment);
        }

        public void incrementLandmarks(int increment) {
            numLandmarks.addAndGet(increment);
        }

        public void reset(){
             systemRuntime.set(0);
             numDetectedObjects.set(0);
             numTrackedObjects.set(0);
             numLandmarks.set(0);
        }
    
}
