@file:Suppress("MemberVisibilityCanBePrivate")

package com.gitlab.drzepka.backbone.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.gitlab.drzepka.backbone.R
import com.gitlab.drzepka.backbone.helper.gone
import java.util.*

/**
 * Dialog containing an RGBA color picker. Be sure to set the [onColorChosenListener] field in order to retrieve
 * the color chosen by user.
 */
class RGBColorPickerDialog(context: Context) : AlertDialog(context), SeekBar.OnSeekBarChangeListener {

    /**
     * Listener that will be run when a color is chosen. The chosen color will be passed as a parameter.
     */
    var onColorChosenListener: ((color: Int) -> Unit)? = null
    /**
     * Color value picked in this dialog.
     */
    var color = 0
        set (value) {
            field = value
            if (updateSeekBars)
                updateSeekBars()
        }

    /**
     * Whether to show alpha seek bar. This should be set before the [show] method is called.
     */
    var showAlpha = false

    private val plane: View
    private val valueText: TextView
    private val seekRed: SeekBar
    private val seekGreen: SeekBar
    private val seekBlue: SeekBar
    private val seekAlpha: SeekBar
    private val textRed: TextView
    private val textGreen: TextView
    private val textBlue: TextView
    private val textAlpha: TextView

    private var updateSeekBars = true

    init {
        // Setup the layout
        @SuppressLint("InflateParams")
        val layout = layoutInflater.inflate(R.layout.dialog_rgb_color_picker, null)
        plane = layout.findViewById(R.id.dialog_rgb_color_picker_plane)
        valueText = layout.findViewById(R.id.dialog_rgb_color_picker_value)
        seekRed = layout.findViewById(R.id.dialog_rgb_color_picker_seek_red)
        seekGreen = layout.findViewById(R.id.dialog_rgb_color_picker_seek_green)
        seekBlue = layout.findViewById(R.id.dialog_rgb_color_picker_seek_blue)
        seekAlpha = layout.findViewById(R.id.dialog_rgb_color_picker_seek_alpha)
        textRed = layout.findViewById(R.id.dialog_rgb_color_picker_text_red)
        textGreen = layout.findViewById(R.id.dialog_rgb_color_picker_text_green)
        textBlue = layout.findViewById(R.id.dialog_rgb_color_picker_text_blue)
        textAlpha = layout.findViewById(R.id.dialog_rgb_color_picker_text_alpha)
        setView(layout)

        // Set default values
        color = Random().nextInt() or 0xff000000.toInt()
        setTitle(R.string.dialog_rgb_color_picker_default_title)

        // Set default buttons
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.ok), { dialog, _ ->
            onColorChosenListener?.invoke(color)
            dialog.dismiss()
        })
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), { dialog, _ ->
            dialog.dismiss()
        })

        seekAlpha.setOnSeekBarChangeListener(this)
        seekRed.setOnSeekBarChangeListener(this)
        seekGreen.setOnSeekBarChangeListener(this)
        seekBlue.setOnSeekBarChangeListener(this)

        seekAlpha.thumb.setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP)
        seekRed.thumb.setColorFilter(0xffff0000.toInt(), PorterDuff.Mode.SRC_ATOP)
        seekGreen.thumb.setColorFilter(0xff00ff00.toInt(), PorterDuff.Mode.SRC_ATOP)
        seekBlue.thumb.setColorFilter(0xff0000ff.toInt(), PorterDuff.Mode.SRC_ATOP)

        // Shift values
        seekAlpha.tag = 24
        seekRed.tag = 16
        seekGreen.tag = 8
        seekBlue.tag = 0
    }

    override fun show() {
        super.show()

        if (!showAlpha) {
            seekAlpha.gone()
            textAlpha.gone()
        }
    }

    /**
     * Set the plane background color to new value.
     */
    private fun updateColor(seekBar: SeekBar) {
        val shift = seekBar.tag as Int
        val component = seekBar.progress shl shift
        val negative = 0xffffffff.toInt() xor (0xff shl shift)
        updateSeekBars = false
        color = (color and negative) or component
        updateSeekBars = true
        plane.setBackgroundColor(color)

        val text = when (shift) {
            24 -> textAlpha
            16 -> textRed
            8 -> textGreen
            else -> textBlue
        }
        text.text = seekBar.progress.toString()
        updateValueText()
    }

    /**
     * Update seek bars positions and plane color.
     */
    private fun updateSeekBars() {
        val alpha = (color and 0xff000000.toInt()) ushr 24
        val red = (color and 0xff0000) ushr 16
        val green = (color and 0xff00) ushr 8
        val blue = color and 0xff

        seekAlpha.progress = alpha
        seekRed.progress = red
        seekGreen.progress = green
        seekBlue.progress = blue

        textAlpha.text = alpha.toString()
        textRed.text = red.toString()
        textGreen.text = green.toString()
        textBlue.text = blue.toString()

        plane.setBackgroundColor(color)
        updateValueText()
    }

    @SuppressLint("SetTextI18n")
    private fun updateValueText() {
        var text = Integer.toHexString(color).padStart(if (showAlpha) 8 else 6, '0')
        if (!showAlpha)
            text = text.substring(2)
        valueText.text = "#$text"
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        updateColor(seekBar)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
    override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
}