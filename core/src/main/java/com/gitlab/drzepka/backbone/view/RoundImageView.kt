@file:Suppress("unused")

package com.gitlab.drzepka.backbone.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import com.gitlab.drzepka.backbone.R

/**
 * Extension of the [ImageView] class that allows to draw a round images. Each corner's radius can be adjusted
 * independently.
 *
 * Available properties:
 * * `app:radius` - radius of the curve. Can be set between `0` and `100`. `0` means no radius and `100` - maximum
 * radius. This property has the highest priority, so if it's set, other `app:radius*` properties are ignored.
 * * `app:radiusTopLeft` - same as `app:radius` but applies only to the top left corner
 * * `app:radiusTopRight` - same as `app:radius` but applies only to the top right corner
 * * `app:radiusBottomLeft` - same as `app:radius` but applies only to the bottom left corner
 * * `app:radiusBottomRight` - same as `app:radius` but applies only to the bottom right corner
 * * `app:fit` - if set to `true`, image will be scaled so it won't be cropped by any radius. (default value is `false`)
 * * `app:adjust` - if set to `true`, view bounds will be adjusted to maintain aspect ratio. (default value is `false`)
 *
 * By default all radii are set to `100`.
 */
class RoundImageView : ImageView {

    private lateinit var backgroundPaint: Paint
    private lateinit var circlePath: Path

    // (In percentages)
    private var radiusTopLeft = 100
    private var radiusTopRight = 100
    private var radiusBottomLeft = 100
    private var radiusBottomRight = 100

