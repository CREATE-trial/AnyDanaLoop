package info.nightscout.androidaps.plugins.source

import android.content.Intent
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.db.BgReading
import info.nightscout.androidaps.interfaces.BgSourceInterface
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.plugins.general.nsclient.NSUpload
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.BundleLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.services.Intents
import info.nightscout.androidaps.utils.resources.ResourceHelper
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class XdripPlugin @Inject constructor(
    injector: HasAndroidInjector,
    resourceHelper: ResourceHelper,
    private val nsUpload: NSUpload,
    aapsLogger: AAPSLogger
) : PluginBase(PluginDescription()
    .mainType(PluginType.BGSOURCE)
    .fragmentClass(BGSourceFragment::class.java.name)
    .pluginName(R.string.xdrip)
    .description(R.string.description_source_xdrip),
    aapsLogger, resourceHelper, injector
), BgSourceInterface {

    private var advancedFiltering = false
    private var sensorBatteryLevel = -1

    override fun advancedFilteringSupported(): Boolean {
        return true;
    }


    override fun handleNewData(intent: Intent) {
        if (!isEnabled(PluginType.BGSOURCE)) return
        val bundle = intent.extras ?: return
        val bg = bundle.getDouble(Intents.EXTRA_BG_ESTIMATE)
        if (bg == 0.0 || bg == 39.0) return

        aapsLogger.debug(LTag.BGSOURCE, "Received xDrip data: " + BundleLogger.log(intent.extras))
        val bgReading  = BgReading()

        bgReading.value = ceil(bg)
        bgReading.direction = bundle.getString(Intents.EXTRA_BG_SLOPE_NAME) ?: "??"
        bgReading.date = bundle.getLong(Intents.EXTRA_TIMESTAMP)
        bgReading.raw = ceil(bundle.getDouble(Intents.EXTRA_RAW))

        val source = bundle.getString(Intents.XDRIP_DATA_SOURCE_DESCRIPTION, "no Source specified")

        MainApp.getDbHelper().createIfNotExists(bgReading, "XDRIP")
        setSource(source)

        //Upload to NS by default
        nsUpload.uploadBg(bgReading, "AnyDanaLoop-xDrip-$source")

    }

    private fun setSource(source: String) {
        advancedFiltering = true
    }

    override fun getSensorBatteryLevel(): Int {
        return sensorBatteryLevel
    }
}
