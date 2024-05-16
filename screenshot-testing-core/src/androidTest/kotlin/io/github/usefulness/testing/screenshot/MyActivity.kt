package io.github.usefulness.testing.screenshot

import android.app.Activity

class MyActivity : Activity() {

    var destroyed = false

    public override fun onDestroy() {
        super.onDestroy()
        destroyed = true
    }
}
