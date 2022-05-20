package info.nightscout.androidaps.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.XmlRes
import androidx.preference.*
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import info.nightscout.androidaps.Config
import info.nightscout.androidaps.R
import info.nightscout.androidaps.danaRKorean.DanaRKoreanPlugin
import info.nightscout.androidaps.danaRv2.DanaRv2Plugin
import info.nightscout.androidaps.danar.DanaRPlugin
import info.nightscout.androidaps.danars.DanaRSPlugin
import info.nightscout.androidaps.data.Profile
import info.nightscout.androidaps.events.EventPreferenceChange
import info.nightscout.androidaps.events.EventRebuildTabs
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.ProfileFunction
import info.nightscout.androidaps.plugins.aps.loop.LoopPlugin
import info.nightscout.androidaps.plugins.aps.openAPSSMB.OpenAPSSMBPlugin
import info.nightscout.androidaps.plugins.bus.RxBusWrapper
import info.nightscout.androidaps.plugins.configBuilder.PluginStore
import info.nightscout.androidaps.plugins.constraints.safety.SafetyPlugin
import info.nightscout.androidaps.plugins.general.automation.AutomationPlugin
import info.nightscout.androidaps.plugins.general.maintenance.MaintenancePlugin
import info.nightscout.androidaps.plugins.general.nsclient.NSClientPlugin
import info.nightscout.androidaps.plugins.general.nsclient.data.NSSettingsStatus
import info.nightscout.androidaps.plugins.general.openhumans.OpenHumansUploader
import info.nightscout.androidaps.plugins.general.smsCommunicator.SmsCommunicatorPlugin
import info.nightscout.androidaps.plugins.general.tidepool.TidepoolPlugin
import info.nightscout.androidaps.plugins.general.wear.WearPlugin
import info.nightscout.androidaps.plugins.general.xdripStatusline.StatusLinePlugin
import info.nightscout.androidaps.plugins.pump.combo.ComboPlugin
import info.nightscout.androidaps.plugins.pump.insight.LocalInsightPlugin
import info.nightscout.androidaps.plugins.pump.medtronic.MedtronicPumpPlugin
import info.nightscout.androidaps.plugins.pump.virtual.VirtualPumpPlugin
import info.nightscout.androidaps.plugins.sensitivity.SensitivityOref1Plugin
import info.nightscout.androidaps.plugins.source.DexcomPlugin
import info.nightscout.androidaps.plugins.configBuilder.ConstraintChecker
import info.nightscout.androidaps.utils.SafeParse
import info.nightscout.androidaps.utils.alertDialogs.OKDialog.show
import info.nightscout.androidaps.utils.protection.PasswordCheck
import info.nightscout.androidaps.utils.protection.ProtectionCheck
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.androidaps.utils.sharedPreferences.SP
import info.nightscout.androidaps.utils.buildHelper.BuildHelper
import javax.inject.Inject

class MyPreferenceFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener, HasAndroidInjector {

    private var pluginId = -1

    @Inject lateinit var rxBus: RxBusWrapper
    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var sp: SP
    @Inject lateinit var profileFunction: ProfileFunction
    @Inject lateinit var pluginStore: PluginStore
    @Inject lateinit var config: Config

    @Inject lateinit var constraintChecker: ConstraintChecker

    @Inject lateinit var automationPlugin: AutomationPlugin
    @Inject lateinit var danaRPlugin: DanaRPlugin
    @Inject lateinit var danaRKoreanPlugin: DanaRKoreanPlugin
    @Inject lateinit var danaRv2Plugin: DanaRv2Plugin
    @Inject lateinit var danaRSPlugin: DanaRSPlugin
    @Inject lateinit var comboPlugin: ComboPlugin
    @Inject lateinit var loopPlugin: LoopPlugin
    @Inject lateinit var localInsightPlugin: LocalInsightPlugin
    @Inject lateinit var medtronicPumpPlugin: MedtronicPumpPlugin
    @Inject lateinit var nsClientPlugin: NSClientPlugin
    @Inject lateinit var openAPSSMBPlugin: OpenAPSSMBPlugin
    @Inject lateinit var safetyPlugin: SafetyPlugin
    @Inject lateinit var sensitivityOref1Plugin: SensitivityOref1Plugin
    @Inject lateinit var dexcomPlugin: DexcomPlugin
    @Inject lateinit var smsCommunicatorPlugin: SmsCommunicatorPlugin
    @Inject lateinit var statusLinePlugin: StatusLinePlugin
    @Inject lateinit var tidepoolPlugin: TidepoolPlugin
    @Inject lateinit var virtualPumpPlugin: VirtualPumpPlugin
    @Inject lateinit var wearPlugin: WearPlugin
    @Inject lateinit var maintenancePlugin: MaintenancePlugin
    @Inject lateinit var buildhelper: BuildHelper

