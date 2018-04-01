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
import android.content.Intent

/**
 * Represents an activity result
 */
data class ActivityResult(val requestCode: Int,
                          val resultCode: Int,
                          val data: Intent?) {

    /**
     * Returns whether the [resultCode] equals [Activity.RESULT_OK]
     */
    val isOk: Boolean
        get() = resultCode == Activity.RESULT_OK

    /**
     * Returns whether the [resultCode] equals [Activity.RESULT_CANCELED]
     */
    val isCanceled: Boolean
            get() = resultCode == Activity.RESULT_CANCELED
}