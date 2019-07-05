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

package com.marimax.ar.core.codelab.cloudanchor

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import com.google.ar.core.Session
import com.google.ar.core.codelab.cloudanchor.helpers.CloudAnchorManager
import com.google.ar.core.codelab.cloudanchor.helpers.ResolveDialogFragment
import com.google.ar.core.codelab.cloudanchor.helpers.SnackbarHelper
import com.google.ar.core.codelab.cloudanchor.helpers.StorageManager
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

/**
 * Main Fragment for the Cloud Anchors Codelab.
 *
 *
 * This is where the AR Session and the Cloud Anchors are managed.
 */
class CloudAnchorFragment : ArFragment() {

    private var arScene: Scene? = null
    private var anchorNode: AnchorNode? = null
    private var andyRenderable: ModelRenderable? = null
    private var cloudAnchorManager = CloudAnchorManager()
    private var snackbarHelper = SnackbarHelper()
    private val storageManager = StorageManager()
    private var resolveButton: Button? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        ModelRenderable.builder()
                .setSource(context!!, R.raw.andy)
                .build()
                .thenAccept { renderable -> andyRenderable = renderable }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate from the Layout XML file.
        val rootView = inflater.inflate(R.layout.cloud_anchor_fragment, container, false)
        val arContainer = rootView.findViewById<LinearLayout>(R.id.ar_container)

        // Call the ArFragment's implementation to get the AR View.
        val arView = super.onCreateView(inflater, arContainer, savedInstanceState)
        arContainer.addView(arView)

        val clearButton = rootView.findViewById<Button>(R.id.clear_button)
        clearButton.setOnClickListener { onClearButtonPressed() }

        resolveButton = rootView.findViewById<Button>(R.id.resolve_button)
        resolveButton?.setOnClickListener { onResolveButtonPressed() }

        arScene = arSceneView.scene
        arScene?.addOnUpdateListener { cloudAnchorManager.onUpdate() }
        setOnTapArPlaneListener { hitResult, _, _ -> onArPlaneTap(hitResult) }
        return rootView
    }

    @Synchronized
    private fun onArPlaneTap(hitResult: HitResult) {
        if (anchorNode != null) {
            // Do nothing if there was already an anchor in the Scene.
            return
        }
        val anchor = hitResult.createAnchor()
        setNewAnchor(anchor)

        resolveButton?.isEnabled = false

        snackbarHelper.showMessage(activity, "Now hosting an anchor... ")

        val callback = object : CloudAnchorManager.CloudAnchorListener {
            override fun onCloudTaskComplete(anchor: Anchor) {
                this@CloudAnchorFragment.onHostedAnchorAvailable(anchor)
            }
        }

        cloudAnchorManager.hostCloudAnchor(arSceneView.session, anchor, callback)
    }

    @Synchronized
    private fun onClearButtonPressed() {
        // Clear the anchor from the scene.
        cloudAnchorManager.clearListeners()
        resolveButton?.isEnabled = true
        setNewAnchor(null)
    }

    @Synchronized
    private fun onResolveButtonPressed() {
        val callback = object : ResolveDialogFragment.OkListener {
            override fun onOkPressed(dialogValue: Int) {
                this@CloudAnchorFragment.onShortCodeEntered(dialogValue);
            }
        }

        val resolveDialog = ResolveDialogFragment.createWithOkListener(callback)
        resolveDialog.show(fragmentManager, "Resolve")
    }

    // Modify the renderables when a new anchor is available.
    @Synchronized
    private fun setNewAnchor(anchor: Anchor?) {
        if (anchorNode != null) {
            // If an AnchorNode existed before, remove and nullify it.
            arScene!!.removeChild(anchorNode!!)
            anchorNode = null
        }
        if (anchor != null) {
            if (andyRenderable == null) {
                // Display an error message if the renderable model was not available.
                val toast = Toast.makeText(context, "Andy model was not loaded.", Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                return
            }
            // Create the Anchor.
            anchorNode = AnchorNode(anchor)
            arScene!!.addChild(anchorNode!!)

            // Create the transformable andy and add it to the anchor.
            val andy = TransformableNode(transformationSystem)
            andy.setParent(anchorNode)
            andy.renderable = andyRenderable
            andy.select()
        }
    }

    @Synchronized
    private fun onHostedAnchorAvailable(anchor: Anchor) {
        val state = anchor.cloudAnchorState
        if (state == Anchor.CloudAnchorState.SUCCESS) {
            val shortCode = storageManager.nextShortCode(activity)
            storageManager.storeUsingShortCode(activity, shortCode, anchor.cloudAnchorId)
            snackbarHelper.showMessage(activity, "Cloud anchor hosted. ID: $shortCode")
            setNewAnchor(anchor)
        } else {
            snackbarHelper.showMessage(activity, "Error while hosting: $state")
        }
    }

    @Synchronized
    private fun onShortCodeEntered(shortCode: Int) {
        val anchor = storageManager.getCloudAnchorId(activity, shortCode)
        if (anchor == null || anchor.isEmpty()) {
            snackbarHelper.showMessage(activity, "A Cloud Anchor ID for the short code $shortCode was not found $anchor")
            return
        }
        resolveButton?.isEnabled = false

        val callback = object : CloudAnchorManager.CloudAnchorListener {
            override fun onCloudTaskComplete(anchor: Anchor) {
                this@CloudAnchorFragment.onResolvedAnchorAvailable(anchor, shortCode)
            }
        }

        cloudAnchorManager.resolveCloudAnchor(arSceneView?.session, anchor, callback)
    }

    @Synchronized
    private fun onResolvedAnchorAvailable(anchor: Anchor, shortCode: Int) {
        val state = anchor.cloudAnchorState
        if (state == Anchor.CloudAnchorState.SUCCESS) {
            snackbarHelper.showMessage(activity, "Cloud Anchor resolved for $shortCode")
            setNewAnchor(anchor)
        } else {
            snackbarHelper.showMessage(activity, "Error while resolving anchor with code $shortCode. Error $state")
            resolveButton?.isEnabled = true
        }
    }

    override fun getSessionConfiguration(session: Session?): Config {
        val config = super.getSessionConfiguration(session)
        config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
        return config
    }
}
