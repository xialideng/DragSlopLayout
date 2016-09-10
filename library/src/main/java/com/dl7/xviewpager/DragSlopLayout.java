package com.dl7.xviewpager;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IntDef;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.ScrollerCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.dl7.xviewpager.animate.AnimatorPresenter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by long on 2016/9/6.
 */
public class DragSlopLayout extends FrameLayout {

    public static final int MODE_DRAG = 1;
    public static final int MODE_FIX_ALWAYS = 2;
    public static final int MODE_ANIMATE = 3;

    // 展开
    static final int STATUS_EXPANDED = 101;
    // 收缩
    static final int STATUS_COLLAPSED = 102;
    // 退出
    static final int STATUS_EXIT = 103;
    // 滚动
    static final int STATUS_SCROLL = 104;

    // ViewDragHelper 的敏感度
    private static final float TOUCH_SLOP_SENSITIVITY = 1.f;
    // 判断快速滑动的速率
    private static final float FLING_VELOCITY = 2000;
    // 下坠动画时间
    private static final int FALL_BOUND_DURATION = 1000;

    // 模式
    private int mMode;
    // 固定高度
    private int mFixHeight;
    // 整个布局高度
    private int mHeight;
    // 拖拽模式的临界Top值
    private int mCriticalTop;
    // 拖拽模式的展开状态Top值
    private int mExpandedTop;
    // 拖拽模式的收缩状态Top值
    private int mCollapsedTop;
    // 是否处于拖拽状态
    private boolean mIsDrag = false;
    // 拖拽状态
    @DragStatus
    private int mDragStatus = STATUS_EXPANDED;
    // 是否有 post 显示动画 Runnable
    private boolean mHasShowRunnable = false;

    private Context mContext;
    // 布局的第1个子视图
    private View mMainView;
    // 可拖拽的视图，为布局的第2个子视图
    private View mDragView;
    // 拖拽帮助类
    private ViewDragHelper mDragHelper;
    // 滚动辅助类
    private ScrollerCompat mScroller;
    // ViewPager 监听器
    private ViewPager.OnPageChangeListener mViewPagerListener;
    // 动画持有者
    private AnimatorPresenter mAnimPresenter;
    // 和 ViewPager 联动时的自动执行进入动画的延迟时间，注意和动画的启动延迟时间区分
    private int mAutoAnimateDelay = 1000;
    // 是否手动执行了退出动画，如果为真则不再执行自动进入动画
    private boolean mIsDoOutAnim = false;


    public DragSlopLayout(Context context) {
        this(context, null);
    }

    public DragSlopLayout(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public DragSlopLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        _init(context, attrs);
    }

    private void _init(Context context, AttributeSet attrs) {
        mContext = context;
        mDragHelper = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, callback);
        mScroller = ScrollerCompat.create(context, new BounceInterpolator());

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DragSlopLayout, 0, 0);
        mFixHeight = a.getDimensionPixelOffset(R.styleable.DragSlopLayout_fix_height, mFixHeight);
        mMode = a.getInt(R.styleable.DragSlopLayout_mode, MODE_DRAG);
        a.recycle();
        if (mMode == MODE_DRAG) {
            mDragStatus = STATUS_COLLAPSED;
        }
        mAnimPresenter = new AnimatorPresenter();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int childCount = getChildCount();
        if (childCount != 2) {
            // DragLayout 只能且必须包含两个子视图，第一个为主视图，另一个为可拖拽的视图
            throw new IllegalArgumentException("DragLayout must contains two sub-views.");
        }
        mMainView = getChildAt(0);
        mDragView = getChildAt(1);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = h;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int childCount = getChildCount();
        if (childCount != 2) {
            // DragLayout 只能且必须包含两个子视图，第一个为主视图，另一个为可拖拽的视图
            throw new IllegalArgumentException("DragLayout must contains two sub-views.");
        }

        MarginLayoutParams lp;
        View mainView = getChildAt(0);
        lp = (MarginLayoutParams) mainView.getLayoutParams();
        int width = mainView.getMeasuredWidth();
        int height = mainView.getMeasuredHeight();
        mainView.layout(lp.leftMargin, lp.topMargin, lp.leftMargin + width, lp.topMargin + height);

        View childView = getChildAt(1);
        lp = (MarginLayoutParams) childView.getLayoutParams();
        int childWidth = childView.getMeasuredWidth();
        int childHeight = childView.getMeasuredHeight();
        if (mMode != MODE_DRAG) {
            // 非拖拽模式固定高度为子视图高度
            mFixHeight = childHeight;
        }
        int childTop;
        if (mDragStatus == STATUS_EXIT) {
            // 对于 ViewPager 换页后会回调 onLayout()，需要进行处理
            if (mMode == MODE_DRAG) {
                childTop = b;
            } else {
                childTop = b - mFixHeight;
                childView.setTranslationY(childHeight);
            }
        } else if (mDragStatus == STATUS_COLLAPSED) {
            childTop = b - mFixHeight;
        } else if (mDragStatus == STATUS_EXPANDED) {
            childTop = b - childHeight;
        } else {
            childTop = b - mFixHeight;
        }
        childView.layout(lp.leftMargin, childTop, lp.leftMargin + childWidth, childTop + childHeight);

