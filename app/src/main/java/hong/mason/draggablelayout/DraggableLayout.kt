package hong.mason.draggablelayout

import android.content.Context
import android.support.v4.view.ViewCompat
import android.support.v4.widget.ViewDragHelper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * Created by mason on 2017. 11. 29..
 * 하단에서 뷰가 올라오고 내려오게 할 수 있는 뷰 그룹
 */
class DraggableLayout : ViewGroup {
    private lateinit var viewDragHelper: ViewDragHelper
    private lateinit var dimView: View
    private lateinit var target: View
    private var isFirstLayout = true
    private var preY: Float? = null
    private var diffY = 0F
    private var state = State.EXPANDED
    private var callbacks : MutableList<Callback> = ArrayList()

    interface Callback {
        fun onExpanded()
        fun onCollapsed()
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet? = null) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.DraggableLayout)
        val dimColor = ta.getColor(R.styleable.DraggableLayout_dimColor, DEFAULT_DIM_COLOR)
        val initialState = ta.getInt(R.styleable.DraggableLayout_state, State.EXPANDED.ordinal)
        state = State.values()[initialState]
        ta.recycle()
        createViewDragHelper()
        createDimView(dimColor)
    }

    fun expand() {
        post { smoothSlideTo(0, measuredHeight - target.measuredHeight) }
    }

    fun collapse() {
        post { smoothSlideTo(0, measuredHeight) }
    }

    fun addCallback(callback: Callback) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        measureChildWithMargins(dimView, widthMeasureSpec, 0, heightMeasureSpec, 0)
        measureChildWithMargins(target, widthMeasureSpec, 0, heightMeasureSpec, 0)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        dimView.layout(left, 0, right, dimView.measuredHeight)
        if (isFirstLayout && state == State.COLLAPSED) {
            target.layout(left, measuredHeight, right, measuredHeight + target.measuredHeight)
        } else {
            target.layout(left, measuredHeight - target.measuredHeight, right, measuredHeight)
        }
        isFirstLayout = false
        updateDimOpacity()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount == 1) {
            throw Exception("need least 1 child view")
        }
        target = getChildAt(1)

    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            if (preY == null) {
                preY = ev.y
            }

            if (viewDragHelper.isViewUnder(target, ev.x.toInt(), ev.y.toInt())) {
            } else if (viewDragHelper.isViewUnder(dimView, ev.x.toInt(), ev.y.toInt())) {
                if (diffY == 0F) {
                    diffY = target.y - ev.y
                }
                val y = ev.y + diffY
                ev.setLocation(0F, y)
            }

            viewDragHelper.processTouchEvent(ev)

            if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
                diffY = 0F
            }
        }

        return true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return viewDragHelper.shouldInterceptTouchEvent(ev)
    }

    override fun computeScroll() {
        super.computeScroll()
        if (viewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    private fun smoothSlideTo(toX: Int, toY: Int) {
        if (viewDragHelper.smoothSlideViewTo(target, toX, toY)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return MarginLayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    private fun createViewDragHelper() {
        viewDragHelper = ViewDragHelper.create(this, 0.5F, InternalCallback())
        val density = context.resources.displayMetrics.density
        viewDragHelper.minVelocity = 100F * density
    }

    private fun createDimView(color : Int) {
        dimView = View(context)
        val params = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        dimView.layoutParams = params
        dimView.setBackgroundColor(color)
        addView(dimView)
    }

    private fun dispatchCallback() {
        when (state) {
            State.EXPANDED -> callbacks.forEach { it.onExpanded() }
            State.COLLAPSED -> callbacks.forEach { it.onCollapsed() }
        }
    }

    private fun updateDimOpacity() {
        val currentY = target.top - (measuredHeight - target.measuredHeight)
        val totalY = target.measuredHeight
        val ratio = 1F - currentY.toFloat() / totalY.toFloat()
        dimView.alpha = ratio
    }

    inner class InternalCallback : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View?, pointerId: Int): Boolean = true

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)

            if (state == ViewDragHelper.STATE_IDLE) {
                if (target.top < measuredHeight - target.measuredHeight / 2) {
                    this@DraggableLayout.state = State.EXPANDED
                } else {
                    this@DraggableLayout.state = State.COLLAPSED
                }
                dispatchCallback()
            }
        }

        override fun onViewPositionChanged(changedView: View?, left: Int, top: Int, dx: Int, dy: Int) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            if (changedView == target) {
                updateDimOpacity()
            }
        }

        override fun onViewReleased(releasedChild: View?, xvel: Float, yvel: Float) {
            state = if (target.top < measuredHeight - target.measuredHeight / 2) {
                smoothSlideTo(0, measuredHeight - target.measuredHeight)
                State.EXPANDED
            } else {
                smoothSlideTo(0, measuredHeight)
                State.COLLAPSED
            }
        }

        override fun clampViewPositionVertical(child: View?, top: Int, dy: Int): Int {
            return when {
                dy < 0 -> maxOf(top, measuredHeight - target.measuredHeight)
                dy > 0 -> minOf(top, measuredHeight)
                else -> target.top
            }
        }
    }

    private enum class State {
        EXPANDED, COLLAPSED
    }

    companion object {
        val TAG = "DraggableLayout"
        val DEFAULT_DIM_COLOR = 0x88000000.toInt()
    }
}