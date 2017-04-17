package com.hm.slidedeletelayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Created by dumingwei on 2017/4/14.
 * 侧滑删除布局
 * 参考自张旭童的SwipeMenuLayout
 * 张旭童的github项目地址 https://github.com/mcxtzhang/SwipeDelMenuLayout
 */
public class SlideDeleteLayout extends ViewGroup {

    private static final String TAG = SlideDeleteLayout.class.getSimpleName();

    //用来处理多指滑动的问题
    private static boolean isTouching;
    //用来处理不允许同时有两个item 滑动出菜单
    private static SlideDeleteLayout mViewCache;
    //最小滑动距离
    private int mScaleTouchSlop;
    //计算滑动速度
    private int mMaxVelocity;
    private VelocityTracker mVelocityTracker;
    //侧滑菜单的宽度
    private int mMenuWidth;
    //自己的高度
    private int mHeight;
    //是否开启滑动显示侧滑菜单的功能
    private boolean slideEnable;
    //ios qq 式交互方式，只允许只有一个item处于展开状态
    private boolean iosStyle;
    //标记是左滑还是右滑显示侧滑菜单，默认左滑显示侧滑菜单
    private boolean leftSlide;
    //内容View
    private View mContentView;
    //左滑的临界值
    private int mLimit;
    //标志是否滑动
    private boolean isUserSlide;
    //在onInterceptTouchEvent函数的up时，判断这个变量，如果仍为true 说明是点击事件，则关闭菜单。
    private boolean clickEvent = true;
    private boolean iosInterceptFlag;

    private PointF mFirstP = new PointF();
    private PointF mLastP = new PointF();
    //多指触摸，只取第一个点
    private int mPointerId;

    private ValueAnimator mCloseAnim;
    private ValueAnimator mExpandAnim;

    public SlideDeleteLayout(Context context) {
        this(context, null);
    }

