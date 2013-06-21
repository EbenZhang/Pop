package com.ezhang.pop.ui;

import android.view.MotionEvent;

class GestureListener implements android.view.GestureDetector.OnGestureListener {
    private static final int SWIPE_MAX_OFF_PATH = 100;

    private static final int SWIPE_MIN_DISTANCE = 100;

    private static final int SWIPE_THRESHOLD_VELOCITY = 100;

    private final IGestureHandler m_handler;

    public GestureListener(IGestureHandler handler) {
        m_handler = handler;
    }

    // 用户轻触触摸屏，由1个MotionEvent ACTION_DOWN触发
    public boolean onDown(MotionEvent e) {
        return false;
    }

    // 用户按下触摸屏、快速移动后松开,由1个MotionEvent ACTION_DOWN,
    // 多个ACTION_MOVE, 1个ACTION_UP触发
    // e1：第1个ACTION_DOWN MotionEvent
    // e2：最后一个ACTION_MOVE MotionEvent
    // velocityX：X轴上的移动速度，像素/秒
    // velocityY：Y轴上的移动速度，像素/秒
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
            return false;

        if ((e1.getX() - e2.getX()) > SWIPE_MIN_DISTANCE
                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
            m_handler.GestureToLeft();
        } else if ((e2.getX() - e1.getX()) > SWIPE_MIN_DISTANCE
                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
            m_handler.GestureToRight();
        }
        return true;
    }

    // 用户长按触摸屏，由多个MotionEvent ACTION_DOWN触发
    public void onLongPress(MotionEvent e) {
    }

    // 用户按下触摸屏，并拖动，由1个MotionEvent ACTION_DOWN, 多个ACTION_MOVE触发
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        return false;
    }

    public void onShowPress(MotionEvent e) {
    }

    // 用户（轻触触摸屏后）松开，由一个1个MotionEvent ACTION_UP触发
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }
}
