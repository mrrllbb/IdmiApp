package sg.edu.nus.idmiapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class GetImageByLocationActivity extends AppCompatActivity {
    String[] urlArray = new String[0];
    EditText latitude;
    EditText longitude;
    Bitmap[] bitmap;
    private static final int MSG_SUCCESS = 0;
    private static final int MSG_FAILURE = 1;
    private Thread mThread;
    private Handler mHandler = new Handler() {
        public void handleMessage (Message msg) {//此方法在ui线程运行
            switch(msg.what) {
                case MSG_SUCCESS:
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    ViewGroup group = (ViewGroup) findViewById(R.id.viewGroup);
                    ImageView[] imageViews = new ImageView[bitmap.length];
                    for (int i = 0; i < imageViews.length; i++) {
                        ImageView imageView = new ImageView(getApplication());
                        imageView.setLayoutParams(new AppBarLayout.LayoutParams(AppBarLayout.LayoutParams.MATCH_PARENT, AppBarLayout.LayoutParams.WRAP_CONTENT));
                        imageViews[i] = imageView;
                        imageView.setImageBitmap(bitmap[i]);
                        group.addView(imageView);
                    }


                    break;

                case MSG_FAILURE:
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    Toast.makeText(getApplication(), "can not find the image", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_image_by_location);
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                ViewGroup group = (ViewGroup) findViewById(R.id.viewGroup);
                group.removeAllViews();
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                mThread = new Thread(runnable);
                mThread.start();

            }
        });
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run()
        {

            try {
                latitude = (EditText)findViewById(R.id.latitude);
                longitude = (EditText)findViewById(R.id.longitude);
                String lat = latitude.getText().toString();
                String lon = longitude.getText().toString();
                String path = "http://ec2-54-218-40-64.us-west-2.compute.amazonaws.com:8080/IcubeServer/enquiryImagesWithCoordinate?latitude="+lat+"&longitude="+lon;
                URL url = new URL(path);
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(15 * 1000);
                if (con.getResponseCode() == 200) {
                    InputStream is = con.getInputStream();
                    byte[] data = readStream(is);
                    String json = new String(data);
                    JSONArray jsonArray = new JSONArray(json);
                    int total = jsonArray.length();
                    urlArray = new String[total];
                    for(int i=0;i<total;i++){
                        urlArray[i] = (String)jsonArray.get(i);
                    }

                }
                if(urlArray.length!=0){
                    bitmap = new Bitmap[urlArray.length];
                    for(int i=0;i<urlArray.length;i++){
                        byte[] data = getImage(urlArray[i]);
                        bitmap[i] = BitmapFactory.decodeByteArray(data, 0, data.length);
                    }


                    mHandler.obtainMessage(MSG_SUCCESS).sendToTarget();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                mHandler.obtainMessage(MSG_FAILURE).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
                mHandler.obtainMessage(MSG_FAILURE).sendToTarget();
            } catch (Exception e) {
                e.printStackTrace();
                mHandler.obtainMessage(MSG_FAILURE).sendToTarget();
            }

        }
    };

        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_get_image_by_location, menu);
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
    private static byte[] readStream(InputStream inputStream) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            bout.write(buffer, 0, len);
        }
        bout.close();
        inputStream.close();
        return bout.toByteArray();
    }
    public byte[] getImage(String path) throws IOException {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");   //设置请求方法为GET
        conn.setReadTimeout(5*1000);    //设置请求过时时间为5秒
        InputStream inputStream = conn.getInputStream();   //通过输入流获得图片数据
        byte[] data = new byte[0];     //获得图片的二进制数据
        try {
            data = readStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;

    }
}
