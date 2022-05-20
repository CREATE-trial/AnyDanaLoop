package info.nightscout.androidaps.plugins.constraints.stages.stages;

import java.util.List;

import javax.inject.Inject;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.interfaces.Constraint;
import info.nightscout.androidaps.plugins.constraints.safety.SafetyPlugin;
import info.nightscout.androidaps.utils.T;

public class Stage1 extends Stage {
    @Inject SafetyPlugin safetyPlugin;

    public Stage1(HasAndroidInjector injector) {
        super(injector, "maxiobzero", R.string.stages_maxiobzero_stage, R.string.stages_maxiobzero_gate);
    }

    @Override
    protected void setupTasks(List<Task> tasks) {
        tasks.add(new MinimumDurationTask(T.days(3).msecs()));
        tasks.add(new Task(R.string.closedmodeenabled) {
            @Override
            public boolean isCompleted() {
                Constraint<Boolean> closedLoopEnabled = new Constraint<>(true);
                safetyPlugin.isClosedLoopEnabled(closedLoopEnabled);
                return closedLoopEnabled.value();
            }
        });
    }
}
