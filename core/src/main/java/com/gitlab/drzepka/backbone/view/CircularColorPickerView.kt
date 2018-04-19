@file:Suppress("MemberVisibilityCanBePrivate")

package com.gitlab.drzepka.backbone.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.graphics.ColorUtils
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.gitlab.drzepka.backbone.R
import com.gitlab.drzepka.backbone.util.Converter

/**
 *  An HSLA color picker view with circilar shape.
 *
 *  **Note:** this view is view is quite resource-heavy and doesn't work very well in horizontal orientations,
 *  so unless you really want to use it, use something different.
 *
 *  **Available attributes:**
 *  * `app:showSaturation` - whether to show saturation slider
 *  * `app:showLuminosity` - whether to show luminosity slider
 *  * `app:showAlpha` - whether to show alpha slider
 *  * `app:initialColor` - a color this picker will be set to
 */
class CircularColorPickerView : View {

    /**
     * Whether to show saturation slider.
     */
    var showSaturation = true
        private set
    /**
     * Whether to show luminosity slider.
     */
    var showLuminosity = true
        private set
    /**
     * Whether to show alpha slider.
     */
    var showAlpha = true
        private set

    /**
     * Chosen hue.
     */
    var pickedHue = 0
        set(value) {
            field = value
            if (autoUpdateColors) {
                if (value < 0 || value >= 360)
                    throw IllegalArgumentException("Hue must be in range [0, 360)")
                updateHueCenter()
                updateColor()
                updateShaders()
                invalidate()
            }
        }
    /**
     * Chosen saturation.
     */
    var pickedSaturation = 1f
        set (value) {
            field = value
            if (autoUpdateColors) {
                if (value < 0f || value > 1f)
                    throw IllegalArgumentException("Saturation must be in range [0, 1]")
                updateColor()
                updateShaders()
                invalidate()
            }
        }
    /**
     * Chosen luminosity.
     */
    var pickedLuminosity = 0.5f
        set (value) {
            field = value
            if (autoUpdateColors) {
                if (value < 0f || value > 1f)
                    throw IllegalArgumentException("Luminosity must be in range [0, 1]")
                updateColor()
                updateShaders()
                invalidate()
            }
        }
    /**
     * Chosen alpha.
     */
    var pickedAlpha = 1f
        set (value) {
            field = value
            if (autoUpdateColors) {
                if (value < 0f || value > 1f)
                    throw IllegalArgumentException("Alpha must be in range [0, 1]")
                updateColor()
                updateShaders()
                invalidate()
            }
        }
    /**
     * An RGBA-format color parsed from hue, saturation, luminosity and alpha.
     */
    var pickedColor = 0
        set (value) {
            field = value
            if (autoUpdateColors) {
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(value, hsl)
                autoUpdateColors = false
                pickedHue = hsl[0].toInt()
                pickedSaturation = hsl[1]
                pickedLuminosity = hsl[2]
                pickedAlpha = (value ushr 24) / 255f
                autoUpdateColors = true
                updateHueCenter()
                updateColor()
                updateShaders()
                invalidate()
            }
        }

    private var touchMode = TOUCH_MODE_NONE
    private var touchOffset = 0f
    private var offsetTop = 0f
    private var pickedHueCenter = PointF()
    private var autoUpdateColors = true

    private val fillPaint = Paint()
    private val strokePaint = Paint()
    private val circlePaint = Paint()
    private val sliderPaint = Paint()

    private val circleColors = IntArray(360)
    private val circleRect = RectF()
    private var circleWidthFraction = 0f
    private var circleCenter = PointF()
    private var circleRadius = 0f
    private var circleLines = FloatArray(0)

    private val saturationSliderRect = RectF()
    private val luminositySliderRect = RectF()
    private val alphaSliderRect = RectF()
    private var saturationSliderShader: Shader? = null
    private var luminositySliderShader: Shader? = null
    private var alphaSliderShader: Shader? = null
    private var saturationSliderPos = PointF()
    private var luminositySliderPos = PointF()
    private var alphaSliderPos = PointF()


