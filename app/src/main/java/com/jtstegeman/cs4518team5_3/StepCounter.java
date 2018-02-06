package com.jtstegeman.cs4518team5_3;

/**
 * Created by kyle on 2/6/18.
 */

public class StepCounter {

    private int numSteps;
    private int calibration;
    private boolean calibrated;

    private StepCounter() {

    }

    public int getNumSteps() {
        return numSteps - calibration;
    }

    public void setNumSteps(int numSteps) {
        this.numSteps = numSteps;
    }

    public int getRawSteps(){
        return numSteps;
    }

    public void calibrate(int steps){
        calibration = steps;
        setCalibrated(true);
    }

    public boolean isCalibrated() {
        return calibrated;
    }

    public void setCalibrated(boolean calibrated) {
        this.calibrated = calibrated;
    }

    public static StepCounter getInstance(){
        return StepCounterHolder.instance;
    }

    private static class StepCounterHolder {
        private static StepCounter instance = new StepCounter();
    }

}
