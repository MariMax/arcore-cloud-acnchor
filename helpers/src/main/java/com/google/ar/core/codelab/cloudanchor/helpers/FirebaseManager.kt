/*
 * Copyright 2019 Google Inc. All Rights Reserved.
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

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

/** Helper class for Firebase storage of cloud anchor IDs.  */
class FirebaseManager
/** Constructor that initializes the Firebase connection.  */
(context: Context) {
    private val rootRef: DatabaseReference

    /** Listener for a new Cloud Anchor ID from the Firebase Database.  */
    interface CloudAnchorIdListener {
        fun onCloudAnchorIdAvailable(cloudAnchorId: String?)
    }

    /** Listener for a new short code from the Firebase Database.  */
    interface ShortCodeListener {
        fun onShortCodeAvailable(shortCode: Int?)
    }

    init {
        val firebaseApp = FirebaseApp.initializeApp(context)
        rootRef = FirebaseDatabase.getInstance(firebaseApp!!).reference.child(KEY_ROOT_DIR)
        DatabaseReference.goOnline()
    }

    /** Gets a new short code that can be used to store the anchor ID.  */
    fun nextShortCode(listener: ShortCodeListener) {
        // Run a transaction on the node containing the next short code available. This increments the
        // value in the database and retrieves it in one atomic all-or-nothing operation.
        rootRef
                .child(KEY_NEXT_SHORT_CODE)
                .runTransaction(
                        object : Transaction.Handler {
                            override fun doTransaction(currentData: MutableData): Transaction.Result {
                                var shortCode = currentData.getValue(Int::class.java)
                                if (shortCode == null) {
                                    // Set the initial short code if one did not exist before.
                                    shortCode = INITIAL_SHORT_CODE - 1
                                }
                                currentData.value = shortCode + 1
                                return Transaction.success(currentData)
                            }

                            override fun onComplete(
                                    error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                                if (!committed) {
                                    Log.e(TAG, "Firebase Error", error!!.toException())
                                    listener.onShortCodeAvailable(null)
                                } else {
                                    listener.onShortCodeAvailable(currentData!!.getValue(Int::class.java))
                                }
                            }
                        })
    }

    /** Stores the cloud anchor ID in the configured Firebase Database.  */
    fun storeUsingShortCode(shortCode: Int, cloudAnchorId: String) {
        rootRef.child(KEY_PREFIX + shortCode).setValue(cloudAnchorId)
    }

    /**
     * Retrieves the cloud anchor ID using a short code. Returns an empty string if a cloud anchor ID
     * was not stored for this short code.
     */
    fun getCloudAnchorId(shortCode: Int, listener: CloudAnchorIdListener) {
        rootRef
                .child(KEY_PREFIX + shortCode)
                .addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                // Listener invoked when the data is successfully read from Firebase.
                                listener.onCloudAnchorIdAvailable(dataSnapshot.value.toString())
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e(
                                        TAG,
                                        "The Firebase operation for getCloudAnchorId was cancelled.",
                                        error.toException())
                                listener.onCloudAnchorIdAvailable(null)
                            }
                        })
    }

    companion object {

        private val TAG = FirebaseManager::class.java.name
        private val KEY_ROOT_DIR = "shared_anchor_codelab_root"
        private val KEY_NEXT_SHORT_CODE = "next_short_code"
        private val KEY_PREFIX = "anchor;"
        private val INITIAL_SHORT_CODE = 142
    }
}
