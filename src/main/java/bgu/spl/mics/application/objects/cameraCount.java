package bgu.spl.mics.application.objects;

import java.util.concurrent.atomic.AtomicInteger;

public class cameraCount {
    private static class cameraCountHolder{
        private static cameraCount instance = new cameraCount(0);
    }
    private AtomicInteger cameras;
    private cameraCount(int cameras){
        this.cameras = new AtomicInteger(cameras);
    }
    public static cameraCount getInstance(){
        return cameraCountHolder.instance;
    }

    public int getCameraCount(){
        return cameras.get();
    }

    public void decrementCameraCount(){
        cameras.decrementAndGet();
    }

    public void setCameraCount(int count){
        cameras.set(count);
    }
}

