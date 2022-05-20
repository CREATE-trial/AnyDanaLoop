package info.nightscout.androidaps.dependencyInjection

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap
import info.nightscout.androidaps.danaRKorean.DanaRKoreanPlugin
import info.nightscout.androidaps.danaRv2.DanaRv2Plugin
import info.nightscout.androidaps.danar.DanaRPlugin
import info.nightscout.androidaps.danars.DanaRSPlugin
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.plugins.aps.loop.LoopPlugin
import info.nightscout.androidaps.plugins.aps.openAPSSMB.OpenAPSSMBPlugin
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin
import info.nightscout.androidaps.plugins.constraints.dstHelper.DstHelperPlugin
import info.nightscout.androidaps.plugins.constraints.stages.StagesPlugin
import info.nightscout.androidaps.plugins.constraints.safety.SafetyPlugin
import info.nightscout.androidaps.plugins.constraints.signatureVerifier.SignatureVerifierPlugin
import info.nightscout.androidaps.plugins.constraints.storage.StorageConstraintPlugin
import info.nightscout.androidaps.plugins.constraints.versionChecker.VersionCheckerPlugin
import info.nightscout.androidaps.plugins.general.actions.ActionsPlugin
import info.nightscout.androidaps.plugins.general.automation.AutomationPlugin
import info.nightscout.androidaps.plugins.general.dataBroadcaster.DataBroadcastPlugin
import info.nightscout.androidaps.plugins.general.food.FoodPlugin
import info.nightscout.androidaps.plugins.general.maintenance.MaintenancePlugin
import info.nightscout.androidaps.plugins.general.nsclient.NSClientPlugin
import info.nightscout.androidaps.plugins.general.openhumans.OpenHumansUploader
import info.nightscout.androidaps.plugins.general.overview.OverviewPlugin
import info.nightscout.androidaps.plugins.general.persistentNotification.PersistentNotificationPlugin
import info.nightscout.androidaps.plugins.general.smsCommunicator.SmsCommunicatorPlugin
import info.nightscout.androidaps.plugins.general.wear.WearPlugin
import info.nightscout.androidaps.plugins.general.xdripStatusline.StatusLinePlugin
import info.nightscout.androidaps.plugins.insulin.InsulinLyumjevPlugin
import info.nightscout.androidaps.plugins.insulin.InsulinOrefRapidActingPlugin
import info.nightscout.androidaps.plugins.insulin.InsulinOrefUltraRapidActingPlugin
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.IobCobCalculatorPlugin
import info.nightscout.androidaps.plugins.profile.local.LocalProfilePlugin
import info.nightscout.androidaps.plugins.profile.ns.NSProfilePlugin
import info.nightscout.androidaps.plugins.pump.combo.ComboPlugin
import info.nightscout.androidaps.plugins.pump.insight.LocalInsightPlugin
import info.nightscout.androidaps.plugins.pump.medtronic.MedtronicPumpPlugin
import info.nightscout.androidaps.plugins.pump.omnipod.OmnipodPumpPlugin
import info.nightscout.androidaps.plugins.pump.virtual.VirtualPumpPlugin
import info.nightscout.androidaps.plugins.sensitivity.SensitivityOref1Plugin
import info.nightscout.androidaps.plugins.source.*
import info.nightscout.androidaps.plugins.treatments.TreatmentsPlugin
import javax.inject.Qualifier