    @Inject lateinit var passwordCheck: PasswordCheck
    @Inject lateinit var nsSettingStatus: NSSettingsStatus
    @Inject lateinit var openHumansUploader: OpenHumansUploader

    // TODO why?
    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        pluginId = args?.getInt("id") ?: -1
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("id", pluginId)
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun addPreferencesFromResourceIfEnabled(p: PluginBase?, rootKey: String?, enabled: Boolean) {
        if (enabled) addPreferencesFromResourceIfEnabled(p, rootKey)
    }

    private fun addPreferencesFromResourceIfEnabled(p: PluginBase?, rootKey: String?) {
        if (p!!.isEnabled() && p.preferencesId != -1)
            addPreferencesFromResource(p.preferencesId, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        (savedInstanceState ?: arguments)?.let { bundle ->
            if (bundle.containsKey("id")) {
                pluginId = bundle.getInt("id")
            }
        }
        if (pluginId != -1) {
            addPreferencesFromResource(pluginId, rootKey)
        } else {
            addPreferencesFromResource(R.xml.pref_general, rootKey)
            addPreferencesFromResource(R.xml.pref_overview, rootKey)
            addPreferencesFromResourceIfEnabled(safetyPlugin, rootKey)
            addPreferencesFromResourceIfEnabled(dexcomPlugin, rootKey)
            addPreferencesFromResourceIfEnabled(loopPlugin, rootKey, config.APS)
            addPreferencesFromResourceIfEnabled(openAPSSMBPlugin, rootKey, config.APS)
            addPreferencesFromResourceIfEnabled(sensitivityOref1Plugin, rootKey)
            addPreferencesFromResourceIfEnabled(danaRPlugin, rootKey, config.PUMPDRIVERS)
            addPreferencesFromResourceIfEnabled(danaRKoreanPlugin, rootKey, config.PUMPDRIVERS)
            addPreferencesFromResourceIfEnabled(danaRv2Plugin, rootKey, config.PUMPDRIVERS)
            addPreferencesFromResourceIfEnabled(danaRSPlugin, rootKey, config.PUMPDRIVERS)
            addPreferencesFromResourceIfEnabled(localInsightPlugin, rootKey, config.PUMPDRIVERS)
            addPreferencesFromResourceIfEnabled(comboPlugin, rootKey, config.PUMPDRIVERS)
            addPreferencesFromResourceIfEnabled(medtronicPumpPlugin, rootKey, config.PUMPDRIVERS)
            addPreferencesFromResource(R.xml.pref_pump, rootKey, buildhelper.isEngineeringMode())
            addPreferencesFromResourceIfEnabled(virtualPumpPlugin, rootKey)
            addPreferencesFromResourceIfEnabled(nsClientPlugin, rootKey)
            addPreferencesFromResourceIfEnabled(tidepoolPlugin, rootKey)
            addPreferencesFromResourceIfEnabled(smsCommunicatorPlugin, rootKey)
            addPreferencesFromResourceIfEnabled(automationPlugin, rootKey)
            addPreferencesFromResourceIfEnabled(wearPlugin, rootKey)
            addPreferencesFromResourceIfEnabled(statusLinePlugin, rootKey)
            addPreferencesFromResource(R.xml.pref_alerts, rootKey) // TODO not organized well
            if (buildhelper.isEngineeringMode()) {
                addPreferencesFromResource(R.xml.pref_datachoices, rootKey)
                addPreferencesFromResourceIfEnabled(maintenancePlugin, rootKey)
                addPreferencesFromResourceIfEnabled(openHumansUploader, rootKey)
            }
        }
        initSummary(preferenceScreen, pluginId != -1)
        preprocessPreferences()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        rxBus.send(EventPreferenceChange(key))
        if (key == resourceHelper.gs(R.string.key_language)) {
            rxBus.send(EventRebuildTabs(true))
            //recreate() does not update language so better close settings
            activity?.finish()
        }
        if (key == resourceHelper.gs(R.string.key_short_tabtitles)) {
            rxBus.send(EventRebuildTabs())
        }
        if (key == resourceHelper.gs(R.string.key_units)) {
            activity?.recreate()
            return
        }
        if (key == resourceHelper.gs(R.string.key_openapsama_useautosens) && sp.getBoolean(R.string.key_openapsama_useautosens, false)) {
            activity?.let {
                show(it, resourceHelper.gs(R.string.configbuilder_sensitivity), resourceHelper.gs(R.string.sensitivity_warning))
            }
        }

        checkForBiometricFallback(key)

        updatePrefSummary(findPreference(key))
        preprocessPreferences()
    }

    private fun preprocessPreferences() {
        for (plugin in pluginStore.plugins) {
            plugin.preprocessPreferences(this)
        }
    }

    private fun checkForBiometricFallback(key: String) {
        // Biometric protection activated without set master password
        if ((resourceHelper.gs(R.string.key_settings_protection) == key ||
                resourceHelper.gs(R.string.key_application_protection) == key ||
                resourceHelper.gs(R.string.key_bolus_protection) == key) &&
            sp.getString(R.string.key_master_password, "") == "" &&
            sp.getInt(key, ProtectionCheck.ProtectionType.NONE.ordinal) == ProtectionCheck.ProtectionType.BIOMETRIC.ordinal
        ) {
            activity?.let {
                val title = resourceHelper.gs(R.string.unsecure_fallback_biometric)
                val message = resourceHelper.gs(R.string.master_password_missing, resourceHelper.gs(R.string.configbuilder_general), resourceHelper.gs(R.string.protection))
                show(it, title = title, message = message)
            }
        }

        // Master password erased with activated Biometric protection
        val isBiometricActivated = sp.getInt(R.string.key_settings_protection, ProtectionCheck.ProtectionType.NONE.ordinal) == ProtectionCheck.ProtectionType.BIOMETRIC.ordinal ||
            sp.getInt(R.string.key_application_protection, ProtectionCheck.ProtectionType.NONE.ordinal) == ProtectionCheck.ProtectionType.BIOMETRIC.ordinal ||
            sp.getInt(R.string.key_bolus_protection, ProtectionCheck.ProtectionType.NONE.ordinal) == ProtectionCheck.ProtectionType.BIOMETRIC.ordinal
        if (resourceHelper.gs(R.string.key_master_password) == key && sp.getString(key, "") == "" && isBiometricActivated) {
            activity?.let {
                val title = resourceHelper.gs(R.string.unsecure_fallback_biometric)
                val message = resourceHelper.gs(R.string.unsecure_fallback_descriotion_biometric)
                show(it, title = title, message = message)
            }
        }
    }

    private fun addPreferencesFromResource(@XmlRes preferencesResId: Int, key: String?, enabled: Boolean) {
        if (enabled) addPreferencesFromResource(preferencesResId, key)
    }

    @SuppressLint("RestrictedApi")
    private fun addPreferencesFromResource(@XmlRes preferencesResId: Int, key: String?) {
        val xmlRoot = preferenceManager.inflateFromResource(context,
            preferencesResId, null)
        val root: Preference?
        if (key != null) {
            root = xmlRoot.findPreference(key)
            if (root == null) return
            require(root is PreferenceScreen) {
                ("Preference object with key " + key
                    + " is not a PreferenceScreen")
            }
            preferenceScreen = root
        } else {
            addPreferencesFromResource(preferencesResId)
        }
    }

    private fun adjustUnitDependentPrefs(pref: Preference) { // convert preferences values to current units
        val unitDependent = arrayOf(
            resourceHelper.gs(R.string.key_hypo_target),
            resourceHelper.gs(R.string.key_activity_target),
            resourceHelper.gs(R.string.key_eatingsoon_target),
            resourceHelper.gs(R.string.key_high_mark),
            resourceHelper.gs(R.string.key_low_mark)
        )
        if (unitDependent.toList().contains(pref.key) && pref is EditTextPreference) {
            val converted = Profile.toCurrentUnits(profileFunction, SafeParse.stringToDouble(pref.text))
            pref.summary = converted.toString()
        }
    }

    private fun updatePrefSummary(pref: Preference?) {
        if (pref is ListPreference) {
            pref.setSummary(pref.entry)
            // Preferences
            // Preferences
            if (pref.getKey() == resourceHelper.gs(R.string.key_settings_protection)) {
                val pass: Preference? = findPreference(resourceHelper.gs(R.string.key_settings_password))
                if (pass != null) pass.isEnabled = pref.value == ProtectionCheck.ProtectionType.CUSTOM_PASSWORD.ordinal.toString()
            }
            // Application
            // Application
            if (pref.getKey() == resourceHelper.gs(R.string.key_application_protection)) {
                val pass: Preference? = findPreference(resourceHelper.gs(R.string.key_application_password))
                if (pass != null) pass.isEnabled = pref.value == ProtectionCheck.ProtectionType.CUSTOM_PASSWORD.ordinal.toString()
            }
            // Bolus
            // Bolus
            if (pref.getKey() == resourceHelper.gs(R.string.key_bolus_protection)) {
                val pass: Preference? = findPreference(resourceHelper.gs(R.string.key_bolus_password))
                if (pass != null) pass.isEnabled = pref.value == ProtectionCheck.ProtectionType.CUSTOM_PASSWORD.ordinal.toString()
            }
        }
        if (pref is EditTextPreference) {
            if (pref.getKey().contains("password") || pref.getKey().contains("secret")) {
                pref.setSummary("******")
            } else if (pref.text != null) {
                pref.dialogMessage = pref.dialogMessage
                pref.setSummary(pref.text)
            }
        }

        for (plugin in pluginStore.plugins) {
            pref?.let { it.key?.let { plugin.updatePreferenceSummary(pref) } }
        }

        val hmacPasswords = arrayOf(
            resourceHelper.gs(R.string.key_bolus_password),
            resourceHelper.gs(R.string.key_master_password),
            resourceHelper.gs(R.string.key_application_password),
            resourceHelper.gs(R.string.key_settings_password)
        )

        if (pref is Preference) {
            if ((pref.key != null) && (hmacPasswords.contains(pref.key))) {
                if (sp.getString(pref.key, "").startsWith("hmac:")) {
                    pref.summary = "******"
                } else {
                    pref.summary = resourceHelper.gs(R.string.password_not_set)
                }
            }
        }
        pref?.let { adjustUnitDependentPrefs(it) }
    }

    private fun initSummary(p: Preference, isSinglePreference: Boolean) {
        p.isIconSpaceReserved = false // remove extra spacing on left after migration to androidx
        // expand single plugin preference by default
        if (p is PreferenceScreen && isSinglePreference) {
            if (p.size > 0 && p.getPreference(0) is PreferenceCategory)
                (p.getPreference(0) as PreferenceCategory).initialExpandedChildrenCount = Int.MAX_VALUE
        }
        if (p is PreferenceGroup) {
            for (i in 0 until p.preferenceCount) {
                initSummary(p.getPreference(i), isSinglePreference)
            }
        } else {
            updatePrefSummary(p)
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_absorption_danars_advanced) && !buildhelper.isEngineeringMode()) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_absorption_danars_advanced))
            if (pref != null) pref.isVisible = false
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_btwatchdog) && !buildhelper.isEngineeringMode()) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_btwatchdog))
            if (pref != null) pref.isVisible = false
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_low_temptarget_lowers_sensitivity) && !buildhelper.isEngineeringMode()) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_low_temptarget_lowers_sensitivity))
            if (pref != null) pref.isVisible = false
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_high_temptarget_raises_sensitivity) && !buildhelper.isEngineeringMode()) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_high_temptarget_raises_sensitivity))
            if (pref != null) pref.isVisible = false
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_units) && buildhelper.isEngineeringMode()) {
            val units: Preference? = findPreference(resourceHelper.gs(R.string.key_units))
            if (units != null) units.isEnabled = true
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_dexcomg5_xdripupload)) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_dexcomg5_xdripupload))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_enable_carbs_required_alert_local) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_enable_carbs_required_alert_local))
            if (config.PUMPCONTROL) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_dexcomg5_nsupload) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_dexcomg5_nsupload))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_allowSMB_with_high_temptarget) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_allowSMB_with_high_temptarget))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_always_use_shortavg) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_always_use_shortavg))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_ns_sync_use_absolute) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_ns_sync_use_absolute))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_ns_noupload) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_ns_noupload))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_nsclient_localbroadcasts) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_nsclient_localbroadcasts))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_ns_upload_only) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_ns_upload_only))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_set_neutral_temps) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_set_neutral_temps))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }

        if (p.getKey() == resourceHelper.gs(R.string.key_show_cgm_button) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_show_cgm_button))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }

        if (p.getKey() == resourceHelper.gs(R.string.key_quickwizard) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_quickwizard))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }

        if (p.getKey() == resourceHelper.gs(R.string.key_raise_notifications_as_android_notifications) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_raise_notifications_as_android_notifications))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }

        if (p.getKey() == resourceHelper.gs(R.string.prefs_range_title) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.prefs_range_title))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }

        if (p.getKey() == resourceHelper.gs(R.string.key_skin) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_skin))
            if (!buildhelper.isEngineeringMode()) {
                if (pref != null) pref.isVisible = false
            }else{
                if (pref != null) pref.isEnabled = true
            }
        }

        if (p.getKey() == resourceHelper.gs(R.string.overview_buttons_selection) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.overview_buttons_selection))
            if (config.PUMPCONTROL) {
                if (pref != null) pref.isVisible = false
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.default_temptargets) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.default_temptargets))
            if (config.PUMPCONTROL) {
                if (pref != null) pref.isVisible = false
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.fillbolus_title) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.fillbolus_title))
            if (config.PUMPCONTROL) {
                if (pref != null) pref.isVisible = false
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.key_show_notes_entry_dialogs) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.key_show_notes_entry_dialogs))
            if (config.PUMPCONTROL) {
                if (pref != null) pref.isVisible = false
            }
        }
        if (p.getKey() == resourceHelper.gs(R.string.overview_advanced) ) {
            val pref: Preference? = findPreference(resourceHelper.gs(R.string.overview_advanced))
            if (config.PUMPCONTROL) {
                if (pref != null) pref.isVisible = false
            }
        }


    }

    // We use Preference and custom editor instead of EditTextPreference
    // to hash password while it is saved and never have to show it, even hashed

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        context?.let { context ->
            if (preference != null) {
                if (preference.key == resourceHelper.gs(R.string.key_master_password)) {
                    passwordCheck.queryPassword(context, R.string.current_master_password, R.string.key_master_password, {
                        passwordCheck.setPassword(context, R.string.master_password, R.string.key_master_password)
                    })
                    return true
                }
                if (preference.key == resourceHelper.gs(R.string.key_settings_password)) {
                    passwordCheck.setPassword(context, R.string.settings_password, R.string.key_settings_password)
                    return true
                }
                if (preference.key == resourceHelper.gs(R.string.key_bolus_password)) {
                    passwordCheck.setPassword(context, R.string.bolus_password, R.string.key_bolus_password)
                    return true
                }
                if (preference.key == resourceHelper.gs(R.string.key_application_password)) {
                    passwordCheck.setPassword(context, R.string.application_password, R.string.key_application_password)
                    return true
                }
                // NSClient copy settings
                if (preference.key == resourceHelper.gs(R.string.key_statuslights_copy_ns)) {
                    nsSettingStatus.copyStatusLightsNsSettings(context)
                    return true
                }
                if (preference.key == resourceHelper.gs(R.string.key_nsclientinternal_api_secret) || preference.key == resourceHelper.gs(R.string.key_nsclientinternal_url) && !buildhelper.isEngineeringMode()) {
                    activity?.let {
                        show(it, resourceHelper.gs(R.string.configbuilder_sensitive_settings_change), resourceHelper.gs(R.string.settings_change_warning))
                    }
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}
