package sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.usefulness.testing.screenshot.sample.R

class ScreenshotsSampleActivity : AppCompatActivity() {

    companion object {
        fun newIntent(context: Context, status: Status) = Intent(context, ScreenshotsSampleActivity::class.java).apply {
            putExtra("status", status.ordinal)
        }
    }

    private val status
        get() = intent?.getIntExtra("status", 0)
            ?.let { Status.values()[it] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        val text = findViewById<TextView>(R.id.text_view)
        text.text = status?.name
    }

    enum class Status {
        Success,
        Warning,
        Error,
    }
}