    private var fit = false
    private var fitDirty = false
    private var adjustBounds = false
    private var fitScale = 1f
    private var fitScaleY = 1f
    private var hardwareAcceleration: Boolean? = null
    private var ownBackground: Drawable? = null

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(attrs, defStyleAttr)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initialize(attrs, defStyleAttr, defStyleAttr)
    }

    private fun initialize(attrs: AttributeSet? = null, defaults: Int? = null, defaultStyle: Int? = null) {
        // Take over the background so its drawing will be controlled here
        ownBackground = background
        background = null

        // Initialize paints
        backgroundPaint = Paint()
        backgroundPaint.isAntiAlias = true
        backgroundPaint.style = Paint.Style.FILL
        circlePath = Path()

        if (attrs == null)
            return

        val array = if (defaultStyle != null && defaults != null)
            context.obtainStyledAttributes(attrs, R.styleable.RoundImageView, defaults, defaultStyle)!!
        else
            context.obtainStyledAttributes(attrs, R.styleable.RoundImageView)!!

        if (array.hasValue(R.styleable.RoundImageView_radius)) {
            // Master radius overrules others
            val radius = array.getInteger(R.styleable.RoundImageView_radius, 0)
            checkRadius("Radius", radius)

            radiusTopLeft = radius
            radiusTopRight = radius
            radiusBottomLeft = radius
            radiusBottomRight = radius
        } else {
            // Obtain other radii
            if (array.hasValue(R.styleable.RoundImageView_radiusTopLeft)) {
                radiusTopLeft = array.getInteger(R.styleable.RoundImageView_radiusTopLeft, 0)
                checkRadius("Top left radius", radiusTopLeft)
            }
            if (array.hasValue(R.styleable.RoundImageView_radiusTopRight)) {
                radiusTopRight = array.getInteger(R.styleable.RoundImageView_radiusTopRight, 0)
                checkRadius("Top right radius", radiusTopRight)
            }
            if (array.hasValue(R.styleable.RoundImageView_radiusBottomLeft)) {
                radiusBottomLeft = array.getInteger(R.styleable.RoundImageView_radiusBottomLeft, 0)
                checkRadius("Bottom left radius", radiusBottomLeft)
            }
            if (array.hasValue(R.styleable.RoundImageView_radiusBottomRight)) {
                radiusBottomRight = array.getInteger(R.styleable.RoundImageView_radiusBottomRight, 0)
                checkRadius("Bottom right radius", radiusBottomRight)
            }
        }

        fit = array.getBoolean(R.styleable.RoundImageView_fit, false)
        adjustBounds = array.getBoolean(R.styleable.RoundImageView_adjust, false)
    }

    private fun postInitialize(accelerated: Boolean) {
        // Determine whether hardware acceleration is allowed to use
        hardwareAcceleration = accelerated && isHardwareAccelerated

        if (hardwareAcceleration == true) {
            // Switching to the hardware layer is required in order for transparency to work. Unfortunately this results
            // in slightly higher memory usage.
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            backgroundPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            circlePath.fillType = Path.FillType.INVERSE_EVEN_ODD
        } else
            circlePath.fillType = Path.FillType.EVEN_ODD
    }

    private fun checkRadius(name: String, value: Int) {
        if (value < 0 || value > 100)
            throw IllegalArgumentException("$name must be between 0 and 100")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        ownBackground?.setBounds(0, 0, w, h)
        rebuildPath()

        // Set this flag, so a drawable scale will be computed before the next draw call
        fitDirty = fit
    }

    private fun fitDrawable() {
        val maxRadius = listOf(radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight).max()!!
        val pc = maxRadius / 100f
        val rx = width * pc / 2f
        val ry = height * pc / 2f

        // Compute the new position of vertex
        val t = Math.atan(height.toDouble() / width)
        val x = rx * Math.cos(t) + (width / 2f - rx)
        val y = ry * Math.sin(t) + (height / 2f - ry)

        // Compute scales
        fitScale = (x / (width / 2f)).toFloat()
        fitScaleY = (y / (height / 2f)).toFloat()

        // Adjust view bounds, if necessary
        if (!adjustBounds || drawable.intrinsicWidth == -1)
            return
        val ratio = width / height.toFloat()
        val drawableRatio = drawable.intrinsicWidth / drawable.intrinsicHeight.toFloat()

        // Shrink one of the dimensions in order to maintain aspect ratio
        if (ratio > drawableRatio) {
            // Shrink width
            fitScale *= (drawableRatio / ratio)
        } else if (ratio < drawableRatio) {
            // Shrink height
            fitScaleY *= (drawableRatio * ratio)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0)
            return

        // Initialize drawing components, depending on whether hardware acceleration is allowed
        if (hardwareAcceleration == null)
            postInitialize(canvas.isHardwareAccelerated)

        if (hardwareAcceleration == false)
            canvas.clipPath(circlePath)
        ownBackground?.draw(canvas)
        if (hardwareAcceleration == true)
            canvas.drawPath(circlePath, backgroundPaint)

        if (fitDirty) {
            fitDirty = false
            fitDrawable()
        }

        if (fit) {
            // Scale the canvas to the foreground drawable will be completly visible
            canvas.save()
            canvas.scale(fitScale, fitScaleY, width / 2f, height / 2f)
        }
        super.onDraw(canvas)
        if (fit)
            canvas.restore()
    }

    @Suppress("JoinDeclarationAndAssignment")
    private fun rebuildPath() {
        circlePath.reset()
        var pc: Float
        var rx: Float
        var ry: Float
        val rect = RectF()

        // Top left corner
        pc = radiusTopLeft / 100f
        rx = width * pc / 2f
        ry = height * pc / 2f
        rect.set(0f, 0f, rx * 2, ry * 2)
        circlePath.moveTo(0f, ry)
        circlePath.arcTo(rect, 180f, 90f)

        // Top right corner
        pc = radiusTopRight / 100f
        rx = width * pc / 2f
        ry = height * pc / 2f
        rect.set(width - 2 * rx, 0f, width * 1f, 2 * ry)
        circlePath.arcTo(rect, 270f, 90f)

        // Bottom right corner
        pc = radiusBottomRight / 100f
        rx = width * pc / 2f
        ry = height * pc / 2f
        rect.set(width - 2 * rx, height - 2 * ry, width * 1f, height * 1f)
        circlePath.arcTo(rect, 0f, 90f)

        // Bottom right corner
        pc = radiusBottomLeft / 100f
        rx = width * pc / 2f
        ry = height * pc / 2f
        rect.set(0f, height - 2 * ry, 2 * rx, height * 1f)
        circlePath.arcTo(rect, 90f, 90f)

        circlePath.close()
    }
}
