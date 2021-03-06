package com.example.txl.tool.recyclerView

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.txl.tool.R
import kotlinx.android.synthetic.main.activity_item_decoration_demo.*
import java.lang.RuntimeException
import kotlin.math.max
import kotlin.math.min

class FlyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fly)
        recycler_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val adapter = FlyAdapter()
        recycler_view.adapter = adapter
//        recycler_view.addItemDecoration(TestItemDecoration())
        recycler_view.addItemDecoration(FlyItemDecoration(this, recycler_view))
    }
}

class FlyItemDecoration(private val context: Context, private val recyclerView: RecyclerView) : RecyclerView.ItemDecoration() {

    companion object {
        const val TAG = "FlyItemDecoration"
    }

    private var itemPath: Path = Path()
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flyDrawable: BitmapDrawable = ContextCompat.getDrawable(context, R.drawable.fly) as BitmapDrawable
    private val viewInfoSpareArray: SparseArray<ViewInfo> = SparseArray()

    /**
     * 滑动方向
     * */
    private var direct = 0
    private var totalOffsetY = 0
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            direct = dy
            totalOffsetY += dy
        }
    }

    /**
     * 将宽度拆分成多少分
     * */
    private val total = 5f
    private val strokeWidth = 80f

    private var maxPosition = 0
    private val matrix = Matrix()
    private val pathMeasure = PathMeasure()

    init {
        recyclerView.addOnScrollListener(scrollListener)
        mPaint.color = 0x77edcbaa
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = strokeWidth
        mPaint.strokeJoin = Paint.Join.ROUND
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.adapter == null) {
            return
        }
        canvas.save()
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val pos = parent.getChildAdapterPosition(child)
            maxPosition = max(maxPosition, pos)
            itemPath = Path()
            if (pos % 2 == 0) {//左侧管
                itemPath.moveTo(child.width / total, child.top.toFloat())
                itemPath.lineTo(child.width / total, child.top.toFloat() + child.height * (total - 1) / total)
                itemPath.lineTo(child.width * (total - 1) / total, child.top.toFloat() + child.height * (total - 1) / total)
                itemPath.lineTo(child.width * (total - 1) / total, child.top.toFloat() + child.height)
            } else {
                itemPath.moveTo(child.width * (total - 1) / total, child.top.toFloat())
                itemPath.lineTo(child.width * (total - 1) / total, child.top.toFloat() + child.height * (total - 1) / total)
                itemPath.lineTo(child.width / total, child.top.toFloat() + child.height * (total - 1) / total)
                itemPath.lineTo(child.width / total, child.top.toFloat() + child.height)
            }
            saveViewInfo(pos, child,itemPath)
            canvas.drawPath(itemPath, mPaint)
        }
        canvas.restore()
