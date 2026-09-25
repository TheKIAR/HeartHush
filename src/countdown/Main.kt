package countdown

import javax.swing.SwingUtilities

/** Entry point: load events, open the main window. */
object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val store = EventStore()
        store.load()
        val auth = AuthService()
        SwingUtilities.invokeLater {
            val frame = MainFrame(store, auth)
            frame.isVisible = true
        }
    }
}
