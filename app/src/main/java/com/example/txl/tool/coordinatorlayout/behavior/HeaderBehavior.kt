package com.example.txl.tool.coordinatorlayout.behavior

import android.content.Context
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.example.txl.tool.R
import com.example.txl.tool.utils.DisplayUtil

/**
 * 当发起嵌套滑动的时候处理
 * */
class HeaderBehavior : androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior<View> {

    val TAG = HeaderBehavior::class.java.simpleName

    var mMaxTranslationY = 0
    var mLeftMargin = 0
    var mRightMargin = 0

    /**
     * y方向上的平移
     * */
    var mTranslationY = 0f

    private var isInit = false


    /**
     * y方向上滑动距离的变化距离比例，
     * */
    private val dyTransformationSpeed = 2

    internal fun init(context: Context) {
        mMaxTranslationY = DisplayUtil.dip2px(context, 55f)
        mLeftMargin = DisplayUtil.dip2px(context, 45f)
        mRightMargin = DisplayUtil.dip2px(context, 45f)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor() : super()

    override fun onStartNestedScroll(coordinatorLayout: androidx.coordinatorlayout.widget.CoordinatorLayout, child: View, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        if (!isInit) {
            init(child.context)
            isInit = true
        }
        if (axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0) {
            return true
        }
        return false
    }


    override fun onNestedPreScroll(coordinatorLayout: androidx.coordinatorlayout.widget.CoordinatorLayout, child: View, target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        mTranslationY = child.translationY
        if (dy > 0) {//向上
            //向上滑动优先滑动顶部的header
            if (Math.abs(mTranslationY) + dy < mMaxTranslationY) {
                consumed[1] = dy
            } else if (Math.abs(mTranslationY) < mMaxTranslationY && Math.abs(mTranslationY) + dy > mMaxTranslationY) {
                consumed[1] = (mMaxTranslationY - Math.abs(mTranslationY)).toInt()
            }
            child.translationY = mTranslationY - consumed[1] / dyTransformationSpeed
            transformationView(child)
        }
    }

    override fun onNestedScroll(coordinatorLayout: androidx.coordinatorlayout.widget.CoordinatorLayout, child: View, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        mTranslationY = child.translationY
        if (dyUnconsumed < 0) {//向下
            //在向下滑动时，当需要滑动的view不在消耗这个滑动的距离将header下拉
            if (Math.abs(mTranslationY) > 0 && mTranslationY - dyUnconsumed <= 0) {
                child.translationY = mTranslationY - dyUnconsumed / dyTransformationSpeed
            } else if (Math.abs(mTranslationY) > 0 && mTranslationY - dyUnconsumed > 0) {
                child.translationY = 0f
            }
            transformationView(child)
        }
    }

    override fun onNestedPreFling(coordinatorLayout: androidx.coordinatorlayout.widget.CoordinatorLayout, child: View, target: View, velocityX: Float, velocityY: Float): Boolean {
        var handle = false
        if (velocityY > 0) {//向下

        } else {//向上

        }
        return handle
    }

    override fun onNestedFling(coordinatorLayout: androidx.coordinatorlayout.widget.CoordinatorLayout, child: View, target: View, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed)
    }

    /**
     * 对当前使用这个view的behavior进行变化处理
     * */
    private fun transformationView(child: View, directUp:Boolean = false) {
        val tvSearch = child.findViewById<TextView>(R.id.tv_transformation)
        val translationY = Math.abs(child.translationY)
        val ratio = translationY / mMaxTranslationY
        //我的布局是使用的FrameLayout
        val layoutParams = tvSearch.layoutParams as FrameLayout.LayoutParams
        layoutParams.marginStart = (mLeftMargin * ratio).toInt()
        layoutParams.marginEnd = (mRightMargin * ratio).toInt()
        tvSearch.layoutParams = layoutParams

        child.background.mutate().alpha = (255 * ratio).toInt()
    }
}