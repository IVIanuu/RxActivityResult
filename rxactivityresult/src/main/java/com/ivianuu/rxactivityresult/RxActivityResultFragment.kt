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
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.reactivex.Maybe
import io.reactivex.subjects.PublishSubject

/**
 * Handles the activity results
 * This class is internal and you should not worry about it
 */
class RxActivityResultFragment : Fragment(), ActivityResultStarter, Application.ActivityLifecycleCallbacks {

    private val subjects = HashMap<Int, PublishSubject<ActivityResult>>()

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

    override fun start(intent: Intent): Maybe<ActivityResult> {
        return startForResult(intent)
    }

    override fun start(intent: Intent, options: Bundle): Maybe<ActivityResult> {
        return startForResult(intent, options)
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity?) {}
    override fun onActivityPaused(activity: Activity?) {}
    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
    override fun onActivityResumed(activity: Activity?) {}
    override fun onActivityStopped(activity: Activity?) {}

    override fun onActivityDestroyed(activity: Activity?) {
        if (this.act == activity) {
            act?.application?.unregisterActivityLifecycleCallbacks(this)
            hasRegisteredCallbacks = false
            this.act = null
        }

        activeActivityResultFragments.remove(activity)
    }

    private fun startForResult(intent: Intent,
                               options: Bundle? = null): Maybe<ActivityResult> {
        val requestCode = RequestCodeGenerator.generate()

        val subject = PublishSubject.create<ActivityResult>()
        subjects[requestCode] = subject

        requireActivity { startActivityForResult(intent, requestCode, options) }

        return subject
            .take(1)
            .singleElement()
    }

    private fun handleActivityResult(requestCode: Int,
                                     resultCode: Int,
                                     data: Intent?) {
        val subject = subjects.remove(requestCode) ?: return
        subject.onNext(ActivityResult(requestCode, resultCode, data))
        subject.onComplete()
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
        
        internal fun get(activity: Activity): ActivityResultStarter {
            var activityResultFragment = findInActivity(activity)
            if (activityResultFragment == null) {
                activityResultFragment = RxActivityResultFragment()
                activity.fragmentManager.beginTransaction()
                    .add(activityResultFragment, TAG_FRAGMENT).commit()
            }

            activityResultFragment.registerActivityListener(activity)

            return activityResultFragment
        }

        private fun findInActivity(activity: Activity): RxActivityResultFragment? {
            var activityResultFragment = activeActivityResultFragments[activity]
            if (activityResultFragment == null) {
                activityResultFragment = activity.fragmentManager
                    .findFragmentByTag(TAG_FRAGMENT) as RxActivityResultFragment?
            }

            return activityResultFragment
        }
    }
}