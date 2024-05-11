package sample

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dev.chrisbanes.insetter.applyInsetter
import io.github.usefulness.testing.screenshot.sample.R

class ScreenshotsSampleActivity : AppCompatActivity() {

    private val status
        get() =
            intent
                ?.getIntExtra("status", 0)
                ?.let { Status.entries[it] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        val text = findViewById<TextView>(R.id.text_view)
        text.text = status?.name

        findViewById<View>(R.id.appBar).applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }
        findViewById<View>(R.id.fab).applyInsetter {
            type(navigationBars = true) {
                margin()
            }
        }
    }

    enum class Status {
        Success,
        Warning,
        Error,
    }
}