//            mCriticalTop = mHeight - (mDragView.getHeight() - mFixHeight) / 2 - mFixHeight;
        mCriticalTop = b - childHeight / 2;
        mExpandedTop = b - childHeight;
        mCollapsedTop = b - mFixHeight;
    }

    /***********************************
     * ViewDragHelper
     ********************************************/

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean isIntercept = mDragHelper.shouldInterceptTouchEvent(ev);
        if (mDragStatus == STATUS_EXPANDED && mMode == MODE_DRAG) {
            isIntercept = true;
        } else if (mDragHelper.isViewUnder(mDragView, (int) ev.getX(), (int) ev.getY()) && mMode == MODE_DRAG) {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            isIntercept = true;
        }
        if (mDragStatus == STATUS_EXIT) {
            getHandler().removeCallbacks(mShowRunnable);
        }
        return isIntercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        return mIsDrag;
    }

    private ViewDragHelper.Callback callback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            requestDisallowInterceptTouchEvent(true);
            mIsDrag = child == mDragView;
            return mIsDrag;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if (Math.abs(yvel) < FLING_VELOCITY) {
                if (mDragView.getTop() > mCriticalTop) {
                    if (mDragStatus == STATUS_EXPANDED) {
                        mDragHelper.smoothSlideViewTo(mDragView, 0, mCollapsedTop);
                    } else {
                        mScroller.startScroll(0, mDragView.getTop(), 0, mCollapsedTop - mDragView.getTop(),
                                FALL_BOUND_DURATION);
                    }
                    ViewCompat.postInvalidateOnAnimation(DragSlopLayout.this);
                } else {
                    mDragHelper.smoothSlideViewTo(mDragView, 0, mExpandedTop);
                    ViewCompat.postInvalidateOnAnimation(DragSlopLayout.this);
                }
            } else if (yvel > 0) {
                mDragHelper.settleCapturedViewAt(0, mCollapsedTop);
                ViewCompat.postInvalidateOnAnimation(DragSlopLayout.this);
            } else {
                mDragHelper.settleCapturedViewAt(0, mExpandedTop);
                ViewCompat.postInvalidateOnAnimation(DragSlopLayout.this);
            }
            requestDisallowInterceptTouchEvent(false);
        }

        /**
         * 检测 ViewDrag 状态变化，eg：
         * STATE_IDLE       闲置、停止
         * STATE_DRAGGING   拖拽中
         * STATE_SETTLING   滚动状态(描述不是很确切)，比如拖拽放手后在onViewReleased()调用smoothSlideViewTo()控制View移动的时候
         * @param state
         */
        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (state == ViewDragHelper.STATE_IDLE) {
                _switchStatus();
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            super.onViewCaptured(capturedChild, activePointerId);
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            int newTop = Math.max(mExpandedTop, top);
            newTop = Math.min(mCollapsedTop, newTop);
            return newTop;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return 0;
        }
    };

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true) || _continueSettling()) {
            mDragStatus = STATUS_SCROLL;
            ViewCompat.postInvalidateOnAnimation(this);
        }
        super.computeScroll();
    }

    /**
     * 处理自定义滚动动画
     */
    private boolean _continueSettling() {
        boolean keepGoing = mScroller.computeScrollOffset();
        if (!keepGoing) {
            return false;
        }
        final int x = mScroller.getCurrX();
        final int y = mScroller.getCurrY();
        final int dx = x - mDragView.getLeft();
        final int dy = y - mDragView.getTop();
        if (dx != 0) {
            ViewCompat.offsetLeftAndRight(mDragView, dx);
        }
        if (dy != 0) {
            ViewCompat.offsetTopAndBottom(mDragView, dy);
        }
        if (keepGoing && x == mScroller.getFinalX() && y == mScroller.getFinalY()) {
            // Close enough. The interpolator/scroller might think we're still moving
            // but the user sure doesn't.
            mScroller.abortAnimation();
            keepGoing = false;
            ViewCompat.postInvalidateOnAnimation(this);
            _switchStatus();
        }
        return keepGoing;
    }

    /*********************************** Inside ********************************************/

    /**
     * 切换状态
     */
    private void _switchStatus() {
        if (mDragView.getTop() == mExpandedTop) {
            mDragStatus = STATUS_EXPANDED;
        } else if (mDragView.getTop() == mCollapsedTop) {
            mDragStatus = STATUS_COLLAPSED;
        } else if (mDragView.getTop() == mHeight) {
            mDragStatus = STATUS_EXIT;
        } else {
            mDragStatus = STATUS_SCROLL;
        }
    }

    /**
     * 判断视图是否为 ViewPager 或它的子类
     *
     * @param view View
     * @return
     */
    private boolean _isViewPager(View view) {
        boolean isViewPager = false;
        if (view instanceof ViewPager) {
            isViewPager = true;
        } else {
            ViewParent parent = view.getParent();
            while (parent != null) {
                if (parent instanceof ViewPager) {
                    isViewPager = true;
                    break;
                }
            }
        }
        return isViewPager;
    }

    /**
     * 隐藏 DragView
     * @param percent   ViewPager 滑动百分比
     */
    private void _hideDragView(float percent) {
        if (!mHasShowRunnable && !mIsDoOutAnim) {
            float hidePercent = percent * 3;
            if (hidePercent > 1.0f) {
                hidePercent = 1.0f;
                mDragStatus = STATUS_EXIT;
            } else {
                mDragStatus = STATUS_SCROLL;
            }

            if (mMode == MODE_DRAG) {
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                final int y = (int) (mFixHeight * hidePercent + mCollapsedTop);
                final int dy = y - mDragView.getTop();
                if (dy != 0) {
                    ViewCompat.offsetTopAndBottom(mDragView, dy);
                }
            } else if (mMode == MODE_ANIMATE) {
                mAnimPresenter.handleAnimateFrame(mDragView, hidePercent);
            }
        }
    }

    /**
     * 显示 DragView
     * @param delay 延迟时间
     */
    private void _showDragView(int delay) {
        mHasShowRunnable = true;
        postDelayed(mShowRunnable, delay);
    }

    /**
     * 动画显示 Runnable
     */
    private Runnable mShowRunnable = new Runnable() {
        @Override
        public void run() {
            mHasShowRunnable = false;
            if (mMode == MODE_DRAG) {
                mScroller.startScroll(0, mDragView.getTop(), 0, mCollapsedTop - mDragView.getTop(), 500);
                ViewCompat.postInvalidateOnAnimation(DragSlopLayout.this);
            } else if (mMode == MODE_ANIMATE) {
                startInAnim();
            }
        }
    };

    /*********************************** ViewPager ********************************************/

    /**
     * 和 ViewPager 进行联动，注意第1个子视图必须为 ViewPager 或它的子类
     *
     * @param isInteract 是否联动
     */
    public void interactWithViewPager(boolean isInteract) {
        if (!_isViewPager(mMainView)) {
            throw new IllegalArgumentException("The first child view must be ViewPager.");
        }
        if (mViewPagerListener != null) {
            ((ViewPager) mMainView).removeOnPageChangeListener(mViewPagerListener);
        }
        if (!isInteract) {
            mViewPagerListener = null;
            return;
        }
        mViewPagerListener = new ViewPager.SimpleOnPageChangeListener() {

            boolean isRightSlide = true;
            float mLastOffset = 0;
            int status = ViewPager.SCROLL_STATE_IDLE;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (status != ViewPager.SCROLL_STATE_IDLE) {
                    // 判断拖拽过界的方向
                    if (Math.abs(positionOffset - mLastOffset) > 0.8f &&
                            status == ViewPager.SCROLL_STATE_DRAGGING) {
                        if (positionOffset > 0.5f) {
                            isRightSlide = false;
                        } else {
                            isRightSlide = true;
                        }
                    }
                    float percent;
                    if (isRightSlide) {
                        percent = positionOffset;
                        if (positionOffset == 0 && status == ViewPager.SCROLL_STATE_SETTLING && mLastOffset > 0.5f) {
                            percent = 1.0f;
                        }
                    } else {
                        percent = 1 - positionOffset;
                    }
                    _hideDragView(percent);
                    mLastOffset = positionOffset;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    isRightSlide = true;
                    mLastOffset = 0;
                    if (mDragStatus == STATUS_EXIT && !mIsDoOutAnim) {
                        _showDragView(mAutoAnimateDelay);
                    }
                }
                status = state;
            }
        };
        ((ViewPager) mMainView).addOnPageChangeListener(mViewPagerListener);
    }

    public int getAutoAnimateDelay() {
        return mAutoAnimateDelay;
    }

    public void setAutoAnimateDelay(int autoAnimateDelay) {
        mAutoAnimateDelay = autoAnimateDelay;
    }

    /*********************************** Animation ********************************************/

    public static final int SLIDE_BOTTOM = 101;
    public static final int SLIDE_LEFT = 102;
    public static final int SLIDE_RIGHT = 103;
    public static final int FADE = 104;
    public static final int FLIP_X = 105;
    public static final int FLIP_Y = 106;
    public static final int ZOOM = 107;
    public static final int ZOOM_LEFT = 108;
    public static final int ZOOM_RIGHT = 109;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SLIDE_BOTTOM, SLIDE_LEFT, SLIDE_RIGHT, FADE, FLIP_X, FLIP_Y, ZOOM, ZOOM_LEFT, ZOOM_RIGHT})
    public @interface AnimatorMode {
    }

    public int getAnimatorMode() {
        return mAnimPresenter.getAnimatorMode();
    }

    public void setAnimatorMode(@AnimatorMode int animatorMode) {
        mAnimPresenter.setAnimatorMode(animatorMode);
    }

    public int getStartDelay() {
        return mAnimPresenter.getStartDelay();
    }

    public void setStartDelay(int startDelay) {
        mAnimPresenter.setStartDelay(startDelay);
    }

    public int getDuration() {
        return mAnimPresenter.getDuration();
    }

    public void setDuration(int duration) {
        mAnimPresenter.setDuration(duration);
    }

    public Interpolator getInterpolator() {
        return mAnimPresenter.getInterpolator();
    }

    public void setInterpolator(Interpolator interpolator) {
        mAnimPresenter.setInterpolator(interpolator);
    }

    public void startInAnim() {
        mIsDoOutAnim = false;
        mAnimPresenter.startInAnim(mDragView);
    }

    public void startOutAnim() {
        mIsDoOutAnim = true;
        mAnimPresenter.startOutAnim(mDragView);
    }

    /***********************************
     * interface
     ********************************************/


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_EXPANDED, STATUS_COLLAPSED, STATUS_EXIT, STATUS_SCROLL})
    @interface DragStatus {
    }

}
