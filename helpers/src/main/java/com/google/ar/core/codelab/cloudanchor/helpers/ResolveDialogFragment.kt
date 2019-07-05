/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.codelab.cloudanchor.helpers

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams

/** A DialogFragment for the Resolve Dialog Box.  */
class ResolveDialogFragment : DialogFragment() {

    private var shortCodeField: EditText? = null
    private var okListener: OkListener? = null

    /** Functional interface for getting the value entered in this DialogFragment.  */
    interface OkListener {
        /**
         * This method is called by the dialog box when its OK button is pressed.
         *
         * @param dialogValue the long value from the dialog box
         */
        fun onOkPressed(dialogValue: Int)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder
                .setView(createDialogLayout())
                .setTitle("Resolve Anchor")
                .setPositiveButton("Resolve") { _, _ -> onResolvePressed() }
                .setNegativeButton("Cancel") { _, _ -> }
        return builder.create()
    }

    private fun createDialogLayout(): LinearLayout {
        val context = context
        val layout = LinearLayout(context)
        shortCodeField = EditText(context)
        // Only allow numeric input.
        shortCodeField!!.inputType = InputType.TYPE_CLASS_NUMBER
        shortCodeField!!.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        // Set a max length for the input text to avoid overflows when parsing.
        shortCodeField!!.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(MAX_FIELD_LENGTH))
        layout.addView(shortCodeField)
        layout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        return layout
    }

    private fun onResolvePressed() {
        val roomCodeText = shortCodeField!!.text
        if (okListener != null && roomCodeText != null && roomCodeText.length > 0) {
            val longVal = Integer.parseInt(roomCodeText.toString())
            okListener!!.onOkPressed(longVal)
        }
    }

    companion object {

        // The maximum number of characters that can be entered in the EditText.
        private val MAX_FIELD_LENGTH = 6

        fun createWithOkListener(listener: OkListener): ResolveDialogFragment {
            val frag = ResolveDialogFragment()
            frag.okListener = listener
            return frag
        }
    }
}
