package info.nightscout.androidaps.setupwizard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.Config
import info.nightscout.androidaps.Constants
import info.nightscout.androidaps.R
import info.nightscout.androidaps.activities.PreferencesActivity
import info.nightscout.androidaps.dialogs.ProfileSwitchDialog
import info.nightscout.androidaps.events.EventConfigBuilderChange
import info.nightscout.androidaps.events.EventPumpStatusChanged
import info.nightscout.androidaps.interfaces.ActivePluginProvider
import info.nightscout.androidaps.interfaces.CommandQueueProvider
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.interfaces.ProfileFunction
import info.nightscout.androidaps.plugins.aps.loop.LoopPlugin
import info.nightscout.androidaps.plugins.bus.RxBusWrapper
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin
import info.nightscout.androidaps.plugins.constraints.stages.StagesFragment
import info.nightscout.androidaps.plugins.constraints.stages.StagesPlugin
import info.nightscout.androidaps.plugins.general.maintenance.ImportExportPrefs
import info.nightscout.androidaps.plugins.general.nsclient.NSClientPlugin
import info.nightscout.androidaps.plugins.general.nsclient.events.EventNSClientStatus
import info.nightscout.androidaps.plugins.general.nsclient.services.NSClientService
import info.nightscout.androidaps.plugins.profile.local.LocalProfileFragment
import info.nightscout.androidaps.plugins.profile.local.LocalProfilePlugin
import info.nightscout.androidaps.plugins.profile.ns.NSProfileFragment
import info.nightscout.androidaps.plugins.profile.ns.NSProfilePlugin
import info.nightscout.androidaps.plugins.pump.common.events.EventRileyLinkDeviceStatusChange
import info.nightscout.androidaps.plugins.pump.omnipod.OmnipodPumpPlugin
import info.nightscout.androidaps.setupwizard.elements.*
import info.nightscout.androidaps.setupwizard.events.EventSWUpdate
import info.nightscout.androidaps.utils.AndroidPermission
import info.nightscout.androidaps.utils.CryptoUtil
import info.nightscout.androidaps.utils.buildHelper.BuildHelper
import info.nightscout.androidaps.utils.extensions.isRunningTest
import info.nightscout.androidaps.utils.locale.LocaleHelper.update
import info.nightscout.androidaps.utils.protection.ProtectionCheck
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.androidaps.utils.sharedPreferences.SP
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SWDefinition @Inject constructor(
    injector: HasAndroidInjector,
    private val rxBus: RxBusWrapper,
    private val context: Context,
    resourceHelper: ResourceHelper,
    private val sp: SP,
    private val profileFunction: ProfileFunction,
    private val localProfilePlugin: LocalProfilePlugin,
    private val activePlugin: ActivePluginProvider,
    private val commandQueue: CommandQueueProvider,
    private val stagesPlugin: StagesPlugin,
    private val configBuilderPlugin: ConfigBuilderPlugin,
    private val loopPlugin: LoopPlugin,
    private val nsClientPlugin: NSClientPlugin,
    private val nsProfilePlugin: NSProfilePlugin,
    private val protectionCheck: ProtectionCheck,
    private val importExportPrefs: ImportExportPrefs,
    private val androidPermission: AndroidPermission,
    private val cryptoUtil: CryptoUtil,
    private val config: Config,
    private val buildHelper: BuildHelper
) {

    lateinit var activity: AppCompatActivity
    private val screens: MutableList<SWScreen> = ArrayList()

    fun getScreens(): List<SWScreen> {
        return screens
    }

    private fun add(newScreen: SWScreen?): SWDefinition {
        if (newScreen != null) screens.add(newScreen)
        return this
    }

    private val screenSetupWizard = SWScreen(injector, R.string.nav_setupwizard)
        .add(SWInfotext(injector)
            .label(R.string.welcometosetupwizard))
    private val screencheckAndroidVersion = SWScreen(injector, R.string.sorry)
        .add(SWBreak(injector))
        .validator(SWValidator { false })

    private val screenUnlock = SWScreen(injector, R.string.password)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(resourceHelper.gs(R.string.unlockapp)))
        .add(SWBreak(injector))
        .add(SWEditPassword(injector)
            .validator(SWTextValidator { text: String -> text == Constants.APP_Password })
            .preferenceId(R.string.key_app_password)
            .label(R.string.app_password_dialogtitle))
        .validator(SWValidator { sp.getString(R.string.key_app_password , "") == Constants.APP_Password })
    private val screenUnits = SWScreen(injector, R.string.units)
        .skippable(false)
        .add(SWRadioButton(injector)
            .option(R.array.unitsArray, R.array.unitsValues)
            .preferenceId(R.string.key_units).label(R.string.units)
            .comment(R.string.setupwizard_units_prompt))
        .validator(SWValidator { sp.contains(R.string.key_units) })
    private val displaySettings = SWScreen(injector, R.string.wear_display_settings)
        .skippable(false)
        .add(SWEditNumberWithUnits(injector, Constants.LOWMARK * Constants.MGDL_TO_MMOLL, 3.0, 8.0)
            .preferenceId(R.string.key_low_mark)
            .updateDelay(5)
            .label(R.string.low_mark)
            .comment(R.string.low_mark_comment))
        .add(SWBreak(injector))
        .add(SWEditNumberWithUnits(injector, Constants.HIGHMARK * Constants.MGDL_TO_MMOLL, 5.0, 20.0)
            .preferenceId(R.string.key_high_mark)
            .updateDelay(5)
            .label(R.string.high_mark)
            .comment(R.string.high_mark_comment))
    private val screenPermissionBattery = SWScreen(injector, R.string.permission)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(resourceHelper.gs(R.string.needwhitelisting, resourceHelper.gs(R.string.app_name))))
        .add(SWBreak(injector))
        .add(SWButton(injector)
            .text(R.string.askforpermission)
            .visibility(SWValidator { androidPermission.permissionNotGranted(context, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) })
            .action(Runnable { androidPermission.askForPermission(activity, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, AndroidPermission.CASE_BATTERY) }))
        .visibility(SWValidator { androidPermission.permissionNotGranted(activity, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) })
        .validator(SWValidator { !androidPermission.permissionNotGranted(activity, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) })
    private val screenPermissionBt = SWScreen(injector, R.string.permission)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(resourceHelper.gs(R.string.needlocationpermission)))
        .add(SWBreak(injector))
        .add(SWButton(injector)
            .text(R.string.askforpermission)
            .visibility(SWValidator { androidPermission.permissionNotGranted(activity, Manifest.permission.ACCESS_FINE_LOCATION) })
            .action(Runnable { androidPermission.askForPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION, AndroidPermission.CASE_LOCATION) }))
        .visibility(SWValidator { androidPermission.permissionNotGranted(activity, Manifest.permission.ACCESS_FINE_LOCATION) })
        .validator(SWValidator { !androidPermission.permissionNotGranted(activity, Manifest.permission.ACCESS_FINE_LOCATION) })
    private val screenPermissionStore = SWScreen(injector, R.string.permission)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(resourceHelper.gs(R.string.needstoragepermission)))
        .add(SWBreak(injector))
        .add(SWButton(injector)
            .text(R.string.askforpermission)
            .visibility(SWValidator { androidPermission.permissionNotGranted(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) })
            .action(Runnable { androidPermission.askForPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, AndroidPermission.CASE_STORAGE) }))
        .visibility(SWValidator { androidPermission.permissionNotGranted(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) })
        .validator(SWValidator { !androidPermission.permissionNotGranted(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) })
    private val screenPermissionWindow = SWScreen(injector, R.string.permission)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(resourceHelper.gs(R.string.needsystemwindowpermission)))
        .add(SWBreak(injector))
        .add(SWButton(injector)
            .text(R.string.askforpermission)
            .visibility(SWValidator { !Settings.canDrawOverlays(activity) })
            .action(Runnable { activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.packageName)))}))
        .visibility(SWValidator { !Settings.canDrawOverlays(activity) })
        .validator(SWValidator { Settings.canDrawOverlays(activity)  })
    private val screenImport = SWScreen(injector, R.string.nav_import)
        .add(SWInfotext(injector)
            .label(R.string.storedsettingsfound))
        .add(SWBreak(injector))
        .add(SWButton(injector)
            .text(R.string.nav_import)
            .action(Runnable { importExportPrefs.importSharedPreferences(activity) }))
        .visibility(SWValidator { importExportPrefs.prefsFileExists() && !androidPermission.permissionNotGranted(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) })
    private val screenNsClient = SWScreen(injector, R.string.nsclientinternal_title)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(R.string.nsclientinfotext))
        .add(SWBreak(injector))
        .add(SWButton(injector)
            .text(R.string.enable_nsclient)
            .action(Runnable {
                nsClientPlugin.setPluginEnabled(PluginType.GENERAL, true)
                nsClientPlugin.setFragmentVisible(PluginType.GENERAL, true)
                configBuilderPlugin.processOnEnabledCategoryChanged(nsClientPlugin, PluginType.GENERAL)
                configBuilderPlugin.storeSettings("SetupWizard")
                rxBus.send(EventConfigBuilderChange())
                rxBus.send(EventSWUpdate(true))
            })
            .visibility(SWValidator { !nsClientPlugin.isEnabled(PluginType.GENERAL) }))
        .add(SWEditUrl(injector)
            .preferenceId(R.string.key_nsclientinternal_url)
            .updateDelay(5)
            .label(R.string.nsclientinternal_url_title)
            .comment(R.string.nsclientinternal_url_dialogmessage))
        .add(SWEditString(injector)
            .validator(SWTextValidator { text: String -> text.length >= 12 })
            .preferenceId(R.string.key_nsclientinternal_api_secret)
            .updateDelay(5)
            .label(R.string.nsclientinternal_secret_dialogtitle)
            .comment(R.string.nsclientinternal_secret_dialogmessage))
        .add(SWBreak(injector))
        .add(SWEventListener(injector, EventNSClientStatus::class.java)
            .label(R.string.status)
            .initialStatus(nsClientPlugin.status)
        )
        .add(SWBreak(injector))
        .validator(SWValidator { nsClientPlugin.nsClientService != null && NSClientService.isConnected && NSClientService.hasWriteAuth })
        .visibility(SWValidator { !(nsClientPlugin.nsClientService != null && NSClientService.isConnected && NSClientService.hasWriteAuth) })
    private val screenPatientName = SWScreen(injector, R.string.patient_name)
        .skippable(true)
        .add(SWInfotext(injector)
            .label(R.string.patient_name_summary))
        .add(SWEditString(injector)
            .validator(SWTextValidator(String::isNotEmpty))
            .preferenceId(R.string.key_patient_name))
    private val screenMasterPassword = SWScreen(injector, R.string.master_password)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(R.string.master_password))
        .add(SWEditEncryptedPassword(injector, cryptoUtil)
            .preferenceId(R.string.key_master_password))
        .add(SWBreak(injector))
        .add(SWInfotext(injector)
            .label(R.string.master_password_summary))
        .validator(SWValidator { !cryptoUtil.checkPassword("", sp.getString(R.string.key_master_password, "")) })
    private val screenTreatmentSafety = SWScreen(injector, R.string.treatmentssafety_title)
        .skippable(false)
        .add(SWBreak(injector))
        .add(SWRadioButton(injector)
            .option(R.array.ageArray, R.array.ageValues)
            .preferenceId(R.string.key_age)
            .label(R.string.patientage)
            .comment(R.string.patientage_summary))
        .add(SWBreak(injector))
        .add(SWEditNumber(injector, Constants.defaultmaxBolus, Constants.limitminmaxBolus, Constants.limitmaxmaxBolus)
            .preferenceId(R.string.key_treatmentssafety_maxbolus)
            .updateDelay(2)
            .label(R.string.treatmentssafety_maxbolus_title))
        .add(SWEditNumber(injector, Constants.defaultmaxCarbs, Constants.limitminmaxCarbs, Constants.limitmaxmaxCarbs)
            .preferenceId(R.string.key_treatmentssafety_maxcarbs)
            .updateDelay(2)
            .label(R.string.treatmentssafety_maxcarbs_title))
        .validator(SWValidator { sp.contains(R.string.key_age) })
        //.validator(SWValidator {(sp.getDouble(R.string.key_treatmentssafety_maxbolus,Constants.defaultmaxBolus) <= Constants.limitmaxmaxBolus) && (sp.getDouble(R.string.key_treatmentssafety_maxbolus,Constants.defaultmaxBolus) >= Constants.limitminmaxBolus)})
    private val screenInsulin = SWScreen(injector, R.string.configbuilder_insulin)
        .skippable(false)
        .add(SWPlugin(injector)
            .option(PluginType.INSULIN, R.string.configbuilder_insulin_description)
            .makeVisible(false)
            .label(R.string.configbuilder_insulin))
        .add(SWBreak(injector))
        .add(SWInfotext(injector)
            .label(R.string.diawarning))
        .add(SWBreak(injector))
        .add(SWButton(injector)
            .text(R.string.insulinsourcesetup)
            .action(Runnable {
                val plugin = activePlugin.activeInsulin as PluginBase
                protectionCheck.queryProtection(activity, ProtectionCheck.Protection.PREFERENCES, Runnable {
                    val i = Intent(activity, PreferencesActivity::class.java)
                    i.putExtra("id", plugin.preferencesId)
                    activity.startActivity(i)
                }, null)
            })
            .visibility(SWValidator { (activePlugin.activeInsulin as PluginBase).preferencesId > 0 }))
    private val screenBgSource = SWScreen(injector, R.string.configbuilder_bgsource)
        .skippable(false)
        .add(SWPlugin(injector)
            .option(PluginType.BGSOURCE, R.string.configbuilder_bgsource_description)
            .label(R.string.configbuilder_bgsource))
        .add(SWBreak(injector))
        .add(SWButton(injector)
            .text(R.string.bgsourcesetup)
            .action(Runnable {
                val plugin = activePlugin.activeBgSource as PluginBase
                protectionCheck.queryProtection(activity, ProtectionCheck.Protection.PREFERENCES, Runnable {
                    val i = Intent(activity, PreferencesActivity::class.java)
                    i.putExtra("id", plugin.preferencesId)
                    activity.startActivity(i)
                }, null)
            })
            .visibility(SWValidator { (activePlugin.activeBgSource as PluginBase).preferencesId > 0 }))
    private val screenProfile = SWScreen(injector, R.string.configbuilder_profile)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(R.string.setupwizard_profile_description))
        .add(SWBreak(injector))
        .add(SWPlugin(injector)
            .option(PluginType.PROFILE, R.string.configbuilder_profile_description)
            .label(R.string.configbuilder_profile))
    private val screenNsProfile = SWScreen(injector, R.string.nsprofile)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(R.string.adjustprofileinns))
        .add(SWFragment(injector, this)
            .add(NSProfileFragment()))
        .validator(SWValidator { nsProfilePlugin.profile != null && nsProfilePlugin.profile!!.getDefaultProfile() != null && nsProfilePlugin.profile!!.getDefaultProfile()!!.isValid("StartupWizard") })
        .visibility(SWValidator { nsProfilePlugin.isEnabled(PluginType.PROFILE) })
    private val screenLocalProfile = SWScreen(injector, R.string.localprofile)
        .skippable(false)
        .add(SWFragment(injector, this)
            .add(LocalProfileFragment()))
        .validator(SWValidator { localProfilePlugin.profile?.getDefaultProfile()?.isValid("StartupWizard") == true })
        .visibility(SWValidator { localProfilePlugin.isEnabled(PluginType.PROFILE) })
    private val screenProfileSwitch = SWScreen(injector, R.string.careportal_profileswitch)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(R.string.profileswitch_ismissing))
        .add(SWButton(injector)
            .text(R.string.doprofileswitch)
            .action(Runnable { ProfileSwitchDialog().show(activity.supportFragmentManager, "SetupWizard") }))
        .validator(SWValidator { profileFunction.getProfile() != null })
        .visibility(SWValidator { profileFunction.getProfile() == null })
    private val screenPump = SWScreen(injector, R.string.configbuilder_pump)
        .skippable(false)
        .add(SWPlugin(injector)
            .option(PluginType.PUMP, R.string.configbuilder_pump_description)
            .label(R.string.configbuilder_pump))
        .add(SWBreak(injector))
        .add(SWButton(injector)
            .text(R.string.pumpsetup)
            .action(Runnable {
                val plugin = activePlugin.activePump as PluginBase
                protectionCheck.queryProtection(activity, ProtectionCheck.Protection.PREFERENCES, Runnable {
                    val i = Intent(activity, PreferencesActivity::class.java)
                    i.putExtra("id", plugin.preferencesId)
                    activity.startActivity(i)
                }, null)
            })
            .visibility(SWValidator { (activePlugin.activePump as PluginBase).preferencesId > 0 }))
        .add( // Omnipod only
            SWInfotext(injector)
                .label(R.string.setupwizard_pump_waiting_for_riley_link_connection)
                .visibility(SWValidator {
                    val activePump = activePlugin.activePump
                    activePump is OmnipodPumpPlugin && !activePump.isRileyLinkReady
                }))
        .add( // Omnipod only
            SWEventListener(injector, EventRileyLinkDeviceStatusChange::class.java)
                .label(R.string.setupwizard_pump_riley_link_status)
                .visibility(SWValidator {
                    activePlugin.activePump is OmnipodPumpPlugin
                }))
        .add(SWButton(injector)
            .text(R.string.readstatus)
            .action(Runnable { commandQueue.readStatus("Clicked connect to pump", null) })
            .visibility(SWValidator {
                // Hide for Omnipod, because as we don't require a Pod to be paired in the setup wizard,
                // Getting the status might not be possible
                activePlugin.activePump !is OmnipodPumpPlugin
            }))
        .add(SWEventListener(injector, EventPumpStatusChanged::class.java)
            .visibility(SWValidator { activePlugin.activePump !is OmnipodPumpPlugin }))
        .validator(SWValidator {
            isPumpInitialized()
        })

    private fun isPumpInitialized(): Boolean {
        val activePump = activePlugin.activePump

        // For Omnipod, consider the pump initialized when a RL has been configured successfully
        // Users will be prompted to activate a Pod after completing the setup wizard.
        return activePump.isInitialized || (activePump is OmnipodPumpPlugin && activePump.isRileyLinkReady)
    }

    private val screenAps = SWScreen(injector, R.string.configbuilder_aps)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(R.string.setupwizard_aps_description))
        .add(SWBreak(injector))
        .add(SWButton(injector)
            .text(R.string.apssetup)
            .action(Runnable {
                val plugin = activePlugin.activeAPS as PluginBase
                protectionCheck.queryProtection(activity, ProtectionCheck.Protection.PREFERENCES, Runnable {
                    val i = Intent(activity, PreferencesActivity::class.java)
                    i.putExtra("id", plugin.preferencesId)
                    activity.startActivity(i)
                }, null)
            })
            .visibility(SWValidator { (activePlugin.activeAPS as PluginBase).preferencesId > 0 }))
        .visibility(SWValidator { config.APS })
    private val screenApsMode = SWScreen(injector, R.string.apsmode_title)
        .skippable(false)
        .add(SWRadioButton(injector)
            .option(R.array.aps_modeArray, R.array.aps_modeValues)
            .preferenceId(R.string.key_aps_mode).label(R.string.apsmode_title)
            .comment(R.string.setupwizard_preferred_aps_mode))
        .validator(SWValidator { sp.contains(R.string.key_aps_mode) })
    private val screenLoop = SWScreen(injector, R.string.configbuilder_loop)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(R.string.setupwizard_loop_description))
        .add(SWBreak(injector))
        .add(SWButton(injector)
            .text(R.string.enableloop)
            .action(Runnable {
                loopPlugin.setPluginEnabled(PluginType.LOOP, true)
                loopPlugin.setFragmentVisible(PluginType.LOOP, true)
                configBuilderPlugin.processOnEnabledCategoryChanged(loopPlugin, PluginType.LOOP)
                configBuilderPlugin.storeSettings("SetupWizard")
                rxBus.send(EventConfigBuilderChange())
                rxBus.send(EventSWUpdate(true))
            })
            .visibility(SWValidator { !loopPlugin.isEnabled(PluginType.LOOP) }))
        .validator(SWValidator { loopPlugin.isEnabled(PluginType.LOOP) })
        .visibility(SWValidator { !loopPlugin.isEnabled(PluginType.LOOP) && config.APS })
    private val screenSensitivity = SWScreen(injector, R.string.configbuilder_sensitivity)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(R.string.setupwizard_sensitivity_description))
        .add(SWBreak(injector))
        .add(SWButton(injector)
            .text(R.string.sensitivitysetup)
            .action(Runnable {
                val plugin = activePlugin.activeSensitivity as PluginBase
                protectionCheck.queryProtection(activity, ProtectionCheck.Protection.PREFERENCES, Runnable {
                    val i = Intent(activity, PreferencesActivity::class.java)
                    i.putExtra("id", plugin.preferencesId)
                    activity.startActivity(i)
                }, null)
            })
            .visibility(SWValidator { (activePlugin.activeSensitivity as PluginBase).preferencesId > 0 }))
    private val getScreenStages = SWScreen(injector, R.string.stages)
        .skippable(false)
        .add(SWInfotext(injector)
            .label(R.string.startstage))
        .add(SWBreak(injector))
        .add(SWFragment(injector, this)
            .add(StagesFragment()))
        .validator(SWValidator { stagesPlugin.stages[StagesPlugin.FIRST_STAGE].isStarted })
        .visibility(SWValidator { !stagesPlugin.stages[StagesPlugin.FIRST_STAGE].isStarted && config.APS })

    private fun swDefinitionFull() { // List all the screens here
        add(if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) null else screencheckAndroidVersion)
            .add(screenSetupWizard)
            .add(if (sp.getString(R.string.key_app_password , "") ==  Constants.APP_Password) null else screenUnlock)
            .add(if (isRunningTest()) null else screenPermissionBattery) // cannot mock ask battery optimization
            .add(screenPermissionBt)
            .add(screenPermissionWindow)
            .add(screenPermissionStore)
            .add(screenMasterPassword)
            .add(if (buildHelper.isEngineeringMode()) screenImport else null)
            .add(if (buildHelper.isEngineeringMode()) screenUnits else null)
            .add(screenNsClient)
            .add(screenPatientName)
            .add(screenTreatmentSafety)
            .add(if (buildHelper.isEngineeringMode()) screenInsulin else null)
            .add(if (buildHelper.isEngineeringMode() || buildHelper.isxDripenabled()) screenBgSource else null)
            .add(if (buildHelper.isEngineeringMode()) screenProfile else null)
            .add(screenLocalProfile)
            .add(if (buildHelper.isEngineeringMode()) screenNsProfile else null)
            .add(screenProfileSwitch)
            .add(screenPump)
            .add(screenAps)
            .add(if (buildHelper.isEngineeringMode()) screenApsMode else null)
            .add(screenLoop)
            .add(screenSensitivity)
            .add(getScreenStages)
    }

    private fun swDefinitionPumpControl() { // List all the screens here
        add(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) null else screencheckAndroidVersion)
            .add(screenSetupWizard)
            .add(if (isRunningTest()) null else screenPermissionBattery) // cannot mock ask battery optimization
            .add(screenPermissionBt)
            .add(screenPermissionWindow)
            .add(screenPermissionStore)
            .add(screenMasterPassword)
            .add(if (buildHelper.isEngineeringMode()) screenImport else null)
            .add(if (buildHelper.isEngineeringMode()) screenUnits else null)
            .add(screenNsClient)
            .add(screenPatientName)
            .add(if (buildHelper.isEngineeringMode() || buildHelper.isxDripenabled()) screenBgSource else null)
            .add(if (buildHelper.isEngineeringMode()) screenProfile else null)
            .add(screenLocalProfile)
            .add(if (buildHelper.isEngineeringMode()) screenNsProfile else null)
            .add(screenPump)
    }

    private fun swDefinitionNSClient() { // List all the screens here
        add(screenSetupWizard)
            .add(if (isRunningTest()) null else screenPermissionBattery) // cannot mock ask battery optimization
            .add(screenPermissionStore)
            .add(screenMasterPassword)
            .add(if (buildHelper.isEngineeringMode()) screenImport else null)
            .add(screenUnits)
            .add(displaySettings)
            .add(screenNsClient)
            //.add(screenBgSource)
            .add(screenPatientName)
            .add(screenTreatmentSafety)
            .add(screenInsulin)
            .add(screenSensitivity)
    }

    init {
        if(!buildHelper.isEngineeringMode()) {
            //set to MMOL by default
            sp.putString(R.string.key_units, Constants.MMOL)
            //set to openloop by default
            if (!config.PUMPCONTROL) {
                if (!sp.contains(R.string.key_aps_mode))
                    sp.putString(R.string.key_aps_mode, "open")
            }else{
                sp.putBoolean(R.string.key_rs_logcanulachange,true)
            }

            sp.putDouble(R.string.key_high_mark, 10.0)
            sp.putDouble(R.string.key_low_mark, 3.9)
        }
        if (config.APS) swDefinitionFull() else if (config.PUMPCONTROL) swDefinitionPumpControl() else if (config.NSCLIENT) swDefinitionNSClient()
    }
}
