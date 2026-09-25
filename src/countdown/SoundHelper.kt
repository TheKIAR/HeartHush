package countdown

import java.awt.Toolkit

/** Alarm beep: 6 short system beeps on a background thread. */
object SoundHelper {

    @Volatile
    private var stop = false

    @Volatile
    private var playing = false

    @Synchronized
    fun playAlarm() {
        if (playing) {
            return
        }
        playing = true
        stop = false
        val t = Thread({
            try {
                for (i in 0 until 6) {
                    if (stop) break
                    try {
                        Toolkit.getDefaultToolkit().beep()
                    } catch (ignored: Exception) {
                    }
                    try {
                        Thread.sleep(900)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            } finally {
                playing = false
            }
        }, "alarm-beep")
        t.isDaemon = true
        t.start()
    }

    fun stop() {
        stop = true
    }
}
