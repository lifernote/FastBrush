package co.adrianblan.fastbrush.touch;

import android.util.Log;

import java.util.ArrayList;

import co.adrianblan.fastbrush.utils.Utils;

/**
 * Class which contains TouchData objects and methods.
 */
public class TouchDataManager {

    private ArrayList<TouchData> touchDataList;
    private TouchData prevTouchData;

    private boolean touchHasStarted;
    private boolean touchHasEnded;
    private boolean touchIsEnding;

    private int numTouches;
    private float averageTouchSize;
    private float minTouchSize;
    private float maxTouchSize;

    public TouchDataManager() {
        touchDataList = new ArrayList<>();
        touchHasEnded = true;
        minTouchSize = 99999;
    }

    public TouchDataManager(int numTouches, float averageTouchSize, float minTouchSize, float maxTouchSize) {
        this();
        this.numTouches = numTouches;
        this.averageTouchSize = averageTouchSize;
        this.minTouchSize = minTouchSize;
        this.maxTouchSize = maxTouchSize;
    }

    /** Takes touch data information, and interpolates objects based on a distance to the previous object */
    public void addInterpolated(TouchData touchData){

        final float MIN_DISTANCE = 0.005f;

        addTouchStatistics(touchData);


        if(!touchHasStarted && (prevTouchData == null
                || touchData.getPosition().distance(prevTouchData.getPosition()) > MIN_DISTANCE)) {
            add(touchData);
            prevTouchData = touchData;
            touchHasStarted = true;
        } else if(touchHasStarted && prevTouchData != null) {

            float distance = touchData.getPosition().distance(prevTouchData.getPosition());

            TouchData parentTouchData = prevTouchData;
            TouchData prevInterpolatedTouchData = prevTouchData;

            int maxInterpolations = (int) (distance / MIN_DISTANCE);

            // Interpolate so that there are no gaps larger than MIN_DISTANCE
            if (maxInterpolations > 0) {

                for (int i = 1; i <= maxInterpolations; i++) {

                    prevInterpolatedTouchData = getInterpolatedTouchData(touchData, parentTouchData,
                            prevInterpolatedTouchData.getSize(), prevInterpolatedTouchData.getPressure(),
                            i, maxInterpolations);

                    add(prevInterpolatedTouchData);
                }
            }


            if(distance > MIN_DISTANCE && !touchIsEnding) {
                // Throttle values so that they do not increase too quickly
                float size = Utils.getThrottledValue(parentTouchData.getSize(), touchData.getSize(), 0.01f);
                float pressure = Utils.getThrottledValue(parentTouchData.getPressure(), touchData.getPressure());

                TouchData td = new TouchData(touchData.getPosition(), touchData.getVelocity(), size, pressure);

                add(td);
            }
        }
    }

    /**
     * Cretates an interpolated TouchData object.
     *
     * @param touchData the TouchData object to interpolate to
     * @param parentTouchData the TouchData object to interpolate position from
     * @param prevSize the size to interpolate from
     * @param prevPressure the pressure to interpolate from
     * @param interpolation the current interpolation
     * @param maxInterpolations the maximum number of interpolations
     * @return
     */
    private TouchData getInterpolatedTouchData(TouchData touchData, TouchData parentTouchData,
                                          float prevSize, float prevPressure,
                                          int interpolation, int maxInterpolations) {

        float interpolationScale = (interpolation) / ((float) maxInterpolations);

        float x = parentTouchData.position.x + (touchData.position.x - parentTouchData.position.x)
                * interpolationScale;

        float y = parentTouchData.position.y + (touchData.position.y - parentTouchData.position.y)
                * interpolationScale;

        float xv = parentTouchData.velocity.x + (touchData.velocity.x - parentTouchData.velocity.x)
                * interpolationScale;
        float yv = parentTouchData.velocity.y + (touchData.velocity.y - parentTouchData.velocity.y) *
                interpolationScale;

        float size = Utils.getThrottledValue(prevSize, touchData.getSize(), 0.01f);
        float pressure  = Utils.getThrottledValue(prevPressure, touchData.getPressure());

        TouchData interpolatedTouchData = new TouchData(x, y, xv, yv, size, pressure);

        return interpolatedTouchData;
    }

    /** Adds a TouchData object to the list */
    private void add(TouchData touchData) {
        normalizeTouchSize(touchData);
        touchDataList.add(touchData);
        prevTouchData = touchData;
        touchHasEnded = false;
    }

    private void addTouchStatistics(TouchData touchData) {
        averageTouchSize = (averageTouchSize * (numTouches / (numTouches + 1f)))
                + (touchData.getSize() / (numTouches + 1f));

        numTouches++;

        minTouchSize = Math.min(touchData.getSize(), minTouchSize);
        maxTouchSize = Math.max(touchData.getSize(), maxTouchSize);
    }

    /** Returns the list of TouchData */
    public ArrayList<TouchData> get() {
        return touchDataList;
    }

    /** Returns whether there is any TouchData in the list */
    public boolean hasTouchData() {
        return !touchDataList.isEmpty();
    }

    /** Gets the last TouchData if it exists, otherwise null */
    public TouchData getLast() {
        return prevTouchData;
    }

    public boolean hasLast() {
        return prevTouchData != null;
    }

    /** Clears all the TouchData */
    public void clear() {
        touchDataList.clear();
    }

    public void touchIsEnding() {
        touchIsEnding = true;
    }

    public void touchHasEnded() {
        touchHasEnded = true;
        touchIsEnding = false;
        touchHasStarted = false;
    }

    public boolean hasTouchEnded() {
        return touchHasEnded;
    }

    public int getNumTouches() {
        return numTouches;
    }

    public float getAverageTouchSize() {
        return averageTouchSize;
    }

    public float getMinTouchSize() {
        return minTouchSize;
    }

    public float getMaxTouchSize() {
        return maxTouchSize;
    }

    /** Takes a touch size sets it to the normalized size [0, 1] */
    public void normalizeTouchSize(TouchData td) {

        float minTouchSizeAvgDistance = averageTouchSize - minTouchSize;
        float normalizedTouchSizeMin = minTouchSize + minTouchSizeAvgDistance / 2f;
        float normalizedTouchSizeMax = Math.min(averageTouchSize + minTouchSizeAvgDistance * 2, maxTouchSize);

        float normalizedSize = Utils.normalize(td.getSize(), normalizedTouchSizeMin,
                normalizedTouchSizeMax);

        td.setNormalizedSize(normalizedSize);
    }
}
