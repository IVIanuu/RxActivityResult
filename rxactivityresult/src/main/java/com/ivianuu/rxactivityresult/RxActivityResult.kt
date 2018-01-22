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
import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import io.reactivex.Maybe
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicInteger

/**
 * Wraps activity result calls and returns [Maybe]'s of [ActivityResult]'s
 */
class RxActivityResult(private val activity: Activity) {

    @JvmOverloads
    fun start(intent: Intent,
              options: Bundle? = null): Maybe<ActivityResult> {
        val fragment = RxActivityResultFragment.get(activity)
        return fragment.start(intent, options)
    }

}

/**
 * Represents a activity result
 */
data class ActivityResult(val requestCode: Int,
                          val resultCode: Int,
                          val data: Intent?) {

    fun isOk() = resultCode == Activity.RESULT_OK

    fun isCanceled() = resultCode == Activity.RESULT_CANCELED
}

/**
 * Handles the activity results
 */
class RxActivityResultFragment : Fragment() {

    private val subjects = HashMap<Int, PublishSubject<ActivityResult>>()

    init {
        retainInstance = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!subjects.containsKey(requestCode)) return
        handleActivityResult(requestCode, resultCode, data)
    }

    internal fun start(intent: Intent,
                       options: Bundle? = null): Maybe<ActivityResult> {
        val requestCode = RequestCodeGenerator.generate()

        val subject = PublishSubject.create<ActivityResult>()
        subjects[requestCode] = subject

        startActivityForResult(intent, requestCode, options)

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

    companion object {
        private const val TAG_FRAGMENT = "com.ivianuu.rxactivityresult.RxActivityResultFragment"

        internal fun get(activity: Activity): RxActivityResultFragment {
            val fm = activity.fragmentManager
            var fragment = fm.findFragmentByTag(TAG_FRAGMENT) as RxActivityResultFragment?
            if (fragment == null) {
                fragment = RxActivityResultFragment()
                fm.beginTransaction()
                        .add(fragment, TAG_FRAGMENT)
                        .commitAllowingStateLoss()
                fm.executePendingTransactions()
            }

            return fragment
        }

    }
}

/**
 * Generates request codes
 */
private object RequestCodeGenerator {

    private val seed = AtomicInteger(500)

    fun generate() = seed.incrementAndGet()
}