package com.dl7.xviewpager.animate;

import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.dl7.xviewpager.DragSlopLayout;
import com.dl7.xviewpager.DragSlopLayout.AnimatorMode;
import com.dl7.xviewpager.animate.in.FadeInAnimator;
import com.dl7.xviewpager.animate.in.FlipInXAnimator;
import com.dl7.xviewpager.animate.in.FlipInYAnimator;
import com.dl7.xviewpager.animate.in.SlideInBottomAnimator;
import com.dl7.xviewpager.animate.in.SlideInLeftAnimator;
import com.dl7.xviewpager.animate.in.SlideInRightAnimator;
import com.dl7.xviewpager.animate.in.ZoomInAnimator;
import com.dl7.xviewpager.animate.in.ZoomInLeftAnimator;
import com.dl7.xviewpager.animate.in.ZoomInRightAnimator;
import com.dl7.xviewpager.animate.out.FadeOutAnimator;
import com.dl7.xviewpager.animate.out.FlipOutXAnimator;
import com.dl7.xviewpager.animate.out.FlipOutYAnimator;
import com.dl7.xviewpager.animate.out.SlideOutBottomAnimator;
import com.dl7.xviewpager.animate.out.SlideOutLeftAnimator;
import com.dl7.xviewpager.animate.out.SlideOutRightAnimator;
import com.dl7.xviewpager.animate.out.ZoomOutAnimator;
import com.dl7.xviewpager.animate.out.ZoomOutLeftAnimator;
import com.dl7.xviewpager.animate.out.ZoomOutRightAnimator;

/**
 * Created by long on 2016/9/9.
 * 动画持有者
 */
public final class AnimatorPresenter {

    private BaseViewAnimator mInAnimator;
    private BaseViewAnimator mOutAnimator;
    @AnimatorMode
    private int mAnimatorMode;
    private int mStartDelay;
    private int mDuration;
    private Interpolator mInterpolator;

    public AnimatorPresenter() {
        mAnimatorMode = DragSlopLayout.SLIDE_BOTTOM;
        mInAnimator = new SlideInBottomAnimator();
        mOutAnimator = new SlideOutBottomAnimator();
        mDuration = 400;
        mInterpolator = new LinearInterpolator();
    }

    /**
     * 设置动画
     * @param animatorMode
     */
    public void setAnimatorMode(@AnimatorMode int animatorMode) {
        mAnimatorMode = animatorMode;
        switch (animatorMode) {
            case DragSlopLayout.SLIDE_BOTTOM:
                mInAnimator = new SlideInBottomAnimator();
                mOutAnimator = new SlideOutBottomAnimator();
                break;
            case DragSlopLayout.SLIDE_LEFT:
                mInAnimator = new SlideInLeftAnimator();
                mOutAnimator = new SlideOutLeftAnimator();
                break;
            case DragSlopLayout.SLIDE_RIGHT:
                mInAnimator = new SlideInRightAnimator();
                mOutAnimator = new SlideOutRightAnimator();
                break;
            case DragSlopLayout.FADE:
                mInAnimator = new FadeInAnimator();
                mOutAnimator = new FadeOutAnimator();
                break;
            case DragSlopLayout.FLIP_X:
                mInAnimator = new FlipInXAnimator();
                mOutAnimator = new FlipOutXAnimator();
                break;
            case DragSlopLayout.FLIP_Y:
                mInAnimator = new FlipInYAnimator();
                mOutAnimator = new FlipOutYAnimator();
                break;
            case DragSlopLayout.ZOOM:
                mInAnimator = new ZoomInAnimator();
                mOutAnimator = new ZoomOutAnimator();
                break;
            case DragSlopLayout.ZOOM_LEFT:
                mInAnimator = new ZoomInLeftAnimator();
                mOutAnimator = new ZoomOutLeftAnimator();
                break;
            case DragSlopLayout.ZOOM_RIGHT:
                mInAnimator = new ZoomInRightAnimator();
                mOutAnimator = new ZoomOutRightAnimator();
                break;
        }
    }

    @AnimatorMode
    public int getAnimatorMode() {
        return mAnimatorMode;
    }

    /**
     * 启动进入动画
     * @param target 目标View
     */
    public void startInAnim(View target) {
        mInAnimator.setTarget(target)
                .setStartDelay(mStartDelay)
                .setDuration(mDuration)
                .setInterpolator(mInterpolator)
                .start();
    }

    /**
     * 启动退出动画
     * @param target 目标View
     */
    public void startOutAnim(View target) {
        mOutAnimator.setTarget(target)
                .setStartDelay(mStartDelay)
                .setDuration(mDuration)
                .setInterpolator(mInterpolator)
                .start();
    }

    /**
     * 处理动画帧
     * @param percent 百分比
     */
    public void handleAnimateFrame(View target, float percent) {
        ViewGroup parent = (ViewGroup) target.getParent();
        float alpha = 1.0f * (1 - percent);
        float translationX = 0;
        float translationY = 0;
        float rotationX = 0;
        float rotationY = 0;
        float scale = 0;

        switch (mAnimatorMode) {
            case DragSlopLayout.SLIDE_BOTTOM:
                translationY = target.getHeight() * percent;
                break;

            case DragSlopLayout.SLIDE_LEFT:
                translationX = - parent.getWidth() * percent;
                break;

            case DragSlopLayout.SLIDE_RIGHT:
                translationX = parent.getWidth() * percent;
                break;

            case DragSlopLayout.FADE:
                break;

            case DragSlopLayout.FLIP_X:
                rotationX = 90 * percent;
                break;

            case DragSlopLayout.FLIP_Y:
                rotationY = 90 * percent;
                break;

            case DragSlopLayout.ZOOM:
                scale = 0.3f + 0.7f * (1 - percent);
                break;

            case DragSlopLayout.ZOOM_LEFT:
                alpha = Math.min(1.0f, alpha * 2);
                final float valueL = parent.getWidth() * 0.2f;
                if (percent < 0.5f) {
                    scale = 1 - percent;
                    translationX = percent * 2 * valueL;
                } else {
                    scale = (1 - percent) * 0.8f + 0.1f;
                    translationX = valueL - (percent * 2 - 1.0f) * parent.getWidth() * 1.2f;
                }
                break;
            case DragSlopLayout.ZOOM_RIGHT:
                alpha = Math.min(1.0f, alpha * 2);
                final float valueR = -parent.getWidth() * 0.2f;
                if (percent < 0.5f) {
                    scale = 1 - percent;
                    translationX = percent * 2 * valueR;
                } else {
                    scale = (1 - percent) * 0.8f + 0.1f;
                    translationX = valueR + (percent * 2 - 1.0f) * parent.getWidth() * 1.2f;
                }
                mInAnimator = new ZoomInRightAnimator();
                mOutAnimator = new ZoomOutRightAnimator();
                break;
        }

        ViewCompat.setAlpha(target, alpha);
        ViewCompat.setTranslationX(target, translationX);
        ViewCompat.setTranslationY(target, translationY);
        ViewCompat.setRotationX(target, rotationX);
        ViewCompat.setRotationY(target, rotationY);
        ViewCompat.setScaleX(target, scale);
        ViewCompat.setScaleY(target, scale);
    }


    public int getStartDelay() {
        return mStartDelay;
    }

    public void setStartDelay(int startDelay) {
        mStartDelay = startDelay;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    public void setInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }
}
