package hu.rics.ev3droidcv;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;

/**
 * Created by rics on 2017.01.10..
 */

public class CameraHandler implements CvCameraViewListener2 {

    private Mat                  mRgba;
    private ColorBlobDetector    mDetector;
    private Scalar               mBlobColorHsv;
    private Scalar CONTOUR_COLOR;


    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mBlobColorHsv = new Scalar(255,0,0,255);
        mDetector.setHsvColor(mBlobColorHsv);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        mDetector.process(mRgba);
        List<MatOfPoint> contours = mDetector.getContours();
        Log.e(MainActivity.TAG, "Contours count: " + contours.size());
        Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

        return mRgba;
    }
}
