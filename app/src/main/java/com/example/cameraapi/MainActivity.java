package com.example.cameraapi;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Button;

import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import androidx.appcompat.app.AppCompatActivity;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /** A safe way to get an instance of the Camera object. */
    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(0); // Camera.open(0) pour les tablettes
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private Camera mCamera;
    private CameraPreview mPreview;
    Camera.PreviewCallback mPicture;
    private WebsocketClient ws;

    public static boolean mutex = false;
    public static long startTime;
    int nbArrays = 0;

    byte[] prevData;

    public Button button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.text_main);

        try {
            ws = initWebsocket();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // Create an instance of Camera
        mCamera = getCameraInstance();

        Camera.Parameters mParameters = mCamera.getParameters();

        List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPreviewSizes();

        for(int i = 1; i < sizeList.size(); i++) {
            Log.i("SIZES", sizeList.get(i).width + " " + sizeList.get(i).height);
        }
        mParameters.setPreviewSize(320, 240);
        /*List<Integer> previewFormats = mParameters.getSupportedPreviewFormats();
        for (int i = 0; i < previewFormats.size(); i++) {
            Log.i("PREVIEW FORMAT", String.valueOf(previewFormats.get(i)));
        }*/
        mParameters.setPreviewFormat(ImageFormat.NV21);
        mCamera.setParameters(mParameters);
        Log.i("FORMATS", mParameters.getSupportedPreviewFormats().toString());

        mPicture = new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                //prevData = data.clone();
                //Log.i("LENGTH", String.valueOf(data.length));

                if (!mutex && ws.isOpen()) {
                    mutex = true;
                    startTime = System.nanoTime();
                    ws.send(data);
                    Log.i("SENDING IMAGE TIME", String.valueOf(System.nanoTime()-startTime));
                }
            }
        };

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera, mPicture);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        mCamera.setPreviewCallback(mPicture);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }

    private WebsocketClient initWebsocket() throws URISyntaxException {
        WebsocketClient c = new WebsocketClient( new URI( "ws://192.168.43.121:9002" )); // more about drafts here: http://github.com/TooTallNate/Java-WebSocket/wiki/Drafts
        //WebsocketClient c = new WebsocketClient( new URI( "ws://192.168.1.4:9002" )); // more about drafts here: http://github.com/TooTallNate/Java-WebSocket/wiki/Drafts
        c.connect();
        return c;
    }

    private class WebsocketClient extends WebSocketClient {

        public WebsocketClient(URI serverUri , Draft draft ) {
            super( serverUri, draft );
        }

        public WebsocketClient(URI serverURI ) {
            super( serverURI );
        }

        public WebsocketClient(URI serverUri, Map<String, String> httpHeaders ) {
            super(serverUri, httpHeaders);
        }

        @Override
        public void onOpen( ServerHandshake handshakedata ) {
            //send("Hello, it is me. Mario :)");
            Log.i("WEBSOCKET", "opened connection" );
            // if you plan to refuse connection based on ip or httpfields overload: onWebsocketHandshakeReceivedAsClient
        }

        @Override
        public void onMessage( String message ) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Stuff that updates the UI
                    if (!message.equals("") && !message.equals("not an image")) button.setBackgroundColor(getResources().getColor(R.color.red));
                    else button.setBackgroundColor(getResources().getColor(R.color.green));
                }
            });

            Log.i("WEBSOCKET TIME", String.valueOf(System.nanoTime()-MainActivity.startTime));
            Log.i("WEBSOCKET", "received: " + message );
            MainActivity.mutex = false;
        }

        @Override
        public void onClose( int code, String reason, boolean remote ) {
            // The codecodes are documented in class org.java_websocket.framing.CloseFrame
            Log.i("WEBSOCKET", "Connection closed by " + ( remote ? "remote peer" : "us" ) + " Code: " + code + " Reason: " + reason );
        }

        @Override
        public void onError( Exception ex ) {
            ex.printStackTrace();
            // if the error is fatal then onClose will be called additionally
        }

    }
}
