package com.hyq.hm.test.greenscreen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PointView extends View {

    private Paint lPaint = new Paint();
    private Paint cPaint = new Paint();
    private Path path = new Path();

    public PointView(Context context) {
        super(context);
        init();
    }

    public PointView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        lPaint.setAntiAlias(true);
        lPaint.setStrokeWidth(5);
        lPaint.setColor(Color.WHITE);
        lPaint.setStyle(Paint.Style.STROKE);
        cPaint.setAntiAlias(true);
        cPaint.setColor(Color.RED);
        setOnTouchListener(new OnTouchListener() {
            private Point movePoint = null;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                int x = (int) event.getX();
                int y = (int) event.getY();
                if (action == MotionEvent.ACTION_DOWN) {
                    movePoint = null;
                    for (Point p : points) {
                        if (Math.abs(x - p.x) < 50) {
                            if (Math.abs(y - p.y) < 50) {
                                movePoint = p;
                                break;
                            }
                        }
                    }
                    return null != movePoint;
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if(movePoint == null){
                        return false;
                    }
                    if(x < maxRect.left){
                        x = maxRect.left;
                    }else if(x > maxRect.right){
                        x = maxRect.right;
                    }
                    if(y < maxRect.top){
                        y = maxRect.top;
                    }else if(y > maxRect.bottom){
                        y = maxRect.bottom;
                    }
                    List<Point> pointList = new ArrayList<>();
                    for (Point p : points) {
                        if(p != movePoint){
                            pointList.add(p);
                        }else{
                            pointList.add(new Point(x,y));
                        }
                    }
                    if(pointList.size() == 4){
                        if(isNonAffine(pointList.get(1).x,pointList.get(1).y,
                                pointList.get(3).x,pointList.get(3).y,
                                pointList.get(2).x,pointList.get(2).y,
                                pointList.get(0).x,pointList.get(0).y)){
                            movePoint.x = x;
                            movePoint.y = y;
                            invalidate();
                            return true;
                        }
                    }
                }else return action == MotionEvent.ACTION_UP;
                return false;
            }
        });
    }
    public float[] getCoordinate(int width,int height) {
        float vw = width*1.0f/maxRect.width();
        float vh = height*1.0f/maxRect.height();
        float[] coordinate = new float[8];
        coordinate[0] = vw*(points[0].x - maxRect.left);
        coordinate[1] = height - vh*(points[0].y - maxRect.top);
        coordinate[2] = vw*(points[2].x - maxRect.left);
        coordinate[3] = height - vh*(points[2].y - maxRect.top);
        coordinate[4] = vw*(points[1].x - maxRect.left);
        coordinate[5] = height - vh*(points[1].y - maxRect.top);
        coordinate[6] = vw*(points[3].x - maxRect.left);
        coordinate[7] = height - vh*(points[3].y - maxRect.top);
        return coordinate;
    }

    private Point[] points = null;
    private Rect maxRect = null;
    public void setRect(Rect rect) {
        if (points == null) {
            points = new Point[4];
            for (int i = 0; i < points.length; i++) {
                points[i] = new Point();
            }
        }
        maxRect = new Rect(rect);
        points[0].x = rect.left;
        points[0].y = rect.top;
        points[1].x = rect.left;
        points[1].y = rect.bottom;
        points[2].x = rect.right;
        points[2].y = rect.top;
        points[3].x = rect.right;
        points[3].y = rect.bottom;
        invalidate();
    }
    public Rect getRect(){
        int height = getHeight();
        int left = points[0].x;
        int top = height - points[0].y;
        int right = points[0].x;
        int bottom = height - points[0].y;
        for (Point p :points){
            left = Math.min(left,p.x);
            top = Math.min(top,height - p.y);
            right = Math.max(right,p.x);
            bottom = Math.max(bottom,height - p.y);
        }
        return new Rect(left,top,right,bottom);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0 || points == null) {
            return;
        }
//        if(rightBottomPoint.x == 0){
//            leftTopPoint.x = 0;
//            leftTopPoint.y = 0;
//            leftBottomPoint.x = 0;
//            leftBottomPoint.y = height;
//            rightTopPoint.x = width;
//            rightTopPoint.y = 0;
//            rightBottomPoint.x = width;
//            rightBottomPoint.y = height;
//        }
        path.reset();
        path.moveTo(points[0].x, points[0].y);
        path.lineTo(points[2].x, points[2].y);
        path.lineTo(points[3].x, points[3].y);
        path.lineTo(points[1].x, points[1].y);
        path.lineTo(points[0].x, points[0].y);
        canvas.drawPath(path, lPaint);

        for (Point p : points){
            canvas.drawCircle(p.x, p.y, 10, cPaint);
        }

    }
    private boolean isNonAffine(float bottomLeftX, float bottomLeftY, float bottomRightX, float bottomRightY, float topRightX, float topRightY, float topLeftX, float topLeftY) {
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
                return t > 0 && t < 1;
            }

        }
        return false;
    }
}
