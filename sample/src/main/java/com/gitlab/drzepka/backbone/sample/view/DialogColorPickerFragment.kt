package com.gitlab.drzepka.backbone.sample.view

import android.os.Bundle
import android.support.v4.app.Fragment
import android.widget.Toast
import com.gitlab.drzepka.backbone.dialog.RGBColorPickerDialog

class DialogColorPickerFragment : Fragment() {

    private val dialog by lazy { RGBColorPickerDialog(context) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialog.onColorChosenListener = {
            Toast.makeText(activity, "Chosen color: " + Integer.toHexString(it), Toast.LENGTH_LONG).show()
        }
        dialog.show()
    }
}