package hu.rics.ev3droidcv;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.List;

import static android.R.attr.path;
import static android.R.attr.x;
import static android.os.Environment.getExternalStoragePublicDirectory;
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
    private int imgCounter = 0;
    Activity parent;
    File storageDir;

    public CameraHandler(Activity parent) {
        this.parent = parent;
        storageDir = parent.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if( storageDir.exists() ) {
            String[] children = storageDir.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(storageDir, children[i]).delete();
            }
        } else {
            storageDir.mkdir();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);  // NOTE width,width is NOT a typo
        mDetector = new ColorBlobDetector();
        mBlobColorHsv = new Scalar(280/2,0.65*255,0.75*255,255); // hue in [0,180], saturation in [0,255], value in [0,255]
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
                direction = -100; // sending extreme value when blue is not found
            }
            ev3Communicator.sendDirection(direction);
            font = FONT_HERSHEY_DUPLEX;
        }
        Imgproc.putText(mRgba,ipAddress,org,font,1,TEXT_COLOR);
        saveMatToImage(mRgba,"ball");

        return mRgba;
    }

    void saveMatToImage(Mat mat,String imageName) {
        String imageFullName = imageName+(imgCounter++)+".jpg";
        File file = new File(storageDir.getPath(), imageFullName);
        try {
            OutputStream fOut = new FileOutputStream(file);
            // convert to bitmap:
            Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bm);
            bm.compress (Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.close();
            //MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
        } catch(FileNotFoundException e) {
            Log.e(MainActivity.TAG, "Cannot save file (not found):" + path + ":" + imageFullName + ":");
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Cannot close file:" + path + ":" + imageFullName + ":");
        }

    }

    public void setEV3Communicator(EV3Communicator ev3Communicator) {
        this.ev3Communicator = ev3Communicator;
    }
}
