package info.nightscout.androidaps.plugins.constraints.versionChecker

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.BuildConfig
import info.nightscout.androidaps.R
import info.nightscout.androidaps.interfaces.Constraint
import info.nightscout.androidaps.interfaces.ConstraintsInterface
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.plugins.bus.RxBusWrapper
import info.nightscout.androidaps.plugins.general.overview.events.EventNewNotification
import info.nightscout.androidaps.plugins.general.overview.notifications.Notification
import info.nightscout.androidaps.utils.extensions.daysToMillis
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.androidaps.utils.sharedPreferences.SP
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class VersionCheckerPlugin @Inject constructor(
    injector: HasAndroidInjector,
    private val sp: SP,
    resourceHelper: ResourceHelper,
    private val versionCheckerUtils: VersionCheckerUtils,
    val rxBus: RxBusWrapper,
    aapsLogger: AAPSLogger
) : PluginBase(PluginDescription()
    .mainType(PluginType.CONSTRAINTS)
    .neverVisible(true)
    .alwaysEnabled(true)
    .showInList(false)
    .pluginName(R.string.versionChecker),
    aapsLogger, resourceHelper, injector
), ConstraintsInterface {

    enum class GracePeriod(val warning: Long, val old: Long, val veryOld: Long) {
        RELEASE(30, 180, 190),
        RC(1, 7, 14)
    }

    private val gracePeriod: GracePeriod
        get() = if ((BuildConfig.VERSION_NAME.contains("RC", ignoreCase = true))) {
            GracePeriod.RC
        } else {
            GracePeriod.RELEASE
        }

    companion object {
        private val WARN_EVERY: Long
            get() = TimeUnit.DAYS.toMillis(1)
    }

    override fun isClosedLoopAllowed(value: Constraint<Boolean>): Constraint<Boolean> {
        checkWarning()
        versionCheckerUtils.triggerCheckVersion()
        return value
    }

    private fun checkWarning() {
        val now = System.currentTimeMillis()

        if (!sp.contains(R.string.key_last_versionchecker_plugin_warning)) {
            sp.putLong(R.string.key_last_versionchecker_plugin_warning, now)
            return
        }


        if (isOldVersion(gracePeriod.warning.daysToMillis()) && shouldWarnAgain(now)) {
            // store last notification time
            sp.putLong(R.string.key_last_versionchecker_plugin_warning, now)

            //notify
            val message = resourceHelper.gs(R.string.new_version_warning)
            val notification = Notification(Notification.OLDVERSION, message, Notification.NORMAL)
            rxBus.send(EventNewNotification(notification))
        }
    }

    private fun shouldWarnAgain(now: Long) =
        now > sp.getLong(R.string.key_last_versionchecker_plugin_warning, 0) + WARN_EVERY

    private fun isOldVersion(gracePeriod: Long): Boolean {
        val now = System.currentTimeMillis()
        return now > sp.getLong(R.string.key_last_time_this_version_detected, 0) + gracePeriod
    }
}
