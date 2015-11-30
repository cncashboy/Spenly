package com.allen.spenly;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;


import com.allen.spenly.tools.ImgTools;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * Created by Allen on 15/11/29.
 */
public class CustomCamera extends Activity implements SurfaceHolder.Callback {
    private Camera.Parameters parameters;
    private Camera mCamera;
    private SurfaceView mPerview;
    private SurfaceHolder mHolder;
    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Uri imageUri = CustomCamera.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());

            File tempFile = new File(Environment.getExternalStorageDirectory(), "spenly" + ".png");
            try {
                FileOutputStream fos = new FileOutputStream(tempFile);
                fos.write(data);
                fos.close();

                Bitmap bitmap = BitmapFactory.decodeFile(tempFile.getAbsolutePath());
                //设置FOCUS_MODE_CONTINUOUS_VIDEO)之后，myParam.set("rotation", 90)失效。
                //图片竟然不能旋转了，故这里要旋转下
                Bitmap rotaBitmap = ImgTools.getRotateBitmap(bitmap, 90.0f);
                String bp = ImgTools.bitmaptoString(rotaBitmap);
                mCamera.startPreview();
                //capture();
                uploadFile(bp, url);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private Button button;
    private String url = "http://spenly.com/live/push";
    private static int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom);
        mPerview = (SurfaceView) findViewById(R.id.surfaceview);
        button = (Button) findViewById(R.id.btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capture();
            }
        });
        mHolder = mPerview.getHolder();
        mHolder.addCallback(CustomCamera.this);
    }

    private void capture() {

        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    Toast.makeText(CustomCamera.this, "对焦完成", Toast.LENGTH_LONG).show();
                    mCamera.takePicture(null, null, mPictureCallback);

                } else {
                    Toast.makeText(CustomCamera.this, "对焦失败", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    //相机参数的初始化设置
    private void initCamera()
    {
        int PreviewWidth = 0;
        int PreviewHeight = 0;
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);//获取窗口的管理器
        Display display = wm.getDefaultDisplay();//获得窗口里面的屏幕
        parameters=mCamera.getParameters();
        // 选择合适的预览尺寸
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
        Toast.makeText(CustomCamera.this,
                sizeList.size()+"", Toast.LENGTH_LONG).show();
        // 如果sizeList只有一个我们也没有必要做什么了，因为就他一个别无选择
        if (sizeList.size() > 1) {
            Iterator<Camera.Size> itor = sizeList.iterator();
            for (int i = 0; i < sizeList.size(); i++) {
                Camera.Size cur = itor.next();

                Log.d("alen","width="+cur.width+"hight="+cur.height+"\n");

            }
            while (itor.hasNext()) {
                Camera.Size cur = itor.next();
                if (cur.width >= PreviewWidth
                        && cur.height >= PreviewHeight) {
                    PreviewWidth = cur.width;
                    PreviewHeight = cur.height;
                    break;
                }
            }
        }
        Toast.makeText(CustomCamera.this,
                PreviewWidth+"----"+PreviewHeight, Toast.LENGTH_LONG).show();
        parameters.setPreviewSize(1280, 720);
        parameters.setJpegQuality(85);
        parameters.setPictureFormat(PixelFormat.JPEG);
        parameters.setPictureSize(1280,720);  // 部分定制手机，无法正常识别该方法。
        // parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mCamera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            mCamera = getmCamera();
            initCamera();
            if (mHolder != null) {
                setStartPreview(mCamera, mHolder);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    /**
     * 获取Camera对象
     *
     * @return
     */
    private Camera getmCamera() {
        Camera camera;
        try {
            camera = Camera.open();

        } catch (Exception e) {
            camera = null;
            e.printStackTrace();
        }
        return camera;
    }

    /**
     * 开始预览相机内容
     */
    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            //将系统Camera预览角度进行调整
            camera.setDisplayOrientation(90);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放相机资源
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;

        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setStartPreview(mCamera, mHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.stopPreview();
        setStartPreview(mCamera, mHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    /**
     * @param path 要上传的文件路径
     * @param url  服务端接收URL
     * @throws Exception
     */
    public void uploadFile(String path, String url) throws Exception {

        if (path.length() > 0) {
            AsyncHttpClient client = new AsyncHttpClient();
            RequestParams params = new RequestParams();
            params.put("sn", "test");
            params.put("ct", path);
            // 上传文件
            client.post(url, params, new AsyncHttpResponseHandler() {
                @Override
                public void onStart() {
                    super.onStart();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers,
                                      byte[] responseBody) {
                    count++;
                    String response = new String(responseBody);
                    Toast.makeText(CustomCamera.this, "上传成功" + count + "张", Toast.LENGTH_LONG).show();
                    // 上传成功后要做的工作
                    capture();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers,
                                      byte[] responseBody, Throwable error) {
                    // 上传失败后要做到工作
                    Toast.makeText(CustomCamera.this, "上传失败", Toast.LENGTH_LONG).show();
                }


                @Override
                public void onRetry(int retryNo) {
                    // TODO Auto-generated method stub
                    super.onRetry(retryNo);
                    // 返回重试次数
                }

            });
        } else {
            Toast.makeText(CustomCamera.this, "文件不存在", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK){
            CustomCamera.this.finish();
            return  true;
        }
            return super.onKeyDown(keyCode, event);

    }
}
