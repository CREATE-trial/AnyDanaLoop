package info.nightscout.androidaps.plugins.constraints.stages

import android.app.Activity
import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.BuildConfig
import info.nightscout.androidaps.Config
import info.nightscout.androidaps.R
import info.nightscout.androidaps.interfaces.*
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.plugins.constraints.stages.stages.*
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.alertDialogs.OKDialog
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.androidaps.utils.sharedPreferences.SP
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StagesPlugin @Inject constructor(
    injector: HasAndroidInjector,
    aapsLogger: AAPSLogger,
    resourceHelper: ResourceHelper,
    private val activePlugin: ActivePluginProvider,
    private val sp: SP,
    private val config: Config


) : PluginBase(PluginDescription()
    .mainType(PluginType.CONSTRAINTS)
    .fragmentClass(StagesFragment::class.qualifiedName)
    .alwaysEnabled(config.APS)
    .showInList(config.APS)
    .pluginName(R.string.stages)
    .shortName(R.string.stages_shortname)
    .description(R.string.description_stages),
    aapsLogger, resourceHelper, injector
), ConstraintsInterface {

    var stages: MutableList<Stage> = ArrayList()

    companion object {
        const val FIRST_STAGE = 0
        @Suppress("unused") const val OPENLOOP_OBJECTIVE = 0
        const val MAXIOB_ZERO_CL_OBJECTIVE = 1
        @Suppress("unused") const val MAXIOB_OBJECTIVE = 2
        const val AUTOSENS_OBJECTIVE = 2
        const val SMB_OBJECTIVE = 3
    }

    public override fun onStart() {
        super.onStart()
        convertSP()
        setupStages()
    }

    override fun specialEnableCondition(): Boolean {
        return activePlugin.activePump.pumpDescription.isTempBasalCapable
    }

    // convert 2.3 SP version
    private fun convertSP() {
        doConvertSP(0, "openloop")
        doConvertSP(1, "maxiobzero")
        doConvertSP(2, "maxiob")
        doConvertSP(3, "smb")
    }

    private fun doConvertSP(number: Int, name: String) {
        if (!sp.contains("Stages_" + name + "_started")) {
            sp.putLong("Stages_" + name + "_started", sp.getLong("Stages" + number + "started", 0L))
            sp.putLong("Stages_" + name + "_accomplished", sp.getLong("Stages" + number + "accomplished", 0L))
        }
        // TODO: we can remove Stages1accomplished sometimes later
    }

    private fun setupStages() {
        stages.clear()
        stages.add(Stage0(injector))
        stages.add(Stage1(injector))
        stages.add(Stage2(injector))
        stages.add(Stage3(injector))
    }

    fun reset() {
        for (stage in stages) {
            stage.startedOn = 0
            stage.accomplishedOn = 0
        }
        // sp.putBoolean(R.string.key_StagesbgIsAvailableInNS, false)
        // sp.putBoolean(R.string.key_StagespumpStatusIsAvailableInNS, false)
        sp.putInt(R.string.key_StagesmanualEnacts, 0)
        // sp.putBoolean(R.string.key_stageuseprofileswitch, false)
        // sp.putBoolean(R.string.key_stageusedisconnect, false)
        // sp.putBoolean(R.string.key_stageusereconnect, false)
        // sp.putBoolean(R.string.key_stageusetemptarget, false)
        // sp.putBoolean(R.string.key_stageuseactions, false)
        sp.putBoolean(R.string.key_stageuseloop, false)
        // sp.putBoolean(R.string.key_stageusescale, false)
    }

    fun completeStages(activity: Activity, request: String) {
        val requestCode = sp.getString(R.string.key_stages_request_code, "")
        var url = sp.getString(R.string.key_nsclientinternal_url, "").toLowerCase(Locale.getDefault())
        if (!url.endsWith("/")) url = "$url/"
        @Suppress("DEPRECATION") val hashNS = Hashing.sha1().hashString(url + BuildConfig.APPLICATION_ID + "/" + requestCode, Charsets.UTF_8).toString()
        if (request.equals(hashNS.substring(0, 10), ignoreCase = true)) {
            sp.putLong("Stages_" + "openloop" + "_started", DateUtil.now())
            sp.putLong("Stages_" + "openloop" + "_accomplished", DateUtil.now())
            // sp.putLong("Stages_" + "maxbasal" + "_started", DateUtil.now())
            // sp.putLong("Stages_" + "maxbasal" + "_accomplished", DateUtil.now())
            sp.putLong("Stages_" + "maxiobzero" + "_started", DateUtil.now())
            sp.putLong("Stages_" + "maxiobzero" + "_accomplished", DateUtil.now())
            sp.putLong("Stages_" + "maxiob" + "_started", DateUtil.now())
            sp.putLong("Stages_" + "maxiob" + "_accomplished", DateUtil.now())
            // sp.putLong("Stages_" + "autosens" + "_started", DateUtil.now())
            // sp.putLong("Stages_" + "autosens" + "_accomplished", DateUtil.now())
            // sp.putLong("Stages_" + "ama" + "_started", DateUtil.now())
            // sp.putLong("Stages_" + "ama" + "_accomplished", DateUtil.now())
            sp.putLong("Stages_" + "smb" + "_started", DateUtil.now())
            sp.putLong("Stages_" + "smb" + "_accomplished", DateUtil.now())
            setupStages()
            OKDialog.show(activity, resourceHelper.gs(R.string.stages), resourceHelper.gs(R.string.codeaccepted))
        } else {
            OKDialog.show(activity, resourceHelper.gs(R.string.stages), resourceHelper.gs(R.string.codeinvalid))
        }
    }

    fun allPriorAccomplished(position: Int) : Boolean {
        var accomplished = true
        for (i in 0 until position) {
            accomplished = accomplished && stages[i].isAccomplished
        }
        return accomplished
    }

    /**
     * Constraints interface
     */

    override fun isClosedLoopAllowed(value: Constraint<Boolean>): Constraint<Boolean> {
        if (!stages[MAXIOB_ZERO_CL_OBJECTIVE].isStarted)
            value.set(aapsLogger, false, String.format(resourceHelper.gs(R.string.stagenotstarted), MAXIOB_ZERO_CL_OBJECTIVE + 1), this)
        return value
    }

    override fun isLGSAllowed(value: Constraint<Boolean>): Constraint<Boolean> {
        if (!stages[MAXIOB_ZERO_CL_OBJECTIVE].isStarted && !stages[MAXIOB_ZERO_CL_OBJECTIVE].isAccomplished)
            value.set(aapsLogger, false, String.format(resourceHelper.gs(R.string.stagenotstarted), MAXIOB_ZERO_CL_OBJECTIVE + 1), this)
        return value
    }

    override fun isFullClosedLoopAllowed(value: Constraint<Boolean>): Constraint<Boolean> {
        if (!stages[MAXIOB_OBJECTIVE].isStarted)
            value.set(aapsLogger, false, String.format(resourceHelper.gs(R.string.stagenotstarted), MAXIOB_OBJECTIVE + 1), this)
        return value
    }

    override fun isClosedLoopEnabled(value: Constraint<Boolean>): Constraint<Boolean> {
        return this.isClosedLoopAllowed(value);
    }

    override fun isSMBModeEnabled(value: Constraint<Boolean>): Constraint<Boolean> {
        if (!stages[SMB_OBJECTIVE].isStarted)
            value.set(aapsLogger, false, String.format(resourceHelper.gs(R.string.stagenotstarted), SMB_OBJECTIVE + 1), this)
        return value
    }

    override fun isSMBModeAllowed(value: Constraint<Boolean>): Constraint<Boolean> {
        if (!stages[SMB_OBJECTIVE].isStarted)
            value.set(aapsLogger, false, String.format(resourceHelper.gs(R.string.stagenotstarted), SMB_OBJECTIVE + 1), this)
        return value
    }

    override fun applyMaxIOBConstraints(maxIob: Constraint<Double>): Constraint<Double> {
        if (stages[MAXIOB_ZERO_CL_OBJECTIVE].isStarted && !stages[MAXIOB_ZERO_CL_OBJECTIVE].isAccomplished)
            maxIob.set(aapsLogger, 0.0, String.format(resourceHelper.gs(R.string.stagenotfinished), MAXIOB_ZERO_CL_OBJECTIVE + 1), this)
        return maxIob
    }
}