    constructor(context: Context?) : super(context) {
        initialize()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initialize(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(attrs, defStyleAttr)
    }

    private fun initialize(attrs: AttributeSet? = null, defStyleAttr: Int = 0) {
        //setLayerType(LAYER_TYPE_HARDWARE, null)

        autoUpdateColors = false

        // Obtain custom attributes
        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.CircularColorPickerView, defStyleAttr, 0)
            showSaturation = array.getBoolean(R.styleable.CircularColorPickerView_showSaturation, showSaturation)
            showLuminosity = array.getBoolean(R.styleable.CircularColorPickerView_showLuminosity, showLuminosity)
            showAlpha = array.getBoolean(R.styleable.CircularColorPickerView_showAlpha, showAlpha)

            if (array.hasValue(R.styleable.CircularColorPickerView_initialColor)) {
                pickedColor = array.getColor(R.styleable.CircularColorPickerView_initialColor, 0)
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(pickedColor, hsl)
                pickedHue = hsl[0].toInt()
                pickedSaturation = hsl[1]
                pickedLuminosity = hsl[2]
                pickedAlpha = (pickedColor ushr 24) / 255f
            }

            array.recycle()
        }

        // Convert static fields' units to pixels
        if (circleContrastStroke == -1f) {
            circleContrastStroke = Converter.dpToPx(context, CIRCLE_CONTRAST_STROKE.toFloat())
            componentMargin = Converter.dpToPx(context, COMPONENT_MARGIN.toFloat())
            sliderHeight = Converter.dpToPx(context, SLIDER_HEIGHT.toFloat())
        }

        // Get RGB color from HSL
        (0 until circleColors.size).forEachIndexed { i, _ ->
            circleColors[i] = ColorUtils.HSLToColor(floatArrayOf(i.toFloat(), 1f, 0.5f))
        }
        updateColor()

        // Initialize paints
        fillPaint.style = Paint.Style.FILL
        fillPaint.isAntiAlias = true

        strokePaint.style = Paint.Style.STROKE
        strokePaint.isAntiAlias = true

        circlePaint.style = Paint.Style.STROKE
        circlePaint.strokeCap = Paint.Cap.ROUND
        circlePaint.strokeWidth = Converter.dpToPx(context, CIRCLE_STROKE.toFloat())
        circlePaint.isAntiAlias = true

        sliderPaint.style = Paint.Style.STROKE
        sliderPaint.strokeWidth = 1f
        sliderPaint.isAntiAlias = true

