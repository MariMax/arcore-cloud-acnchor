/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.codelab.cloudanchor.helpers

import android.app.Activity
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.TextView

/**
 * Helper to manage the sample snackbar. Hides the Android boilerplate code, and exposes simpler
 * methods.
 */
class SnackbarHelper {
    private var messageSnackbar: Snackbar? = null
    private val maxLines = 2
    private var lastMessage = ""

    private enum class DismissBehavior {
        HIDE, SHOW, FINISH
    }

    /** Shows a snackbar with a given message.  */
    fun showMessage(activity: FragmentActivity?, message: String) {
        if (activity != null && !message.isEmpty() && (messageSnackbar == null || lastMessage != message)) {
            lastMessage = message
            show(activity, message, DismissBehavior.HIDE)
        }
    }

    private fun show(
            activity: Activity, message: String, dismissBehavior: DismissBehavior) {
        activity.runOnUiThread {
            messageSnackbar = Snackbar.make(
                    activity.findViewById<View>(android.R.id.content),
                    message, Snackbar.LENGTH_INDEFINITE)
            messageSnackbar!!.view.setBackgroundColor(BACKGROUND_COLOR)
            if (dismissBehavior != DismissBehavior.HIDE) {
                messageSnackbar!!.setAction("Dismiss") { _ -> messageSnackbar!!.dismiss() }
                if (dismissBehavior == DismissBehavior.FINISH) {
                    messageSnackbar!!.addCallback(
                            object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                    super.onDismissed(transientBottomBar, event)
                                    activity.finish()
                                }
                            })
                }
            }
            (messageSnackbar!!
                    .view
                    .findViewById<View>(android.support.design.R.id.snackbar_text) as TextView).maxLines = maxLines
            messageSnackbar!!.show()
        }
    }

    companion object {
        private val BACKGROUND_COLOR = -0x40cdcdce
    }
}
