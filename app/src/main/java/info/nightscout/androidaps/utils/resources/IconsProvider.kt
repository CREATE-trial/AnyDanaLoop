package info.nightscout.androidaps.utils.resources

import info.nightscout.androidaps.Config
import info.nightscout.androidaps.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconsProvider @Inject constructor(private val config: Config) {

    fun getIcon(): Int =
        when {
            config.NSCLIENT    -> R.mipmap.ic_yellowowl
            config.PUMPCONTROL -> R.mipmap.ic_monitor
            else               -> R.mipmap.ic_launcher
        }

    fun getBitmapIcon(): Int =
        when {
            config.PUMPCONTROL -> R.mipmap.ic_monitor_bitmap
            else               -> R.mipmap.ic_launcher_bitmap
        }

    fun getNotificationIcon(): Int =
        when {
            config.NSCLIENT    -> R.drawable.ic_notif_nsclient
            config.PUMPCONTROL -> R.drawable.ic_notif_monitor
            else               -> R.drawable.ic_notif_adl
        }
}