package info.nightscout.androidaps.historyBrowser

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.interfaces.ActivePluginProvider
import info.nightscout.androidaps.interfaces.ProfileFunction
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.plugins.bus.RxBusWrapper
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.IobCobCalculatorPlugin
import info.nightscout.androidaps.plugins.sensitivity.SensitivityOref1Plugin
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.androidaps.utils.sharedPreferences.SP
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IobCobCalculatorPluginHistory @Inject constructor(
    injector: HasAndroidInjector,
    aapsLogger: AAPSLogger,
    rxBus: RxBusWrapper,
    sp: SP,
    resourceHelper: ResourceHelper,
    profileFunction: ProfileFunction,
    activePlugin: ActivePluginProvider,
    treatmentsPluginHistory: TreatmentsPluginHistory,
    sensitivityOref1Plugin: SensitivityOref1Plugin,
    fabricPrivacy: FabricPrivacy,
    dateUtil: DateUtil
) : IobCobCalculatorPlugin(injector, aapsLogger, rxBus, sp, resourceHelper, profileFunction,
    activePlugin, treatmentsPluginHistory, sensitivityOref1Plugin, fabricPrivacy, dateUtil) {

    override fun onStart() { // do not attach to rxbus
    }
}
