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

package com.ivianuu.rxactivityresult.sample

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.ivianuu.rxactivityresult.ActivityResult
import com.ivianuu.rxactivityresult.RequestCodeGenerator
import com.ivianuu.rxactivityresult.RxActivityResult
import io.reactivex.disposables.Disposable

class MainActivity : AppCompatActivity() {

    private val activityResultStarter by lazy { RxActivityResult.get(this) }

    private var disposable: Disposable? = null

    private var requestCode = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            requestCode = savedInstanceState.getInt("request_code", -1)
        }

        if (requestCode != -1) {
            activityResultStarter.result(requestCode)
                .subscribe(::onResult)
                .let { disposable = it }
        }

        findViewById<Button>(R.id.request).setOnClickListener {
            requestCode = RequestCodeGenerator.generate()
            activityResultStarter.start(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), requestCode)
                    .subscribe(::onResult)
                    .let { disposable = it }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (requestCode != -1) {
            outState.putInt("request_code", requestCode)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

    private fun onResult(result: ActivityResult) {
        requestCode = -1
        Log.d("RxActivityResult", "on result $result")
    }
}
