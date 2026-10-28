package nl.rechtopzitten

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick-settings tile: one tap toggles "Even weg"; when off, a tap starts the day. */
class AwayTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        update()
    }

    override fun onClick() {
        super.onClick()
        when (Store(this).mode) {
            Store.MODE_DESK -> Tracker.goAway(this)
            Store.MODE_AWAY -> Tracker.comeBack(this)
            else -> Tracker.start(this, bySchedule = false)
        }
        update()
    }

    private fun update() {
        val tile = qsTile ?: return
        val mode = Store(this).mode
        tile.label = "Rechtop"
        tile.subtitle = when (mode) {
            Store.MODE_DESK -> "Aan je bureau"
            Store.MODE_AWAY -> "Even weg"
            else -> "Uit"
        }
        tile.state = if (mode == Store.MODE_DESK) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
