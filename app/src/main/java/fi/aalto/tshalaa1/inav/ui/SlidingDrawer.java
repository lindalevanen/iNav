package fi.aalto.tshalaa1.inav.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import fi.aalto.tshalaa1.inav.R;
import fi.aalto.tshalaa1.inav.Utils;

public class SlidingDrawer extends LinearLayout {

    private int alwaysVisibleHeight = -1;
    private int handleHeight = -1;
    private boolean mOpen = false;
    private View mHandle;

    private static int PADDING;

    public SlidingDrawer(Context context) {
        super(context);
        init(null, 0);
    }

    public SlidingDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public SlidingDrawer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        setOrientation(VERTICAL);
//        PADDING = (int)Utils.dpToPx(getContext(), 5);

    }

//    private static final int IV_ID = 0xf8000001;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mHandle == null) {
            setHandle(getChildAt(0));
            setY(((View) getParent()).getHeight() - PADDING - getHandleHeight());
        } else {
            position(true);
        }
    }

    private void position(boolean animate) {
        float newY = 0;
        if (mOpen) {
            newY = ((View) getParent()).getHeight() - getMeasuredHeight()- PADDING;
        } else {
            newY = ((View) getParent()).getHeight() - PADDING - getHandleHeight();
        }
        if (animate) {
            Animator anim = ObjectAnimator.ofFloat(this, "Y", getY(), newY);
            anim.setDuration(200);
            anim.start();
        } else {
            setY(newY);
        }
    }

    public void setHandle(View handle) {
        handleHeight = handle.getMeasuredHeight();
        mHandle = handle;
        handle.setOnTouchListener(handleTouchListener);
        handle.setOnClickListener(handleClickListener);
    }

    public void setAlwaysVisibleHeight(int widthInPx) {
        if (mHandle != null) {
            alwaysVisibleHeight = mHandle.getMeasuredHeight() + widthInPx;
        }
    }

    private void animateOpen() {
        System.out.println("open");
        int currY = ((View)getParent()).getHeight() - getMeasuredHeight() - PADDING;
        int dist = getMeasuredHeight() - getHandleHeight();
        Animator anim = ObjectAnimator.ofFloat(this, "Y", currY + dist, currY);
        anim.setDuration(500);
        anim.start();
    }

    private void animateClose() {
        System.out.println("close");
        int currY = ((View)getParent()).getHeight() - getMeasuredHeight() - PADDING;
        int dist = getMeasuredHeight() - getHandleHeight();
        Animator anim = ObjectAnimator.ofFloat(this, "Y", currY, currY + dist);
        anim.setDuration(500);
        anim.start();
    }

    private int getHandleHeight() {
        if (alwaysVisibleHeight != -1) {
            return alwaysVisibleHeight;
        }
        if (handleHeight > 0) {
            return handleHeight;
        }
        handleHeight = mHandle.getHeight();
        return handleHeight;
    }

    private OnClickListener handleClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mOpen) {
                animateClose();
            } else {
                animateOpen();
            }
            mOpen = !mOpen;
        }
    };

    private OnTouchListener handleTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {

            }
            return false;
        }
    };

    public void setOpen(boolean open) {
        mOpen = true;
        position(false);
    }

    public void open() {
        if (!mOpen) {
            animateOpen();
        }
        mOpen = true;
    }

    public void close() {
        if (mOpen) {
            animateClose();
        }
        mOpen = false;
    }
}
