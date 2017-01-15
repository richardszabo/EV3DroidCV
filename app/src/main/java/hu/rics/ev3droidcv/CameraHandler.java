package hu.rics.ev3droidcv;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.net.SocketException;
import java.util.List;

import static android.R.attr.x;
import static org.opencv.core.Core.FONT_HERSHEY_DUPLEX;
import static org.opencv.core.Core.FONT_HERSHEY_SIMPLEX;
import static org.xmlpull.v1.XmlPullParser.TEXT;

/**
 * Created by rics on 2017.01.10..
 */

public class CameraHandler implements CvCameraViewListener2 {

    private Mat                  mRgba, mRgbaF, mRgbaT;
    private ColorBlobDetector    mDetector;
    private Scalar               mBlobColorHsv;
    private Scalar CONTOUR_COLOR;
    private Scalar MARKER_COLOR;
    private Scalar TEXT_COLOR;
    private String ipAddress;
    private EV3Communicator ev3Communicator;
    private Point org;

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);  // NOTE width,width is NOT a typo
        mDetector = new ColorBlobDetector();
        mBlobColorHsv = new Scalar(60/2,0.6*255,0.8*255,255); // hue in [0,180], saturation in [0,255], value in [0,255]
        mDetector.setHsvColor(mBlobColorHsv);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
        MARKER_COLOR = new Scalar(0,0,255,255);
        TEXT_COLOR = new Scalar(255,255,255,255);
        org = new Point(1,20);
        try {
            ipAddress = EV3Communicator.getIPAddress(true);
        } catch (SocketException e) {
            Log.e(MainActivity.TAG, "Cannot get IP address");
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        // Rotate mRgba 90 degrees
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );
        //

        mDetector.process(mRgba);
        List<MatOfPoint> contours = mDetector.getContours();
        if( ev3Communicator.isConnected() ) {
            Log.i(MainActivity.TAG, "Contours count: " + contours.size());
        }
        Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
        Point center = mDetector.getCenterOfMaxContour();
        double direction = 0;
        if( center != null ) {
            Imgproc.drawMarker(mRgba, center, MARKER_COLOR);
            direction = (center.x - mRgba.cols()/2)/mRgba.cols(); // portrait orientation
            if( ev3Communicator.isConnected() ) {
                Log.i(MainActivity.TAG, "direction: " + direction);
            }
        }
        int font = FONT_HERSHEY_SIMPLEX;
        if( ev3Communicator.isConnected() ) {
            if( center == null ) {
                direction = -100; // sending extreme value when yellow is not found
            }
            ev3Communicator.sendDirection(direction);
            font = FONT_HERSHEY_DUPLEX;
        }
        Imgproc.putText(mRgba,ipAddress,org,font,1,TEXT_COLOR);

        return mRgba;
    }

    public void setEV3Communicator(EV3Communicator ev3Communicator) {
        this.ev3Communicator = ev3Communicator;
    }
}