    public SlideDeleteLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideDeleteLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        mScaleTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMaxVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlideDeleteLayout, defStyleAttr, 0);
        slideEnable = ta.getBoolean(R.styleable.SlideDeleteLayout_slideEnable, true);
        iosStyle = ta.getBoolean(R.styleable.SlideDeleteLayout_iosStyle, true);
        leftSlide = ta.getBoolean(R.styleable.SlideDeleteLayout_leftSlide, true);
        ta.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setClickable(true);
        mMenuWidth = 0;
        mHeight = 0;
        int contentWidth = 0;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            childView.setClickable(true);
            if (childView.getVisibility() != GONE) {
                measureChild(childView, widthMeasureSpec, heightMeasureSpec);
                mHeight = Math.max(mHeight, childView.getMeasuredHeight());
                if (i > 0) {
                    //第二个item后面都是滑动菜单
                    mMenuWidth += childView.getMeasuredWidth();
                } else {
                    mContentView = childView;
                    contentWidth = childView.getMeasuredWidth();
                }
            }
        }
        setMeasuredDimension(getPaddingLeft() + contentWidth + getPaddingRight(), mHeight + getPaddingTop() + getPaddingBottom());
        mLimit = mMenuWidth * 4 / 10;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        int left = getPaddingLeft();
        int right = getPaddingLeft();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                if (i == 0) {
                    child.layout(left, getPaddingTop(), left + child.getMeasuredWidth(), getPaddingTop() + child.getMeasuredHeight());
                    left = left + child.getMeasuredWidth();
                } else {
                    if (leftSlide) {
                        child.layout(left, getPaddingTop(), left + child.getMeasuredWidth(), getPaddingTop() + child.getMeasuredHeight());
                        left = left + child.getMeasuredWidth();
                    } else {
                        child.layout(right - child.getMeasuredWidth(), getPaddingTop(), right, getPaddingTop() + child.getMeasuredHeight());
                        right = right - child.getMeasuredWidth();
                    }
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (slideEnable) {
            if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(ev.getRawX() - mFirstP.x) > mScaleTouchSlop) {
                    Log.e(TAG, "onInterceptTouchEvent: action move return true ");
                    return true;
                }
            } else if (ev.getAction() == MotionEvent.ACTION_UP) {
                if (leftSlide) {
                    //左滑显示删除菜单的情况
                    if (getScrollX() > mScaleTouchSlop) {
                        //这里判断落点在内容区域,拦截事件，关闭删除菜单。如果落点在菜单区域，不拦截事件
                        if (ev.getX() < getWidth() - getScrollX()) {
                            if (clickEvent) {
                                smoothClose();
                            }
                            return true;
                        }
                    }
                } else {
                    //右滑显示删除菜单的情况
                    if (-getScrollX() > mScaleTouchSlop) {
                        if (ev.getX() > -getScrollX()) {
                            if (clickEvent) {
                                smoothClose();
                            }
                            return true;
                        }
                    }
                }
                if (isUserSlide) {
                    return true;
                }
            }
            //模仿IOS 点击其他区域关闭
            if (iosInterceptFlag) {
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (slideEnable) {
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(ev);
            VelocityTracker velocityTracker = mVelocityTracker;
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isUserSlide = false;
                    clickEvent = true;
                    iosInterceptFlag = false;
                    if (isTouching) {
                        //如果有别的指头摸过了，那么就return false。
                        return false;
                    } else {
                        //第一个摸的指头，改变标志位
                        isTouching = true;
                    }
                    mFirstP.set(ev.getRawX(), ev.getRawY());
                    mLastP.set(ev.getRawX(), ev.getRawY());
                    if (mViewCache != null) {
                        if (mViewCache != this) {
                            mViewCache.smoothClose();
                            iosInterceptFlag = iosStyle;
                        }
                        //不让父控件拦截事件
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    mPointerId = ev.getPointerId(0);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (iosInterceptFlag) {
                        break;
                    }
                    float gap = mLastP.x - ev.getRawX();
                    if (Math.abs(gap) > 10 || Math.abs(getScrollX()) > 10) {
                        //不让父控件拦截事件
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (Math.abs(gap) > mScaleTouchSlop) {
                        clickEvent = false;
                    }
                    Log.e(TAG, "dispatchTouchEvent: gap=" + gap);
                    scrollBy((int) gap, 0);
                    //越界修正
                    if (leftSlide) {
                        //左滑
                        if (getScrollX() < 0) {
                            scrollTo(0, 0);
                        }
                        if (getScrollX() > mMenuWidth) {
                            scrollTo(mMenuWidth, 0);
                        }
                    } else {//右滑
                        if (getScrollX() < -mMenuWidth) {
                            scrollTo(-mMenuWidth, 0);
                        }
                        if (getScrollX() > 0) {
                            scrollTo(0, 0);
                        }
                    }
                    mLastP.set(ev.getRawX(), ev.getRawY());
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    //属于滑动了
                    if (Math.abs(ev.getRawX() - mFirstP.x) > mScaleTouchSlop) {
                        isUserSlide = true;
                    }
                    if (!iosInterceptFlag) {
                        velocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
                        float velocityX = velocityTracker.getXVelocity(mPointerId);
                        if (Math.abs(velocityX) > 1000) {
                            if (velocityX < -1000) {
                                if (leftSlide) {
                                    //左滑
                                    smoothExpand();
                                } else {
                                    smoothClose();
                                }
                            } else {
                                if (leftSlide) {
                                    //左滑
                                    smoothClose();
                                } else {
                                    smoothExpand();
                                }
                            }
                        } else {
                            if (Math.abs(getScrollX()) > mLimit) {
                                smoothExpand();
                            } else {
                                smoothClose();
                            }
                        }
                    }
                    releaseVelocityTracker();
                    isTouching = false;
                    break;
                default:
                    break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }


    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void smoothExpand() {
        Log.e(TAG, "smoothExpand: " + this.toString());
        mViewCache = SlideDeleteLayout.this;
        if (mContentView != null) {
            mContentView.setLongClickable(false);
        }
        cancelAnim();
        mExpandAnim = ValueAnimator.ofInt(getScrollX(), leftSlide ? mMenuWidth : -mMenuWidth);
        mExpandAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                scrollTo((Integer) animation.getAnimatedValue(), 0);
            }
        });
        mExpandAnim.setInterpolator(new OvershootInterpolator());
        mExpandAnim.setDuration(300).start();

    }

    private void smoothClose() {
        Log.e(TAG, "smoothClose: " + this.toString());
        mViewCache = null;
        if (mContentView != null) {
            mContentView.setLongClickable(true);
        }
        cancelAnim();
        mCloseAnim = ValueAnimator.ofInt(getScrollX(), 0);
        mCloseAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                scrollTo((Integer) animation.getAnimatedValue(), 0);
            }
        });
        mCloseAnim.setInterpolator(new AccelerateInterpolator());
        mCloseAnim.setDuration(300).start();

    }

    @Override
    protected void onDetachedFromWindow() {
        if (this == mViewCache) {
            mViewCache.smoothClose();
            mViewCache = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean performLongClick() {
        if (Math.abs(getScrollX()) > mScaleTouchSlop) {
            return false;
        }
        return super.performLongClick();
    }

    private void cancelAnim() {
        if (mCloseAnim != null && mCloseAnim.isRunning()) {
            mCloseAnim.cancel();
        }
        if (mExpandAnim != null && mExpandAnim.isRunning()) {
            mExpandAnim.cancel();
        }
    }
}
