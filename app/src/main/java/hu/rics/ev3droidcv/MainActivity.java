package hu.rics.ev3droidcv;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.net.SocketException;

import static hu.rics.ev3droidcv.EV3Communicator.getIPAddress;
import static org.opencv.android.OpenCVLoader.initDebug;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "EV3DroidCV";
    private CameraHandler cameraHandler;
    private CameraBridgeViewBase mOpenCvCameraView;
    private EV3Communicator ev3Communicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.ev3droid_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.ev3droid_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        cameraHandler = new CameraHandler(this);
        ev3Communicator = new EV3Communicator();
        cameraHandler.setEV3Communicator(ev3Communicator);
        mOpenCvCameraView.setCvCameraViewListener(cameraHandler);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG,"OpenCV Manager Connected");
                    mOpenCvCameraView.enableView();
                    ev3Communicator.execute();
                    break;
                default:
                    Log.i(TAG,"OpenCV Manager Install");
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //initialize OpenCV manager
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
}
