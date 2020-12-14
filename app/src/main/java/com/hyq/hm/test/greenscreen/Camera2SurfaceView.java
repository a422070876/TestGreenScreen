package com.hyq.hm.test.greenscreen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.core.content.PermissionChecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Camera2SurfaceView extends SurfaceView {

    private EGLUtils  mEglUtils = new EGLUtils();
    private GLVideoRenderer videoRenderer = new GLVideoRenderer();
    private GLRenderer mRenderer = new GLRenderer();
//    private GLBitmapRenderer bitmapRenderer = new GLBitmapRenderer();
    private GLImageRenderer imageRenderer = new GLImageRenderer();
    private GLDrawRenderer drawRenderer = new GLDrawRenderer();

    private float[] coordinate = null;
    private Rect drawRect = null;
    private boolean isShowImage = false;

    private String mCameraId;
    private CameraManager mCameraManager;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private Handler mHandler;

    private int screenWidth = -1, screenHeight,previewWidth,previewHeight;
    private Rect rect = new Rect();

    public Rect getRect() {
        return rect;
    }

    private Handler cameraHandler;
    private HandlerThread cameraThread;

    public Camera2SurfaceView(Context context) {
        super(context);
        init();
    }

    public Camera2SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){



        cameraThread = new HandlerThread("Camera2Thread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());

        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_car);

        initCamera2();
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                cameraHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mEglUtils.initEGL(getHolder().getSurface());
                        GLES20.glEnable(GLES20.GL_BLEND);
                        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                        mRenderer.initShader();
                        videoRenderer.initShader();
//                        bitmapRenderer.initShader(bitmap);
                        drawRenderer.initShader();
                        imageRenderer.initShader();
                        imageRenderer.setBitmap(bitmap);
                        videoRenderer.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                            @Override
                            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                                cameraHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(mCameraCaptureSession == null){
                                            return;
                                        }
                                        if(coordinate != null && !isShowImage){
                                            isShowImage = true;
                                            float left_up_x = coordinate[0];
                                            float left_up_y = coordinate[1];
                                            float right_up_x = coordinate[2];
                                            float right_up_y = coordinate[3];
                                            float left_down_x = coordinate[4];
                                            float left_down_y = coordinate[5];
                                            float right_down_x = coordinate[6];
                                            float right_down_y = coordinate[7];
                                            float[] positionData = new float[8];
                                            float[] attributesData = new float[12];
                                            float[] textureVertexData = new float[8];
                                            float left = coordinate[0];
                                            float top = coordinate[1];
                                            float right = coordinate[0];
                                            float bottom = coordinate[1];
                                            for (int i = 2; i < coordinate.length; i+=2) {
                                                left = Math.min(left,coordinate[i]);
                                                top = Math.min(top,coordinate[i+1]);
                                                right = Math.max(right,coordinate[i]);
                                                bottom = Math.max(bottom,coordinate[i+1]);
                                            }
                                            float h = bottom - top;
                                            float f = previewHeight - bottom - top;

                                            textureVertexData[0] = right/previewWidth;
                                            textureVertexData[1] = (top + h + f)/previewHeight;
                                            textureVertexData[2] = left/previewWidth;
                                            textureVertexData[3] = (top + h + f)/previewHeight;
                                            textureVertexData[4] = right/previewWidth;
                                            textureVertexData[5] = (top + f)/previewHeight;
                                            textureVertexData[6] = left/previewWidth;
                                            textureVertexData[7] = (top + f)/previewHeight;
//                                            textureVertexData[0] = right/previewWidth;
//                                            textureVertexData[1] = bottom/previewHeight;
//                                            textureVertexData[2] = left/previewWidth;
//                                            textureVertexData[3] = bottom/previewHeight;
//                                            textureVertexData[4] = right/previewWidth;
//                                            textureVertexData[5] = top/previewHeight;
//                                            textureVertexData[6] = left/previewWidth;
//                                            textureVertexData[7] = top/previewHeight;
                                            drawRenderer.setVertexData(textureVertexData);
                                            drawNonAffine(left_up_x, left_up_y,
                                                    right_up_x, right_up_y,
                                                    right_down_x, right_down_y,
                                                    left_down_x, left_down_y,
                                                    attributesData, positionData);
                                            imageRenderer.setVertexData(positionData,attributesData);
                                        }
                                        videoRenderer.drawFrame();
                                        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
                                        GLES20.glViewport(rect.left,rect.top,rect.width(),rect.height());
//                                        bitmapRenderer.drawFrame();
                                        mRenderer.drawFrame(videoRenderer.getTexture());
                                        if(isShowImage){
                                            imageRenderer.drawFrame();
                                            GLES20.glViewport(drawRect.left,drawRect.top,drawRect.width(),drawRect.height());
                                            drawRenderer.drawFrame(videoRenderer.getTexture());
                                        }
                                        mEglUtils.swap();
                                    }
                                });
                            }
                        });

                        if(screenWidth != -1){
                            openCamera2();
                        }
                    }
                });
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int w, int h) {
                final int sw = screenWidth;
                screenWidth = w;
                screenHeight = h;
                cameraHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Size mPreviewSize =  getPreferredPreviewSize(mSizes, screenWidth, screenHeight);
                        previewWidth = mPreviewSize.getHeight();
                        previewHeight = mPreviewSize.getWidth();
//                        int previewWidth = mPreviewSize.getWidth();
//                        int previewHeight = mPreviewSize.getHeight();

                        int left, top, viewWidth, viewHeight;
                        float sh = screenWidth * 1.0f / screenHeight;
                        float vh = previewWidth * 1.0f / previewHeight;
                        if (sh < vh) {
                            left = 0;
                            viewWidth = screenWidth;
                            viewHeight = (int) (previewHeight * 1.0f / previewWidth * viewWidth);
                            top = (screenHeight - viewHeight) / 2;
                        } else {
                            top = 0;
                            viewHeight = screenHeight;
                            viewWidth = (int) (previewWidth * 1.0f / previewHeight * viewHeight);
                            left = (screenWidth - viewWidth) / 2;
                        }
                        rect.left = left;
                        rect.top = top;
                        rect.right = left + viewWidth;
                        rect.bottom = top + viewHeight;
                        videoRenderer.setSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
                        imageRenderer.setWorld(previewWidth,previewHeight);
                        if(sw == -1){
                            openCamera2();
                        }
                    }
                });
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                cameraHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(mCameraCaptureSession != null){
                            mCameraCaptureSession.getDevice().close();
                            mCameraCaptureSession.close();
                            mCameraCaptureSession = null;
                        }
                        GLES20.glDisable(GLES20.GL_BLEND);
                        videoRenderer.release();
                        drawRenderer.release();
                        imageRenderer.release();
                        mRenderer.release();
                        mEglUtils.release();
                        isShowImage = false;
                    }
                });
            }
        });

    }

    private Size[] mSizes;
    private void initCamera2() {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mCameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            assert mCameraManager != null;
            String[] CameraIdList = mCameraManager.getCameraIdList();
            mCameraId = CameraIdList[0];
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if(map != null){
                mSizes = map.getOutputSizes(SurfaceTexture.class);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    @SuppressLint("WrongConstant")
    private void openCamera2(){
        if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                mCameraManager.openCamera(mCameraId, stateCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            takePreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {

        }
    };

    private void takePreview() {
        try {
            final CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(videoRenderer.getSurface());
            mCameraDevice.createCaptureSession(Collections.singletonList(videoRenderer.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice) return;
                    mCameraCaptureSession = cameraCaptureSession;
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    CaptureRequest previewRequest = builder.build();
                    try {
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private Size getPreferredPreviewSize(Size[] sizes, int width, int height) {
        List<Size> collectorSizes = new ArrayList<>();
        for (Size option : sizes) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    collectorSizes.add(option);
                }
            } else {
                if (option.getHeight() > width && option.getWidth() > height) {
                    collectorSizes.add(option);
                }
            }
        }
        if (collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size s1, Size s2) {
                    return Long.signum(s1.getWidth() * s1.getHeight() - s2.getWidth() * s2.getHeight());
                }
            });
        }
        return sizes[0];
    }
    public void setSmooth(float smooth){
        drawRenderer.setSmooth(smooth/100.0f);
    }


    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    public void setCoordinate(float[] coordinate,Rect rect) {
        isShowImage = false;
        this.drawRect = rect;
        this.coordinate = coordinate;
    }

    private void drawNonAffine(float bottomLeftX, float bottomLeftY, float bottomRightX, float bottomRightY, float topRightX, float topRightY, float topLeftX, float topLeftY, float[] attributesData, float[] positionData) {
        float ax = topRightX - bottomLeftX;
        float ay = topRightY - bottomLeftY;
        float bx = topLeftX - bottomRightX;
        float by = topLeftY - bottomRightY;

        float cross = ax * by - ay * bx;

        if (cross != 0) {
            float cy = bottomLeftY - bottomRightY;
            float cx = bottomLeftX - bottomRightX;
            float s = (ax * cy - ay * cx) / cross;

            if (s > 0 && s < 1) {
                float t = (bx * cy - by * cx) / cross;
                if (t > 0 && t < 1) {
                    //uv coordinates for texture
                    float u0 = 0; // texture bottom left u
                    float v0 = 0; // texture bottom left v
                    float u2 = 1; // texture top right u
                    float v2 = 1; // texture top right v
                    int bufferIndex = 0;
                    float q0 = 1 / (1 - t);
                    float q1 = 1 / (1 - s);
                    float q2 = 1 / t;
                    float q3 = 1 / s;

//                positionData[bufferIndex++] = bottomLeftX;
//                positionData[bufferIndex++] = bottomLeftY;
//                positionData[bufferIndex++] = bottomRightX;
//                positionData[bufferIndex++] = bottomRightY;
//                positionData[bufferIndex++] = topRightX;
//                positionData[bufferIndex++] = topRightY;
//                positionData[bufferIndex++] = topLeftX;
//                positionData[bufferIndex++] = topLeftY;

                    positionData[bufferIndex++] = bottomRightX;
                    positionData[bufferIndex++] = bottomRightY;
                    positionData[bufferIndex++] = bottomLeftX;
                    positionData[bufferIndex++] = bottomLeftY;

                    positionData[bufferIndex++] = topRightX;
                    positionData[bufferIndex++] = topRightY;
                    positionData[bufferIndex++] = topLeftX;
                    positionData[bufferIndex++] = topLeftY;


                    bufferIndex = 0;
                    attributesData[bufferIndex++] = u2 * q1;
                    attributesData[bufferIndex++] = v2 * q1;
                    attributesData[bufferIndex++] = q1;

                    attributesData[bufferIndex++] = u0 * q0;
                    attributesData[bufferIndex++] = v2 * q0;
                    attributesData[bufferIndex++] = q0;

                    attributesData[bufferIndex++] = u2 * q2;
                    attributesData[bufferIndex++] = v0 * q2;
                    attributesData[bufferIndex++] = q2;

                    attributesData[bufferIndex++] = u0 * q3;
                    attributesData[bufferIndex++] = v0 * q3;
                    attributesData[bufferIndex++] = q3;
                }
            }

        }
    }
}
