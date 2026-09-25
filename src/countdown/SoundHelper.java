package countdown;

import java.awt.Toolkit;

/** Alarm beep: 6 short system beeps on a background thread. */
public final class SoundHelper {

    private SoundHelper() {
    }

    private static volatile boolean stop;
    private static volatile boolean playing;

    public static synchronized void playAlarm() {
        if (playing) {
            return;
        }
        playing = true;
        stop = false;
        Thread t = new Thread(() -> {
            try {
                for (int i = 0; i < 6 && !stop; i++) {
                    try {
                        Toolkit.getDefaultToolkit().beep();
                    } catch (Exception ignored) {
                    }
                    try {
                        Thread.sleep(900);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                playing = false;
            }
        }, "alarm-beep");
        t.setDaemon(true);
        t.start();
    }

    public static void stop() {
        stop = true;
    }
}
