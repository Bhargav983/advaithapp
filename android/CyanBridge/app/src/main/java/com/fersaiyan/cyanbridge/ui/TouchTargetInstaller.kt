package com.fersaiyan.cyanbridge.ui

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.Switch
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

object TouchTargetInstaller {
    private const val EXTRA_TOUCH_DP = 18

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityResumed(activity: Activity) {
                    install(activity)
                }

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
    }

    private fun install(activity: Activity) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val extraTouchPx = activity.dp(EXTRA_TOUCH_DP)

        root.post {
            root.walk { view ->
                if (!view.isImportantTapTarget()) return@walk

                view.expandTouchArea(extraTouchPx)
            }
        }
    }

    private fun View.isImportantTapTarget(): Boolean {
        return this is Button ||
            this is MaterialButton ||
            this is Switch ||
            this is SwitchMaterial ||
            this is CheckBox ||
            this is RadioButton ||
            this is CompoundButton
    }

    private fun View.expandTouchArea(extraPx: Int) {
        val parentView = parent as? View ?: return
        parentView.post {
            val bounds = Rect()
            getHitRect(bounds)
            bounds.inset(-extraPx, -extraPx)

            val existing = parentView.touchDelegate
            parentView.touchDelegate = if (existing is MultiTouchDelegate) {
                existing.apply { addDelegate(TouchDelegate(bounds, this@expandTouchArea)) }
            } else {
                MultiTouchDelegate(parentView).apply {
                    existing?.let { addDelegate(it) }
                    addDelegate(TouchDelegate(bounds, this@expandTouchArea))
                }
            }
        }
    }

    private fun ViewGroup.walk(block: (View) -> Unit) {
        block(this)
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child is ViewGroup) {
                child.walk(block)
            } else {
                block(child)
            }
        }
    }

    private fun Activity.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private class MultiTouchDelegate(delegateView: View) : TouchDelegate(Rect(), delegateView) {
        private val delegates = mutableListOf<TouchDelegate>()

        fun addDelegate(delegate: TouchDelegate) {
            delegates.add(delegate)
        }

        override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
            return delegates.any { it.onTouchEvent(event) }
        }
    }
}
