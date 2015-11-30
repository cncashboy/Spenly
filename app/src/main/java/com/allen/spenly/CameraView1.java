package com.allen.spenly;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Environment;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Allen on 15/11/28.
 */
public class CameraView1 extends SurfaceView implements SurfaceHolder.Callback, android.hardware.Camera.PictureCallback {

    private SurfaceHolder holder;
    private Camera camera;
    private Activity act;
    private Handler handler = new Handler();
    private Context context;
    private SurfaceView surfaceView;
    private AudioManager audio;
    private int current;


    private android.hardware.Camera.Parameters parameters;
    public CameraView1(final Context context) {
        super(context);
        surfaceView =this;
        audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final  int curent = audio.getRingerMode();
        audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        this.context = context;
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (camera == null) {
                    handler.postDelayed(this, 4 * 1000);// 由于启动camera需要时间，在此让其等两秒再进行聚焦知道camera不为空
                } else {
                    camera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success) {
                                camera.takePicture(new Camera.ShutterCallback() {
                                    @Override
                                    public void onShutter() {
                                        Toast.makeText(context,"自动对焦success",Toast.LENGTH_SHORT).show();
                                    }
                                }, null, CameraView1.this);
                            } else {
                                Toast.makeText(context,"自动对焦失败",Toast.LENGTH_SHORT).show();

                            }
                        }
                    });
                }
            }
        }, 2 * 1000);
    }
    public CameraView1(Context context, Activity act) {// 在此定义一个构造方法用于拍照过后把CameraActivity给finish掉
        this(context);
        this.act = act;
    }
    /**


     * */
    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        camera = Camera.open();// 摄像头的初始化
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (holder != null) {
                    try {
                        camera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else {
                    handler.postDelayed(this, 1 * 1000);
                }
            }
        }, 2 * 1000);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        parameters = camera.getParameters();
        camera.setParameters(parameters);// 设置参数
        camera.startPreview();// 开始预览

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
        try {

            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            String time = format.format(date);

            //在SD卡上创建文件夹
            File file = new File(Environment.getExternalStorageDirectory()
                    + "/myCamera/pic");
            if (!file.exists()) {

                file.mkdirs();
            }

            String path = Environment.getExternalStorageDirectory()
                    + "/myCamera/pic/" + time + ".jpg";
            data2file(data, path);
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
            holder.removeCallback(CameraView1.this);
            audio.setRingerMode(current);
            act.finish();

            uploadFile(path);

        } catch (Exception e) {

        }
    }
    private void data2file(byte[] w, String fileName) throws Exception {// 将二进制数据转换为文件的函数
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(fileName);
            out.write(w);
            out.close();
        } catch (Exception e) {
            if (out != null)
                out.close();
            throw e;
        }
    }
      private void uploadFile(String filePath)// 拍照过后上传文件到服务器
  {
      Toast.makeText(context,"uploadFile",Toast.LENGTH_SHORT).show();

  }
}
