package com.essentialappsfm.sporttechniquesprinting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CameraActivity extends Activity
{

    private static final int VIDEO_REQUEST_CODE = 200;
    private static final String TAG = CameraActivity.class.getSimpleName();
    public static final int MEDIA_TYPE_VIDEO = 1;
    Intent intentVideo;
    VideoView videoDisplay;
    private Uri fileUri;
    private String stringUri;
    private String username;
    ImageButton controlButton;
    boolean isPlaying = true;

    DisplayMetrics metrics;
    SurfaceView surfaceView;
    MediaController controller;

    private String device;

    private boolean isCameraSupported()
    {
        if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            // this device has a camera
            return true;
        }
        else
        {
            // no camera on this device
            return false;
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // Checking camera availability
        if (!isCameraSupported())
        {
            Toast.makeText(getApplicationContext(),"Sorry! There is no camera present on this device",Toast.LENGTH_LONG).show();

            // will close the app if the device doesn't have camera
            finish();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);

        //create new Intent
        intentVideo = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        //These lines are moved to move to next view
        intentVideo.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
        intentVideo.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 3);
        //intentVideo.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //intentVideo.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, Configuration.ORIENTATION_LANDSCAPE);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);  // create a file to save the video
        intentVideo.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);  // set the image file name

        // start the Video Capture Intent
        startActivityForResult(intentVideo, VIDEO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //super.onActivityResult(requestCode, resultCode, intentVideo);
        if (requestCode == VIDEO_REQUEST_CODE)
        {
            if (resultCode == RESULT_OK)
            {
                videoDisplay = (VideoView) findViewById(R.id.videoView);
                videoDisplay.setOnPreparedListener(pListener);
                videoDisplay.setVideoPath(fileUri.getPath());
                videoDisplay.start();

                //videoPlayback();
                // Video captured and saved to fileUri Intent
                stringUri = fileUri.toString();
                username = "Fergus";

                Video myVideo = new Video();
                myVideo.id = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
                myVideo.user = username;
                myVideo.vidPath = stringUri;

                SaveMongoInstance task = new SaveMongoInstance();
                task.execute(myVideo);

                Toast.makeText(this, "Video Saved to:" + stringUri, Toast.LENGTH_LONG).show();
            }
            else if (resultCode == RESULT_CANCELED)
            {
                // User cancelled the video capture
                // user cancelled recording
                Toast.makeText(getApplicationContext(),"Video Recording Cancelled", Toast.LENGTH_SHORT).show();
            }
            else
            {
                // failed to record video
                Toast.makeText(getApplicationContext(), "Failed to Record Video", Toast.LENGTH_SHORT).show();
            }
        }
    }

    MediaPlayer.OnPreparedListener pListener = new MediaPlayer.OnPreparedListener()
    {
        @Override
        public void onPrepared(MediaPlayer mp)
        {
            mp.setVolume(0f, 0f);
        }
    };

    public void playPause(View view)
    {
        controlButton = (ImageButton) findViewById(R.id.play_button);
        if(isPlaying)
        {
            controlButton.setImageResource(R.drawable.ic_action_play);
            videoDisplay.pause();
        }
        else
        {
            controlButton.setImageResource(R.drawable.ic_action_pause);
            videoDisplay.start();
        }
        isPlaying = !isPlaying;
    }

    private void videoPlayback()
    {
        try
        {
            videoDisplay = (VideoView) findViewById(R.id.videoView);
            videoDisplay.setVideoPath(fileUri.getPath());
            metrics = new DisplayMetrics();
            this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int height = metrics.heightPixels;
            int width = metrics.widthPixels;
            videoDisplay.setMinimumHeight(height);
            videoDisplay.setMinimumWidth(width);
            videoDisplay.requestFocus();

            controller = new MediaController(this);
            videoDisplay.setMediaController(controller);
            videoDisplay.setOnPreparedListener(pListener);
            videoDisplay.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /** Create a file Uri for saving an image or video to specific folder
     * https://developer.android.com/guide/topics/media/camera.html#saving-media
     * */
    private static Uri getOutputMediaFileUri(int type)
    {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type)
    {
        // To be safe, you should check that the SDCard is mounted

        if(Environment.getExternalStorageState() != null)
        {
            // this works for Android 2.2 and above
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCamera");

            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (! mediaStorageDir.exists())
            {
                if (! mediaStorageDir.mkdirs())
                {
                    Log.d(TAG, "failed to create directory");
                    return null;
                }
            }

            // Create a media file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File mediaFile;
            if(type == MEDIA_TYPE_VIDEO)
            {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "VID_"+ timeStamp + ".mp4");
            } else
            {
                return null;
            }
            return mediaFile;
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}