//        drawFly(canvas, parent)
        drawFlayV2(canvas, parent)
    }

    private fun drawFlayV2(canvas: Canvas,parent: RecyclerView){
        val rdy = (parent.computeVerticalScrollRange() + parent.height) * 1f / parent.computeVerticalScrollRange() * totalOffsetY
        //计算出小飞机应该移动到那个View上
        val attachViewInfo =  getAttachViewInfo(parent,rdy.toInt())
        val attachView = parent.findViewHolderForAdapterPosition(attachViewInfo.position)?.itemView ?: return
        val progress = (rdy-attachViewInfo.top)/attachView.height
        pathMeasure.setPath(attachViewInfo.path,false)
        pathMeasure.getMatrix(progress*pathMeasure.length,matrix,PathMeasure.POSITION_MATRIX_FLAG or PathMeasure.TANGENT_MATRIX_FLAG)
        matrix.preTranslate(-strokeWidth/2,-strokeWidth/2)
        matrix.preScale(0.5f,0.5f)
        canvas.save()
        canvas.drawBitmap(flyDrawable.bitmap,matrix,mPaint)
        canvas.restore()
    }

    private fun saveViewInfo(pos: Int, child: View,path:Path) {
        val viewInfo = ViewInfo()
        viewInfo.position = pos

        if (pos == 0) {
            viewInfo.top = 0
        } else {
            viewInfo.top = viewInfoSpareArray[pos - 1].bottom

        }
        viewInfo.path = path
        viewInfo.bottom = viewInfo.top + child.height.toLong()
//        Log.e(TAG,"saveViewInfo  $viewInfo")
        viewInfoSpareArray.put(pos, viewInfo)
    }

    /**
     * 画飞机
     * */
    private fun drawFly(canvas: Canvas, parent: RecyclerView) {
        val attachViewInfo = getAttachViewInfo(parent)
        val attachView = parent.findViewHolderForAdapterPosition(attachViewInfo.position)?.itemView
                ?: return
        //小飞机对当前View的总滑动距离  = 水平距离 + 垂直距离
        val totalDistance = (total - 2) / total * attachView.width + attachView.height
        //计算当前小飞机相对当前VIew的滑动完成度,
        //滑动单位像素小飞机移动的距离应该是  ；； 当前View的高度 / 移动总距离
        unitPixelDistance = totalDistance / (attachView.height)
        leftFirstPoint = attachView.height * (total - 1) / total
        leftSecondPoint = leftFirstPoint + (attachView.width.toFloat() * (total - 2) / total)
        val dy = totalOffsetY - attachViewInfo.top.toFloat()
        Log.e(TAG, "dy is $dy  top is : ${attachViewInfo.top}  offset = $totalOffsetY  move size ${totalOffsetY * unitPixelDistance}")
        val progress: Float = unitPixelDistance * dy / totalDistance
        if (progress > 1 || progress < 0) {//这个完成度最多是1，如果大于1说明代码 逻辑有问题
//            throw RuntimeException("progress is $progress please think more time")
//            return
        }
        val pos = parent.getChildAdapterPosition(attachView)
        //这个是否是左侧管道
        val isLeft = pos % 2 == 0
        val offsetX = 0
        val offsetY = 0
        val moveSize = dy * unitPixelDistance
        Log.e(TAG,"move size is $moveSize  leftFirstPoint::  $leftFirstPoint  leftSecondPoint :: $leftSecondPoint")
        val left = (getOffsetX(isLeft, dy, attachView) - strokeWidth / 2).toInt()
        val top = (getOffsetY(isLeft, dy, attachView) - strokeWidth/2 - dy).toInt()
        Log.e("FlyItemDecoration", "draw fly position:: ${attachViewInfo.position} attachViewInfo.top ${attachViewInfo.top} unitPixelDistance  :: $unitPixelDistance left :: $left  top :: $top  leftFirstPoint :: $leftFirstPoint  leftSecondPoint :: $leftSecondPoint  progress :: $progress  totalDistance :: $totalDistance")
        val right = (left + strokeWidth).toInt()
        val bottom = top + strokeWidth.toInt()
        val desRect = Rect(left, top, right, bottom)
        val cx = left + strokeWidth / 2f
        val cy = top + strokeWidth / 2f
        var degrees = 180f
        canvas.save()
        canvas.rotate(degrees, cx, cy)
        canvas.drawBitmap(flyDrawable.bitmap, null, desRect, mPaint)
        canvas.rotate(-degrees)
        canvas.restore()
    }

    /**
     * 左管道第一个拐点
     * */
    private var leftFirstPoint = 0f
    private var leftSecondPoint = 0f
    //滑动单位像素小飞机移动的距离应该是  ；；  移动总距离 / 当前View的高度
    private var unitPixelDistance = 0f

    private fun getOffsetX(left: Boolean, dy: Float, attachView: View): Int {
        //前进距离
        val moveSize = dy * unitPixelDistance
        if (left) {
            if (moveSize < leftFirstPoint) {
                return (1 / total * attachView.width).toInt()
            } else if (moveSize > leftSecondPoint) {
                return (attachView.width * (total - 1) / total).toInt()
            } else {
                return (unitPixelDistance * dy + attachView.width / total - attachView.height * (total - 1) / total).toInt()
            }
        } else {
            if (moveSize < leftFirstPoint) {
                return (attachView.width * (total - 1) / total).toInt()
            } else if (moveSize > leftSecondPoint) {
                return (1 / total * attachView.width).toInt()
            } else {
                return (-unitPixelDistance * dy + (attachView.width  + attachView.height) * (total - 1) / total).toInt()
            }
        }
    }

    private fun getOffsetY(left: Boolean, dy: Float, attachView: View): Int {
        //前进距离
        val moveSize = dy * unitPixelDistance
        if (left) {
            if (moveSize < leftFirstPoint) {
                return (unitPixelDistance * dy).toInt()
            } else if (moveSize > leftSecondPoint) {
//                return (unitPixelDistance * attachView.height / (total * unitPixelDistance * attachView.height - (total - 1) * attachView.height - (total - 2) * attachView.width) * dy
//                        + attachView.height - attachView.height * attachView.height * unitPixelDistance / (total * unitPixelDistance * attachView.height - (total - 1) * attachView.height - (total - 2) * attachView.width)).toInt()
                val offsetY = unitPixelDistance * dy - (total -2)/total * attachView.width
                return min(offsetY.toInt(),attachView.height)
            } else {
                return ((total - 1) / total * attachView.height).toInt()
            }
        } else {
            if (moveSize < leftFirstPoint) {
                return (unitPixelDistance * dy).toInt()
            } else if (moveSize > leftSecondPoint) {
//                return (unitPixelDistance * attachView.height / (total * unitPixelDistance * attachView.height - (total - 1) * attachView.height - (total - 2) * attachView.width) * dy
//                        + attachView.height - attachView.height * attachView.height * unitPixelDistance / (total * unitPixelDistance * attachView.height - (total - 1) * attachView.height - (total - 2) * attachView.width)).toInt()
                val offsetY = unitPixelDistance * dy - (total -2)/total * attachView.width
                return min(offsetY.toInt(),attachView.height)
            } else {
                return ((total - 1) / total * attachView.height).toInt()
            }
        }
    }

    /**
     * 查找小飞机应该在哪个View上面
     * */
    private fun getAttachView(recyclerView: RecyclerView): View? {
        val attachViewInfo = getAttachViewInfo(recyclerView)
        return recyclerView.findViewHolderForAdapterPosition(attachViewInfo.position)?.itemView
    }

    private fun getAttachViewInfo(recyclerView: RecyclerView): ViewInfo {
        return getAttachViewInfo(recyclerView,totalOffsetY)
    }

    private fun getAttachViewInfo(parent: RecyclerView,dy: Int):ViewInfo{
        if (recyclerView.adapter == null) {
            throw RuntimeException("adapter is null")
        }
        var start = 0
        var end = maxPosition
        var half = (end - start) / 2
        var attachViewInfo = viewInfoSpareArray.get(half)

        while (!isQualifiedCandidateView(attachViewInfo)) {
            if (start == end) {
                Log.e("FlyItemDecoration", "start == end  is $start")
                break
            }
            if (half < 0) {
                Log.e("FlyItemDecoration", "why half < 0  half is $half")
                break
            }
            if (attachViewInfo.top >= dy) {//当前的View在滑动距离下面
                end = half
                half = (end - start) / 2 + start
                attachViewInfo = viewInfoSpareArray.get(half)
            } else {
                start = half
                half = (end - start) / 2 + start
                attachViewInfo = viewInfoSpareArray.get(half)
            }
            if(end - start == 1){
                break
            }
        }
        return attachViewInfo
    }

    private fun isQualifiedCandidateView(viewInfo: ViewInfo): Boolean {
        return viewInfo.top <= totalOffsetY && viewInfo.bottom >= totalOffsetY
    }

    class ViewInfo {
        /**
         * 记录adapter所在位置
         * */
        var position = 0
        /**
         * 相对第一个View总偏移的
         * */
        var top = 0L
        var bottom = 0L
        var path:Path = Path()

        override fun toString(): String {
            return "ViewInfo(position=$position, top=$top, bottom=$bottom, path=$path)"
        }
    }
}

class FlyAdapter : RecyclerView.Adapter<FlyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlyViewHolder {
        return FlyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_decoration_text, parent, false))
    }

    override fun getItemCount(): Int {
        return 20
    }

    override fun onBindViewHolder(holder: FlyViewHolder, position: Int) {
        holder.tv.text = "我是第 $position 个"
        if (position % 2 == 0) {
            holder.tv.setBackgroundColor(Color.parseColor("#78945612"))
        } else {
            holder.tv.setBackgroundColor(Color.parseColor("#78123456"))
        }
    }
}

class FlyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tv: TextView = itemView as TextView

    init {
        val params = tv.layoutParams
        params.height = 480
        tv.layoutParams = params
    }
}