@Module
abstract class PluginsModule {

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(0)
    abstract fun bindOverviewPlugin(plugin: OverviewPlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(10)
    abstract fun bindIobCobCalculatorPlugin(plugin: IobCobCalculatorPlugin): PluginBase

    @Binds
    @APS
    @IntoMap
    @IntKey(20)
    abstract fun bindActionsPlugin(plugin: ActionsPlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(30)
    abstract fun bindInsulinOrefRapidActingPlugin(plugin: InsulinOrefRapidActingPlugin): PluginBase

    @Binds
    @Ultrarapid
    @IntoMap
    @IntKey(40)
    abstract fun bindInsulinOrefUltraRapidActingPlugin(plugin: InsulinOrefUltraRapidActingPlugin): PluginBase

    @Binds
    @Engineer
    @IntoMap
    @IntKey(42)
    abstract fun bindInsulinLyumjevPlugin(plugin: InsulinLyumjevPlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(80)
    abstract fun bindSensitivityOref1Plugin(plugin: SensitivityOref1Plugin): PluginBase

    @Binds
    @PumpDriver
    @IntoMap
    @IntKey(90)
    abstract fun bindDanaRPlugin(plugin: DanaRPlugin): PluginBase

    @Binds
    @PumpDriver
    @IntoMap
    @IntKey(100)
    abstract fun bindDanaRKoreanPlugin(plugin: DanaRKoreanPlugin): PluginBase

    @Binds
    @PumpDriver
    @IntoMap
    @IntKey(110)
    abstract fun bindDanaRv2Plugin(plugin: DanaRv2Plugin): PluginBase

    @Binds
    @PumpDriver
    @IntoMap
    @IntKey(120)
    abstract fun bindDanaRSPlugin(plugin: DanaRSPlugin): PluginBase

    @Binds
    @Engineer
    @IntoMap
    @IntKey(130)
    abstract fun bindLocalInsightPlugin(plugin: LocalInsightPlugin): PluginBase

    @Binds
    @Engineer
    @IntoMap
    @IntKey(140)
    abstract fun bindComboPlugin(plugin: ComboPlugin): PluginBase

    @Binds
    @Medtronic
    @IntoMap
    @IntKey(150)
    abstract fun bindMedtronicPumpPlugin(plugin: MedtronicPumpPlugin): PluginBase

    @Binds
    @Engineer
    @IntoMap
    @IntKey(155)
    abstract fun bindOmnipodPumpPlugin(plugin: OmnipodPumpPlugin): PluginBase

    @Binds
    @VirtualPump
    @IntoMap
    @IntKey(170)
    abstract fun bindVirtualPumpPlugin(plugin: VirtualPumpPlugin): PluginBase

    @Binds
    @APS
    @IntoMap
    @IntKey(190)
    abstract fun bindLoopPlugin(plugin: LoopPlugin): PluginBase


    @Binds
    @APS
    @IntoMap
    @IntKey(220)
    abstract fun bindOpenAPSSMBPlugin(plugin: OpenAPSSMBPlugin): PluginBase

    @Binds
    @Engineer
    @IntoMap
    @IntKey(230)
    abstract fun bindNSProfilePlugin(plugin: NSProfilePlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(240)
    abstract fun bindLocalProfilePlugin(plugin: LocalProfilePlugin): PluginBase

    @Binds
    @Engineer
    @IntoMap
    @IntKey(250)
    abstract fun bindAutomationPlugin(plugin: AutomationPlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(260)
    abstract fun bindTreatmentsPlugin(plugin: TreatmentsPlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(265)
    abstract fun bindSafetyPlugin(plugin: SafetyPlugin): PluginBase

    @Binds
    @NotNSClient
    @IntoMap
    @IntKey(270)
    abstract fun bindVersionCheckerPlugin(plugin: VersionCheckerPlugin): PluginBase

    @Binds
    @Engineer
    @IntoMap
    @IntKey(280)
    abstract fun bindSmsCommunicatorPlugin(plugin: SmsCommunicatorPlugin): PluginBase

    @Binds
    @APS
    @IntoMap
    @IntKey(290)
    abstract fun bindStorageConstraintPlugin(plugin: StorageConstraintPlugin): PluginBase

    @Binds
    @APS
    @IntoMap
    @IntKey(300)
    abstract fun bindSignatureVerifierPlugin(plugin: SignatureVerifierPlugin): PluginBase

    @Binds
    @APS
    @IntoMap
    @IntKey(310)
    abstract fun bindStagesPlugin(plugin: StagesPlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(320)
    abstract fun bindFoodPlugin(plugin: FoodPlugin): PluginBase

    @Binds
    @APS
    @IntoMap
    @IntKey(330)
    abstract fun bindWearPlugin(plugin: WearPlugin): PluginBase

    @Binds
    @Engineer
    @IntoMap
    @IntKey(340)
    abstract fun bindStatusLinePlugin(plugin: StatusLinePlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(350)
    abstract fun bindPersistentNotificationPlugin(plugin: PersistentNotificationPlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(360)
    abstract fun bindNSClientPlugin(plugin: NSClientPlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(370)
    abstract fun bindMaintenancePlugin(plugin: MaintenancePlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(380)
    abstract fun bindDstHelperPlugin(plugin: DstHelperPlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(390)
    abstract fun bindDataBroadcastPlugin(plugin: DataBroadcastPlugin): PluginBase

    @Binds
    @Xdrip
    @IntoMap
    @IntKey(400)
    abstract fun bindXdripPlugin(plugin: XdripPlugin): PluginBase

    @Binds
    @NSBG
    @IntoMap
    @IntKey(410)
    abstract fun bindNSClientSourcePlugin(plugin: NSClientSourcePlugin): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(440)
    abstract fun bindDexcomPlugin(plugin: DexcomPlugin): PluginBase
    @Binds
    @Engineer
    @IntoMap
    @IntKey(470)
    abstract fun bindRandomBgPlugin(plugin: RandomBgPlugin): PluginBase

    @Binds
    @Engineer
    @IntoMap
    @IntKey(480)
    abstract fun bindOpenHumansPlugin(plugin: OpenHumansUploader): PluginBase

    @Binds
    @AllConfigs
    @IntoMap
    @IntKey(490)
    abstract fun bindConfigBuilderPlugin(plugin: ConfigBuilderPlugin): PluginBase

    @Qualifier
    annotation class Engineer

    @Qualifier
    annotation class Xdrip

    @Qualifier
    annotation class NSBG

    @Qualifier
    annotation class Medtronic

    @Qualifier
    annotation class VirtualPump

    @Qualifier
    annotation class Ultrarapid

    @Qualifier
    annotation class AllConfigs

    @Qualifier
    annotation class PumpDriver

    @Qualifier
    annotation class NotNSClient

    @Qualifier
    annotation class APS

}