        autoUpdateColors = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null)
            return super.onTouchEvent(event)
        autoUpdateColors = false

        fun getValue(rect: RectF): Float {
            var pos = event.x - touchOffset
            if (pos < rect.left)
                pos = rect.left
            else if (pos > rect.right)
                pos = rect.right
            pos -= rect.left

            return pos / (rect.right - rect.left)
        }

        fun updateSliders() {
            updateColor()
            updateShaders()
            invalidate()
        }

        fun handleHue() {
            // Calculate the angle
            val angle = Math.atan2(event.x - circleCenter.x.toDouble(), circleCenter.y.toDouble() - event.y)
            pickedHue = (Math.round(Math.toDegrees(angle))).toInt()
            if (pickedHue < 0)
                pickedHue += 360
            updateHueCenter()
            updateColor()
            updateShaders()
            invalidate()
        }

        fun handleSaturation() {
            pickedSaturation = getValue(saturationSliderRect)
            updateSliderPos(saturationSliderPos, pickedSaturation, saturationSliderRect)
            updateSliders()
        }

        fun handleLuminosity() {
            pickedLuminosity = getValue(luminositySliderRect)
            updateSliderPos(luminositySliderPos, pickedLuminosity, luminositySliderRect)
            updateSliders()
        }

        fun handleAlpha() {
            pickedAlpha = getValue(alphaSliderRect)
            updateSliderPos(alphaSliderPos, pickedAlpha, alphaSliderRect)
            updateSliders()
        }

        fun inSliderBoundaries(rect: RectF): Boolean {
            // Slider pointer size is taken into account
            return event.x >= rect.left - sliderHeight
                    && event.x <= rect.right + sliderHeight
                    && event.y >= rect.top
                    && event.y <= rect.bottom
        }

        fun inSliderPointerBoundaries(pointer: PointF) = Math.abs(event.x - pointer.x) <= sliderHeight

        when (event.action) {
        // Determine which component was clicked
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Check hue
                val dist = Math.hypot(event.x - circleCenter.x.toDouble(), event.y - circleCenter.y.toDouble())
                if (dist >= circleRadius * CIRCLE_COLOR_AREA_RADIUS && dist <= circleRadius * 1.2f) {
                    touchMode = TOUCH_MODE_HUE
                    handleHue()
                    autoUpdateColors = true
                    return true
                }

                // Check saturation
                if (showSaturation && inSliderBoundaries(saturationSliderRect)) {
                    if (inSliderPointerBoundaries(saturationSliderPos)) {
                        touchMode = TOUCH_MODE_SATURATION
                        touchOffset = event.x - saturationSliderPos.x
                    }
                    handleSaturation()
                    autoUpdateColors = true
                    return true
                }

                // Check luminosity
                if (showLuminosity && inSliderBoundaries(luminositySliderRect)) {
                    if (inSliderPointerBoundaries(luminositySliderPos)) {
                        touchMode = TOUCH_MODE_LUMINOSITY
                        touchOffset = event.x - luminositySliderPos.x
                    }
                    handleLuminosity()
                    autoUpdateColors = true
                    return true
                }

                // Check alpha
                if (showAlpha && inSliderBoundaries(alphaSliderRect)) {
                    if (inSliderPointerBoundaries(alphaSliderPos)) {
                        touchMode = TOUCH_MODE_ALPHA
                        touchOffset = event.x - alphaSliderPos.x
                    }
                    handleAlpha()
                    autoUpdateColors = true
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                when (touchMode) {
                    TOUCH_MODE_HUE -> {
                        handleHue()
                    }
                    TOUCH_MODE_SATURATION -> {
                        handleSaturation()
                    }
                    TOUCH_MODE_LUMINOSITY -> {
                        handleLuminosity()
                    }
                    TOUCH_MODE_ALPHA -> {
                        handleAlpha()
                    }
                }

                if (touchMode != TOUCH_MODE_NONE) {
                    autoUpdateColors = true
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                // Reset touch mode
                touchMode = TOUCH_MODE_NONE
                touchOffset = 0f
            }
        }

        autoUpdateColors = true
        return super.onTouchEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        // Prevent from scrolling when this view is doing something
        if (touchMode != TOUCH_MODE_NONE)
            parent.requestDisallowInterceptTouchEvent(true)

        return super.dispatchTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Compute hue circle boundaries
        val width = (w - paddingStart - paddingEnd) * circleWidthFraction
        circleRect.top = paddingTop.toFloat() + circlePaint.strokeWidth
        circleRect.left = (w - width) / 2f
        circleRect.right = w - circleRect.left
        circleRect.bottom = circleRect.top + width
        circleRadius = (width - circlePaint.strokeWidth) / 2f
        circleCenter.x = circleRect.centerX()
        circleCenter.y = circleRect.centerY()

        // Generate the contrast lines
        val lineCount = 20
        val deltaAngle = 360 / lineCount.toFloat()
        val longerRadius = circleRadius - 6f
        val shorterRadius = circleRadius - circleRadius / 10 - 6f
        circleLines = FloatArray(lineCount * 4)
        (0 until lineCount).forEach {
            val r = Math.toRadians(deltaAngle * it.toDouble())
            circleLines[it * 4 + 0] = (longerRadius * Math.cos(r)).toFloat() + circleCenter.x
            circleLines[it * 4 + 1] = (longerRadius * Math.sin(r)).toFloat() + circleCenter.y
            circleLines[it * 4 + 2] = (shorterRadius * Math.cos(r)).toFloat() + circleCenter.x
            circleLines[it * 4 + 3] = (shorterRadius * Math.sin(r)).toFloat() + circleCenter.y
        }

        if (showSaturation || showLuminosity || showAlpha) {
            // Compute the X position of sliders
            val sliderWidth = (w - paddingStart - paddingEnd) * SLIDER_WIDTH
            val sliderLeft = (w - sliderWidth) / 2f
            // Slider bar is slightly smaller that the whole slider. The following variable describes how much
            // height from one side (top or bottom) is shrunk.
            val deltaHeight = sliderHeight * 0.25f
            var shownSliders = 0

            if (showSaturation) {
                saturationSliderRect.left = sliderLeft
                saturationSliderRect.right = w - sliderLeft
                saturationSliderRect.top = circleRect.bottom + componentMargin + deltaHeight
                saturationSliderRect.bottom = saturationSliderRect.top + sliderHeight - deltaHeight
                saturationSliderPos.y = saturationSliderRect.centerY()
                updateSliderPos(saturationSliderPos, pickedSaturation, saturationSliderRect)
                shownSliders++
            }

            if (showLuminosity) {
                luminositySliderRect.left = sliderLeft
                luminositySliderRect.right = w - sliderLeft
                luminositySliderRect.top = circleRect.bottom + componentMargin + (sliderHeight + componentMargin) * shownSliders + deltaHeight
                luminositySliderRect.bottom = luminositySliderRect.top + sliderHeight - deltaHeight
                luminositySliderPos.y = luminositySliderRect.centerY()
                updateSliderPos(luminositySliderPos, pickedLuminosity, luminositySliderRect)
                shownSliders++
            }

            if (showAlpha) {
                alphaSliderRect.left = sliderLeft
                alphaSliderRect.right = w - sliderLeft
                alphaSliderRect.top = circleRect.bottom + componentMargin + (sliderHeight + componentMargin) * shownSliders + deltaHeight
                alphaSliderRect.bottom = alphaSliderRect.top + sliderHeight - deltaHeight
                alphaSliderPos.y = alphaSliderRect.centerY()
                updateSliderPos(alphaSliderPos, pickedAlpha, alphaSliderRect)
            }
        }

        // Update other fields
        updateHueCenter()
        updateShaders()
    }

    @Suppress("JoinDeclarationAndAssignment")
    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var chosenWidth = 0
        var chosenHeight: Int
        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)

        // Determine what width this view should have
        when (widthSpecMode) {
            MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> {
                // Take as much space as possible
                chosenWidth = MeasureSpec.getSize(widthMeasureSpec)
            }
            MeasureSpec.UNSPECIFIED -> {
                // Try setting width to be the same as screen width
                val metrics = DisplayMetrics()
                val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                manager.defaultDisplay.getMetrics(metrics)
                chosenWidth = metrics.widthPixels
            }
        }

        // Compute initial height (based on hue circle dimensions)
        chosenHeight = paddingTop + paddingBottom + (chosenWidth * CIRCLE_SIZE + circlePaint.strokeWidth * 2).toInt()

        // Compute an additional height occupied by optional sliders
        var sliderCount = 0
        if (showSaturation)
            sliderCount++
        if (showLuminosity)
            sliderCount++
        if (showAlpha)
            sliderCount++
        chosenHeight += Math.round(sliderCount * (sliderHeight + componentMargin))

        // Check if height restrictions aren't exceeded
        if (heightSpecMode != MeasureSpec.UNSPECIFIED) {
            val heightRestriction = MeasureSpec.getSize(heightMeasureSpec)

            fun scaleDown() {
                // If resulting scale is too small, just ignore it (extremely downscaled view won't be usable anyway)
                val scale = Math.max(heightRestriction / chosenHeight.toFloat(), 0.5f)
                circleWidthFraction = CIRCLE_SIZE * scale
            }

            if (heightSpecMode == MeasureSpec.AT_MOST && chosenHeight > heightRestriction) {
                // View is too big, perform downscaling
                scaleDown()
            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                if (chosenHeight > heightRestriction)
                    scaleDown()
                else {
                    offsetTop = (heightRestriction - chosenHeight) / 2f
                    circleWidthFraction = CIRCLE_SIZE
                }
            } else
                circleWidthFraction = CIRCLE_SIZE
        } else {
            // There are no height restrictions
            circleWidthFraction = CIRCLE_SIZE
        }

        setMeasuredDimension(chosenWidth, chosenHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null)
            return

        drawCircle(canvas)
        if (showSaturation)
            drawSaturationSlider(canvas)
        if (showLuminosity)
            drawLuminositySlider(canvas)
        if (showAlpha)
            drawAlphaSlider(canvas)
    }

    private fun drawCircle(canvas: Canvas) {
        // Circle shadow
        //canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius + circlePaint.strokeWidth, shadowPaint)

        // Hue ring
        (0 until 360).forEach {
            circlePaint.color = circleColors[it]
            canvas.drawArc(circleRect, it - 90f, 1f, false, circlePaint)
        }

        // Background
        fillPaint.color = CIRCLE_BACKGROUND
        canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius, fillPaint)

        // Contrast ring
        strokePaint.color = CIRCLE_CONTRAST
        strokePaint.strokeWidth = circleContrastStroke
        canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius * 0.98f, strokePaint)

        // Contrast lines
        canvas.drawLines(circleLines, strokePaint)

        // Picked hue pointer
        fillPaint.color = CIRCLE_CONTRAST
        canvas.drawCircle(pickedHueCenter.x, pickedHueCenter.y, circlePaint.strokeWidth / 1.2f, fillPaint)

        // Line from center to pointer
        canvas.drawLine(circleCenter.x, circleCenter.y, pickedHueCenter.x, pickedHueCenter.y, strokePaint)

        // Selected color
        fillPaint.color = pickedColor
        canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius * 0.33f, fillPaint)

        // Plus in the center
        val oldStroke = strokePaint.strokeWidth
        val delta = circleRadius * 0.16f
        strokePaint.strokeWidth *= 1.7f
        canvas.drawLine(circleCenter.x - delta, circleCenter.y, circleCenter.x + delta, circleCenter.y, strokePaint)
        canvas.drawLine(circleCenter.x, circleCenter.y - delta, circleCenter.x, circleCenter.y + delta, strokePaint)
        strokePaint.strokeWidth = oldStroke
    }

    private fun drawSaturationSlider(canvas: Canvas) {
        fillPaint.shader = saturationSliderShader
        canvas.drawRoundRect(saturationSliderRect, 10f, 10f, fillPaint)
        fillPaint.shader = null

        sliderPaint.style = Paint.Style.STROKE
        sliderPaint.color = Color.DKGRAY
        canvas.drawRoundRect(saturationSliderRect, 10f, 10f, sliderPaint)

        drawSliderPointer(canvas, saturationSliderPos)
    }

    private fun drawLuminositySlider(canvas: Canvas) {
        fillPaint.shader = luminositySliderShader
        canvas.drawRoundRect(luminositySliderRect, 10f, 10f, fillPaint)
        fillPaint.shader = null

        sliderPaint.style = Paint.Style.STROKE
        sliderPaint.color = Color.DKGRAY
        canvas.drawRoundRect(luminositySliderRect, 10f, 10f, sliderPaint)

        drawSliderPointer(canvas, luminositySliderPos)
    }

    private fun drawAlphaSlider(canvas: Canvas) {
        fillPaint.shader = alphaSliderShader
        canvas.drawRoundRect(alphaSliderRect, 10f, 10f, fillPaint)
        fillPaint.shader = null

        sliderPaint.style = Paint.Style.STROKE
        sliderPaint.color = Color.DKGRAY
        canvas.drawRoundRect(alphaSliderRect, 10f, 10f, sliderPaint)

        drawSliderPointer(canvas, alphaSliderPos)
    }

    private fun drawSliderPointer(canvas: Canvas, position: PointF) {
        // Draw the filling
        sliderPaint.style = Paint.Style.FILL
        sliderPaint.color = Color.WHITE
        canvas.drawCircle(position.x, position.y, sliderHeight, sliderPaint)

        // Draw the outline
        sliderPaint.style = Paint.Style.STROKE
        sliderPaint.color = Color.DKGRAY
        canvas.drawCircle(position.x, position.y, sliderHeight, sliderPaint)
    }

    /**
     * Computes position of the selected hue pointer.
     */
    private fun updateHueCenter() {
        val alpha = Math.toRadians(pickedHue.toDouble() - 90)
        val radius = circleRadius + circlePaint.strokeWidth / 2f
        pickedHueCenter.x = (radius * Math.cos(alpha)).toFloat() + circleCenter.x
        pickedHueCenter.y = (radius * Math.sin(alpha)).toFloat() + circleCenter.y
    }

    /**
     * Computes the X coordinate of the `sliderPos` based on `value` and `rect`.
     */
    private fun updateSliderPos(sliderPos: PointF, value: Float, rect: RectF) {
        val width = rect.right - rect.left
        sliderPos.x = rect.left + width * value
    }

    /**
     * Converts HSL (with aplha) into ARGB model.
     */
    private fun updateColor() {
        val hsl = ColorUtils.HSLToColor(floatArrayOf(pickedHue.toFloat(), pickedSaturation, pickedLuminosity))
        pickedColor = (Math.round(pickedAlpha * 255) shl 24) or (hsl and 0xffffff)
    }

    /**
     * Updates shaders used by sliders. This method must be called after all slider rects are updated.
     */
    private fun updateShaders() {
        fun hsla(h: Int, s: Float, v: Float, a: Float): Int {
            val hsl = ColorUtils.HSLToColor(floatArrayOf(h.toFloat(), s, v))
            val alpha = Math.round(255 * a) shl 24
            return (hsl and 0xffffff) or alpha
        }

        if (showSaturation) {
            saturationSliderShader = LinearGradient(
                    saturationSliderRect.left,
                    saturationSliderRect.centerY(),
                    saturationSliderRect.right, saturationSliderRect.centerY(),
                    hsla(pickedHue, 0f, pickedLuminosity, pickedAlpha),
                    hsla(pickedHue, 1f, pickedLuminosity, pickedAlpha),
                    Shader.TileMode.CLAMP)
        }

        if (showLuminosity) {
            luminositySliderShader = LinearGradient(
                    luminositySliderRect.left,
                    luminositySliderRect.centerY(),
                    luminositySliderRect.right,
                    luminositySliderRect.centerY(),
                    intArrayOf(
                            ColorUtils.setAlphaComponent(Color.BLACK, Math.round(pickedAlpha * 255)),
                            hsla(pickedHue, pickedSaturation, 0.5f, pickedAlpha),
                            ColorUtils.setAlphaComponent(Color.WHITE, Math.round(pickedAlpha * 255))),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP)
        }

        if (showAlpha) {
            alphaSliderShader = LinearGradient(
                    alphaSliderRect.left,
                    alphaSliderRect.centerY(),
                    alphaSliderRect.right,
                    alphaSliderRect.centerY(),
                    0x00000000,
                    hsla(pickedHue, pickedSaturation, pickedLuminosity, 1f),
                    Shader.TileMode.CLAMP)
        }
    }

    companion object {
        /**
         * Fraction of the width that the color picker circle will occupy (may be overriden if when there's
         * not enough space to draw circle).
         */
        private const val CIRCLE_SIZE = 0.6f
        /**
         * Width of the circle outer ring stroke (in DP units).
         */
        private const val CIRCLE_STROKE = 19
        /**
         * Background color of the hue circle dial.
         */
        private const val CIRCLE_BACKGROUND = Color.DKGRAY
        /**
         * Color of lines on the hue circle dial.
         */
        private const val CIRCLE_CONTRAST = Color.LTGRAY
        /**
         * Width of the inner hue circle lines stroke (in DP units).
         */
        private const val CIRCLE_CONTRAST_STROKE = 1.1
        /**
         * Size of the area inside the hue circle that will tell currently selected color (value as a fraction
         * of a circle radius).
         */
        private const val CIRCLE_COLOR_AREA_RADIUS = 0.4f
        /**
         * Vertical distance between components (in DP units).
         */
        private const val COMPONENT_MARGIN = 25
        /**
         * Width of the sliders (as a percentage of the widget width).
         */
        private const val SLIDER_WIDTH = 0.73f
        /**
         * Height of the sliders (in DP units).
         */
        private const val SLIDER_HEIGHT = 14

        // Touch modes
        private const val TOUCH_MODE_NONE = 0
        private const val TOUCH_MODE_HUE = 1
        private const val TOUCH_MODE_SATURATION = 2
        private const val TOUCH_MODE_LUMINOSITY = 3
        private const val TOUCH_MODE_ALPHA = 4

        // Const fields converted to pixels
        private var circleContrastStroke = -1f
        private var componentMargin = -1f
        private var sliderHeight = -1f
    }
}