/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.rxactivityresult

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import io.reactivex.Maybe
import io.reactivex.subjects.MaybeSubject

/**
 * Handles the activity results
 */
class RxActivityResultFragment : Fragment(), ActivityResultStarter, Application.ActivityLifecycleCallbacks {

    private val subjects = HashMap<Int, MaybeSubject<ActivityResult>>()

    private val requireActivityActions = ArrayList<(() -> Unit)>()

    private var act: Activity? = null
    private var hasRegisteredCallbacks = false

    init {
        retainInstance = true
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        requireActivityActions.reversed().forEach { it.invoke() }
        requireActivityActions.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!subjects.containsKey(requestCode)) return
        handleActivityResult(requestCode, resultCode, data)
    }

    override fun start(intent: Intent, requestCode: Int): Maybe<ActivityResult> {
        return startForResult(intent, null, requestCode)
    }

    override fun start(intent: Intent, options: Bundle, requestCode: Int): Maybe<ActivityResult> {
        return startForResult(intent, options, requestCode)
    }

    override fun result(requestCode: Int): Maybe<ActivityResult> {
        val subject = subjects[requestCode]
        return subject ?: Maybe.empty()
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
    }
    override fun onActivityStarted(activity: Activity?) {
    }
    override fun onActivityPaused(activity: Activity?) {
    }
    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
    }
    override fun onActivityResumed(activity: Activity?) {
    }
    override fun onActivityStopped(activity: Activity?) {
    }

    override fun onActivityDestroyed(activity: Activity?) {
        if (this.act == activity) {
            act?.application?.unregisterActivityLifecycleCallbacks(this)
            hasRegisteredCallbacks = false
            this.act = null
        }

        activeActivityResultFragments.remove(activity)
    }

    private fun startForResult(
        intent: Intent,
        options: Bundle? = null,
        requestCode: Int
    ): Maybe<ActivityResult> {
        val requestCode = if (requestCode != -1) {
            requestCode
        } else {
            RequestCodeGenerator.generate()
        }

        val subject = MaybeSubject.create<ActivityResult>()
        subjects[requestCode] = subject

        requireActivity { startActivityForResult(intent, requestCode, options) }

        return subject
    }

    private fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        val subject = subjects[requestCode] ?: return
        subject.onSuccess(ActivityResult(requestCode, resultCode, data))
    }

    private fun requireActivity(action: () -> Unit) {
        if (activity != null) {
            action()
        } else {
            requireActivityActions.add(action)
        }
    }

    private fun registerActivityListener(activity: Activity) {
        this.act = activity

        if (!hasRegisteredCallbacks) {
            hasRegisteredCallbacks = true
            activity.application.registerActivityLifecycleCallbacks(this)
            activeActivityResultFragments[activity] = this
        }
    }

    companion object {
        private const val TAG_FRAGMENT = "com.ivianuu.rxactivityresult.RxActivityResultFragment"

        private val activeActivityResultFragments = HashMap<Activity, RxActivityResultFragment>()
        
        internal fun get(activity: FragmentActivity): ActivityResultStarter {
            var activityResultFragment = findInActivity(activity)
            if (activityResultFragment == null) {
                activityResultFragment = RxActivityResultFragment()
                activity.supportFragmentManager
                    .beginTransaction()
                    .add(activityResultFragment, TAG_FRAGMENT)
                    .commit()
            }

            activityResultFragment.registerActivityListener(activity)

            return activityResultFragment
        }

        private fun findInActivity(activity: FragmentActivity): RxActivityResultFragment? {
            var activityResultFragment = activeActivityResultFragments[activity]
            if (activityResultFragment == null) {
                activityResultFragment = activity.supportFragmentManager
                    .findFragmentByTag(TAG_FRAGMENT) as RxActivityResultFragment?
            }

            return activityResultFragment
        }
    }
}