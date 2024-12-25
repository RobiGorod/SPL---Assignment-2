package bgu.spl.mics.application.objects;

/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks identified.
 */
public class StatisticalFolder {
        private int systemRuntime;
        private int numDetectedObjects;
        private int numTrackedObjects;
        private int numLandmarks;
    
        public StatisticalFolder(int systemRuntime, int numDetectedObjects, int numTrackedObjects, int numLandmarks) {
            this.systemRuntime = systemRuntime;
            this.numDetectedObjects = numDetectedObjects;
            this.numTrackedObjects = numTrackedObjects;
            this.numLandmarks = numLandmarks;
        }
    
        public int getSystemRuntime() {
            return systemRuntime;
        }
    
        public int getNumDetectedObjects() {
            return numDetectedObjects;
        }
    
        public int getNumTrackedObjects() {
            return numTrackedObjects;
        }
    
        public int getNumLandmarks() {
            return numLandmarks;
        }
}
