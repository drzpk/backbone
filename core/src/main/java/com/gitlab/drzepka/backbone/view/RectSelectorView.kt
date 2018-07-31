package com.gitlab.drzepka.backbone.view

import android.content.Context
import android.graphics.*
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import com.gitlab.drzepka.backbone.R
import com.gitlab.drzepka.backbone.util.Converter

/**
 * Used for selecting rectangular area. User can drag one of the four corners in order to change selected area.
 *
 * **Available attributes:**
 * * `app:overlayColor` - overlay color of the area that isn't selected
 * * `app:cornerColor` - color of corner circles used to change selection
 * * `app:cornerRadius` - radius of corner circles
 */
class RectSelectorView : View {

    /** Currently selected region. Call the [invalidate] method after manually changing the selection. */
    val selection = Rect()
    /** Minimum width of the selection box. */
    var minSelectionWidth = 320
        set(value) {
            field = Math.min(value, width)
        }
    /** Minimum height of the selection box. */
    var minSelectionHeight = 240
        set(value) {
            field = Math.min(value, height)
        }

    private val drawPaint = Paint()
    private val clearPaint = Paint()
    private var overlayColor = 0x90000000.toInt()
    private var cornerColor = 0xFFF3FF27.toInt()
    private var cornerRadius = 0f

    private var dragMode = DragMode.NOTHING
    private var startX = 0f
    private var startY = 0f
    private var startLeft = 0
    private var startTop = 0

    constructor(context: Context?) : super(context) {
        initialize()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initialize(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(attrs, defStyleAttr)
    }

    @Suppress("unused")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initialize(attrs, defStyleAttr, defStyleRes)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Change corners' positions proportionally
        val factorX = w.toFloat() / oldw
        val factorY = h.toFloat() / oldh

        selection.left = (selection.left * factorX).toInt()
        selection.right = (selection.right * factorX).toInt()
        selection.top = (selection.top * factorY).toInt()
        selection.bottom = (selection.bottom * factorY).toInt()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        fun checkCornerDistance(x: Int, y: Int): Boolean {
            val distance = Math.hypot(x.toDouble() - event.x, y.toDouble() - event.y)
            return distance <= cornerRadius
        }

        fun between(min: Int, max: Int, value: Float): Boolean = value >= min && value <= max

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Determine what will be dragged
                dragMode = when {
                    checkCornerDistance(selection.left, selection.top) -> DragMode.UPPER_LEFT
                    checkCornerDistance(selection.right, selection.top) -> DragMode.UPPER_RIGHT
                    checkCornerDistance(selection.left, selection.bottom) -> DragMode.LOWER_LEFT
                    checkCornerDistance(selection.right, selection.bottom) -> DragMode.LOWER_RIGHT
                    between(selection.left, selection.right, event.x) || between(selection.top, selection.bottom, event.y) -> {
                        startX = event.x
                        startY = event.y
                        startLeft = selection.left
                        startTop = selection.top
                        DragMode.SELECTION
                    }
                    else -> DragMode.NOTHING
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                dragMode = DragMode.NOTHING
                true
            }
            MotionEvent.ACTION_MOVE -> {
                @Suppress("NON_EXHAUSTIVE_WHEN")
                when (dragMode) {
                    RectSelectorView.DragMode.UPPER_LEFT -> {
                        selection.left = Math.max(Math.min(event.x.toInt(), selection.right - minSelectionWidth), 0)
                        selection.top = Math.max(Math.min(event.y.toInt(), selection.bottom - minSelectionHeight), 0)
                        invalidate()
                    }
                    RectSelectorView.DragMode.UPPER_RIGHT -> {
                        selection.right = Math.min(Math.max(event.x.toInt(), selection.left + minSelectionWidth), width)
                        selection.top = Math.max(Math.min(event.y.toInt(), selection.bottom - minSelectionHeight), 0)
                        invalidate()
                    }
                    RectSelectorView.DragMode.LOWER_LEFT -> {
                        selection.left = Math.max(Math.min(event.x.toInt(), selection.right - minSelectionWidth), 0)
                        selection.bottom = Math.min(Math.max(event.y.toInt(), selection.top + minSelectionHeight), height)
                        invalidate()
                    }
                    RectSelectorView.DragMode.LOWER_RIGHT -> {
                        selection.right = Math.min(Math.max(event.x.toInt(), selection.left + minSelectionWidth), width)
                        selection.bottom = Math.min(Math.max(event.y.toInt(), selection.top + minSelectionHeight), height)
                        invalidate()
                    }
                    RectSelectorView.DragMode.SELECTION -> {
                        var left = Math.max(startLeft - (startX - event.x), 0f)
                        if (left + selection.width() > right)
                            left -= (left + selection.width() - right)

                        var top = Math.max(startTop - (startY - event.y), 0f)
                        if (top + selection.height() > bottom)
                            top -= (top + selection.height() - bottom)

                        selection.offsetTo(left.toInt(), top.toInt())
                        invalidate()
                    }
                }

                true
            }
            else -> false
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas!!.drawColor(overlayColor, PorterDuff.Mode.SRC_OVER)
        canvas.drawRect(selection.left.toFloat(), selection.top.toFloat(), selection.right.toFloat(), selection.bottom.toFloat(), clearPaint)

        drawPaint.color = cornerColor
        canvas.drawCircle(selection.left.toFloat(), selection.top.toFloat(), cornerRadius, drawPaint)
        canvas.drawCircle(selection.right.toFloat(), selection.top.toFloat(), cornerRadius, drawPaint)
        canvas.drawCircle(selection.left.toFloat(), selection.bottom.toFloat(), cornerRadius, drawPaint)
        canvas.drawCircle(selection.right.toFloat(), selection.bottom.toFloat(), cornerRadius, drawPaint)
    }

    private fun initialize(attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) {
        val styles = context.obtainStyledAttributes(attrs, R.styleable.RectSelectorView, defStyleAttr, defStyleRes)
        overlayColor = styles.getColor(R.styleable.RectSelectorView_overlayColor, overlayColor)
        cornerColor = styles.getColor(R.styleable.RectSelectorView_cornerColor, cornerColor)
        cornerRadius = styles.getDimension(R.styleable.RectSelectorView_cornerRadius, Converter.dpToPx(context, 7f))
        styles.recycle()

        // Set initial selection position
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val factor = 0.9f
                selection.top = (height * (1 - factor)).toInt()
                selection.left = (width * (1 - factor)).toInt()
                selection.right = (width * factor).toInt()
                selection.bottom = (height * factor).toInt()
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        // PorterDuff.Mode.CLEAR doesn't work in hardware-accelerated layer
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        clearPaint.color = Color.TRANSPARENT
        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private enum class DragMode {
        NOTHING,
        UPPER_LEFT,
        UPPER_RIGHT,
        LOWER_LEFT,
        LOWER_RIGHT,
        SELECTION
    }
}