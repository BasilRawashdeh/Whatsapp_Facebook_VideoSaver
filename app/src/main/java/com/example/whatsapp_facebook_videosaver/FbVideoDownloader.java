package com.example.whatsapp_facebook_videosaver;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import static android.content.Context.DOWNLOAD_SERVICE;

public class FbVideoDownloader implements VideoDownloader {

    private Context context;
    private String VideoURL;
    private long DownLoadID;
    private String VideoTitle;

    public FbVideoDownloader(Context context, String videoURL) {
        this.context = context;
        VideoURL = videoURL;
    }

    @Override
    public String createDirectory() {

        File Whatsbook = new File(Environment.getExternalStorageDirectory()+File.separator+"Facebook_Video");

        if(!Whatsbook.exists() && !Whatsbook.isDirectory())
        {
            // create empty directory
            if (Whatsbook.mkdirs())
            {
                Log.i("CreateDir","App dir created");
                Facebook_fragment.progressBar.setVisibility(View.INVISIBLE);
                Facebook_fragment.downlaod.setVisibility(View.VISIBLE);
                Facebook_fragment.downloadericon.setVisibility(View.VISIBLE);
            }
            else
            {
                Log.w("CreateDir","Unable to create app dir!");
            }
        }
        else
        {
            Log.i("CreateDir","App dir already exists");
            Facebook_fragment.progressBar.setVisibility(View.INVISIBLE);
            Facebook_fragment.downlaod.setVisibility(View.VISIBLE);
            Facebook_fragment.downloadericon.setVisibility(View.VISIBLE);
            Facebook_fragment.link.setText("");
        }
        assert Whatsbook != null;
        return Whatsbook.getPath();
    }

    @Override
    public String getVideoId(String link) {
        return link;
    }

    @Override
    public void DownloadVideo() {
        new Data().execute(getVideoId(VideoURL));
    }

    @SuppressLint("StaticFieldLeak")
    private class Data extends AsyncTask<String, String,String>{
        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection;
            BufferedReader reader;
            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                String buffer = "No URL";
                String Line;
                while ((Line = reader.readLine()) != null)
                {
                    if(Line.contains("og:video:url"))
                    {
                        Line = Line.substring(Line.indexOf("og:video:url"));
                        if(Line.contains("og:title"))
                        {
                            VideoTitle = Line.substring(Line.indexOf("og:title"));
                            VideoTitle = VideoTitle.substring(ordinalIndexOf(VideoTitle,"\"",1)+1,ordinalIndexOf(VideoTitle,"\"",2));
                        }
                        Line = Line.substring(ordinalIndexOf(Line,"\"",1)+1,ordinalIndexOf(Line,"\"",2));
                        if(Line.contains("amp;")) {
                            Line = Line.replace("amp;", "");
                        }
                        if(!Line.contains("https"))
                        {
                            Line = Line.replace("http","https");
                        }
                        buffer=Line;
                        break;
                    }
                    else {
                        buffer = "No URL";
                    }
                }
                return buffer;
            } catch (IOException e) {
                return "No URL";
            }

        }

        @Override
        protected void onPostExecute(String s) {
            if(!s.contains("No URL")) {
                String path = createDirectory();
                if(VideoTitle == null || VideoTitle.equals(""))
                {
                    VideoTitle = "fbVideo" + new Date().toString()+".mp4";
                }
                else {
                    VideoTitle = VideoTitle + ".mp4";
                }
                File newFile = new File(path, VideoTitle);
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(s));
                    request.allowScanningByMediaScanner();
                    request.setDescription(VideoTitle)
                            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE)
                            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                            .setDestinationUri(Uri.fromFile(newFile))
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setVisibleInDownloadsUi(true)
                            .setTitle("Downloading");
                    DownloadManager manager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
                    assert manager != null;
                    DownLoadID = manager.enqueue(request);
                } catch (Exception e) {
                    Looper.prepare();
                    Toast.makeText(context, "Video Can't be downloaded! Try Again", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }

            }
            else {
                if (Looper.myLooper() == null)
                {
                    Looper.prepare();
                }
                Toast.makeText(context, "Wrong Video URL or Check Internet Connection", Toast.LENGTH_SHORT).show();
                Facebook_fragment.progressBar.setVisibility(View.INVISIBLE);
                Facebook_fragment.downlaod.setVisibility(View.VISIBLE);
                Facebook_fragment.downloadericon.setVisibility(View.VISIBLE);
                Looper.loop();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (Looper.myLooper() == null)
            {
                Looper.prepare();
            }
            Toast.makeText(context, "Video Can't be downloaded! Try Again", Toast.LENGTH_SHORT).show();
            Looper.loop();
        }
    }

    private static int ordinalIndexOf(String str, String substr, int n) {
        int pos = -1;
        do {
            pos = str.indexOf(substr, pos + 1);
        } while (n-- > 0 && pos != -1);
        return pos;
    }

}