@file:Suppress("MemberVisibilityCanBePrivate")

package com.gitlab.drzepka.backbone.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.gitlab.drzepka.backbone.R
import com.gitlab.drzepka.backbone.helper.gone

/**
 * Layout container that can collapse its contents by clicking a button in the layout header. It can contain only one
 * direct child.
 *
 * Available properties:
 * * `app:title` - title of a header
 * * `app:collapsed` - whether view is created as collapsed (defaults to false)
 * * `app:collapseSpeed` - a collapse speed (see the [collapseSpeed] field)
 * * `app:headerTextColor` - color of a header text
 * * `app:headerBackgroundColor` - color of a header background
 */
class CollapsibleLayout : LinearLayout {

    /** A text that will be displayed in layout header as title. */
    var title = ""
        set(value) {
            field = value
            if (setUp)
                titleView.text = value
        }

    /** When set to true, the [collapseSpeed] field is a total duration of animation, in milliseconds. */
    var staticSpeed = true

    /** Speed of expanding/collapsing animation, in milliseconds per 100px of height.  */
    var collapseSpeed = DEFAULT_COLLAPSE_SPEED
        set(value) {
            field = value
            if (value <= 10 || value > 10000)
                throw IllegalArgumentException("Collapse speed should be in range (10; 10000]")
        }

    private lateinit var container: FrameLayout
    private lateinit var layout: ViewGroup
    private lateinit var header: ViewGroup
    private lateinit var image: ImageView
    private lateinit var titleView: TextView

    private var setUp = false
    private var collapsed = false
    private var animationInProgress = false
    private var headerForeground = Color.BLACK
    private var headerBackground = Color.TRANSPARENT

    constructor (context: Context) : super(context) {
        initialize()
    }

    constructor (context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        initialize(attributeSet)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(attrs, defStyleAttr)
    }

    @Suppress("unused")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initialize(attrs, defStyleAttr, defStyleRes)
    }

    private fun initialize(attrs: AttributeSet? = null, defAttr: Int = 0, defRes: Int = 0) {
        // Change default values
        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.CollapsibleLayout, defAttr, defRes)!!
            title = array.getString(R.styleable.CollapsibleLayout_title) ?: javaClass.simpleName
            collapsed = array.getBoolean(R.styleable.CollapsibleLayout_collapsed, false)
            headerForeground = array.getColor(R.styleable.CollapsibleLayout_headerTextColor, Color.BLACK)
            headerBackground = array.getColor(R.styleable.CollapsibleLayout_headerBackgroundColor, Color.TRANSPARENT)
        }

        orientation = VERTICAL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!setUp) {
            // Create view that will contain collapsible layout
            container = FrameLayout(context)
            container.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            if (childCount != 1)
                throw IllegalStateException("Exactly one child must be provided")
            layout = getChildAt(0) as ViewGroup

            // Insert the header
            header = LayoutInflater.from(context).inflate(R.layout.view_collapsible_layout, this, false) as ViewGroup
            header.setOnClickListener { toggle() }
            header.setBackgroundColor(headerBackground)
            addView(header, 0)

            removeView(layout)
            container.addView(layout)
            addView(container)

            image = header.findViewById(R.id.view_collapsible_layout_header_image)
            titleView = findViewById(R.id.view_collapsible_layout_header_text)
            if (headerForeground != Color.BLACK) {
                image.setColorFilter(headerForeground, PorterDuff.Mode.SRC_ATOP)
                titleView.setTextColor(headerForeground)
            }

            // Refresh the title (and trigger a custom setter)
            setUp = true
            title = title

            // Check if layout should be collapsed
            if (collapsed)
                layout.gone()
            updateCollapseArrow(collapsed, false)
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * Expands the layout.
     */
    fun expand() {
        if (!collapsed || animationInProgress)
            return
        animationInProgress = true

        container.measure(
                MeasureSpec.makeMeasureSpec(container.width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        performAnimation(0f, container.measuredHeight.toFloat(), false)
    }

    /**
     * Collapses the layout.
     */
    fun collapse() {
        if (collapsed || animationInProgress)
            return
        animationInProgress = true

        performAnimation(container.height.toFloat(), 0f, true)
    }

    /**
     * Toggles the layout expand state.
     */
    fun toggle() {
        if (collapsed)
            expand()
        else
            collapse()
    }

    private fun performAnimation(from: Float, to: Float, newState: Boolean) {
        val duration = if (!staticSpeed) Math.max(from, to) * collapseSpeed / 100 else collapseSpeed
        val animator = ValueAnimator.ofFloat(from, to)
                .setDuration(duration.toLong())

        animator.addUpdateListener {
            val delta = it.animatedValue as Float
            container.layoutParams.height = delta.toInt()
            container.requestLayout()
        }

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator?) {
                animationInProgress = false
                collapsed = newState
            }

            override fun onAnimationRepeat(animation: Animator?) = Unit
            override fun onAnimationCancel(animation: Animator?) = Unit
            override fun onAnimationStart(animation: Animator?) = Unit
        })

        animator.start()
        updateCollapseArrow(newState)
    }

    private fun updateCollapseArrow(collapsed: Boolean, animate: Boolean = true) {
        val newRotation = if (collapsed) 0f else 180f
        if (animate) {
            image.animate()
                    .rotation(newRotation)
                    .setDuration(IMAGE_ANIMATION_DURATION)
                    .start()
        } else
            image.rotation = newRotation
    }

    companion object {
        /** Default speed of expanding/collapsing animation, in milliseconds per 100px of height. */
        private const val DEFAULT_COLLAPSE_SPEED = 400f
        /** Duration of the rotation of the image. */
        private const val IMAGE_ANIMATION_DURATION = 350L
    }
}