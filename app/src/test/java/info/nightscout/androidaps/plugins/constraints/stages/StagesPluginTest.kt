package info.nightscout.androidaps.plugins.constraints.stages

import dagger.android.AndroidInjector
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.R
import info.nightscout.androidaps.TestBase
import info.nightscout.androidaps.interfaces.ActivePluginProvider
import info.nightscout.androidaps.interfaces.Constraint
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.plugins.constraints.stages.stages.Stage
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.androidaps.utils.sharedPreferences.SP
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
class StagesPluginTest : TestBase() {

    @Mock lateinit var resourceHelper: ResourceHelper
    @Mock lateinit var activePlugin: ActivePluginProvider
    @Mock lateinit var sp: SP

    private lateinit var stagesPlugin: StagesPlugin

    val injector = HasAndroidInjector {
        AndroidInjector {
            if (it is Stage) {
                it.sp = sp
                it.resourceHelper = resourceHelper
            }
        }
    }

    @Before fun prepareMock() {
        stagesPlugin = StagesPlugin(injector, aapsLogger, resourceHelper, activePlugin, sp)
        stagesPlugin.onStart()
        `when`(resourceHelper.gs(R.string.stagenotstarted)).thenReturn("Stage %1\$d not started")
    }

    @Test fun notStartedStagesShouldLimitLoopInvocation() {
        stagesPlugin.stages[StagesPlugin.FIRST_OBJECTIVE].startedOn = 0
        var c = Constraint(true)
        c = stagesPlugin.isLoopInvocationAllowed(c)
        Assert.assertEquals("Stages: Stage 1 not started", c.getReasons(aapsLogger))
        Assert.assertEquals(false, c.value())
        stagesPlugin.stages[StagesPlugin.FIRST_OBJECTIVE].startedOn = DateUtil.now()
    }

    @Test fun notStartedStage6ShouldLimitClosedLoop() {
        stagesPlugin.stages[StagesPlugin.MAXIOB_ZERO_CL_OBJECTIVE].startedOn = 0
        var c = Constraint(true)
        c = stagesPlugin.isClosedLoopAllowed(c)
        Assert.assertEquals(true, c.getReasons(aapsLogger).contains("Stage 6 not started"))
        Assert.assertEquals(false, c.value())
    }

    @Test fun notStartedStage8ShouldLimitAutosensMode() {
        stagesPlugin.stages[StagesPlugin.AUTOSENS_OBJECTIVE].startedOn = 0
        var c = Constraint(true)
        c = stagesPlugin.isAutosensModeEnabled(c)
        Assert.assertEquals(true, c.getReasons(aapsLogger).contains("Stage 8 not started"))
        Assert.assertEquals(false, c.value())
    }

    @Test fun notStartedStage9ShouldLimitAMAMode() {
        stagesPlugin.stages[StagesPlugin.AMA_OBJECTIVE].startedOn = 0
        var c = Constraint(true)
        c = stagesPlugin.isAMAModeEnabled(c)
        Assert.assertEquals(true, c.getReasons(aapsLogger).contains("Stage 9 not started"))
        Assert.assertEquals(false, c.value())
    }

    @Test fun notStartedStage10ShouldLimitSMBMode() {
        stagesPlugin.stages[StagesPlugin.SMB_OBJECTIVE].startedOn = 0
        var c = Constraint(true)
        c = stagesPlugin.isSMBModeEnabled(c)
        Assert.assertEquals(true, c.getReasons(aapsLogger).contains("Stage 10 not started"))
        Assert.assertEquals(false, c.value())
    }
}