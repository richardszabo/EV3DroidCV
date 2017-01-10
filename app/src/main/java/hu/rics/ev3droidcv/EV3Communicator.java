package hu.rics.ev3droidcv;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static android.R.attr.direction;

/**
 * Created by rics on 2017.01.10..
 */

public class EV3Communicator extends AsyncTask<Void, Void, Boolean>  {

    private static ServerSocket socket;
    private static Socket conn;
    public static DataOutputStream out;
    private boolean isConnected = false;

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            Log.i(MainActivity.TAG,"Serversocket creation");
            socket = new ServerSocket(1234);
            Log.i(MainActivity.TAG,"accepting");
            conn = socket.accept();
            Log.i(MainActivity.TAG,"stream");
            out = new DataOutputStream(conn.getOutputStream());
            return true;
        } catch (IOException e) {
            Log.e(MainActivity.TAG, e.getMessage());
            Log.e(MainActivity.TAG, e.getCause().toString());
        }
        return false;
    }

    @Override
    public void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if( result ) {
            isConnected = true;
        }
        Log.i(MainActivity.TAG,"Connect state:" + isConnected);
    }

    boolean isConnected() {
        return isConnected;
    }

    /**
     * Sending direction info to EV3.
     *
     * @param direction value in [-0.5,0.5]
     */
    public void sendDirection(double direction) {
        if( isConnected() ) {
            try {
                out.writeDouble(direction);
            } catch (IOException e) {
                Log.e(MainActivity.TAG,"Cannot send.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Get IP address from first non-localhost interface
     * taken from here: http://stackoverflow.com/a/13007325/21047
     * @param useIPv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) throws SocketException {
        List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface intf : interfaces) {
            List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
            for (InetAddress addr : addrs) {
                if (!addr.isLoopbackAddress()) {
                    String sAddr = addr.getHostAddress();
                    //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                    boolean isIPv4 = sAddr.indexOf(':')<0;

                    if (useIPv4) {
                        if (isIPv4)
                            return sAddr;
                    } else {
                        if (!isIPv4) {
                            int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                            return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                        }
                    }
                }
            }
        }
        return null;
    }
}
