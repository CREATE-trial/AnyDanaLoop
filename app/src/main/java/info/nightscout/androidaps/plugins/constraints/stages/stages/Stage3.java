package info.nightscout.androidaps.plugins.constraints.stages.stages;

import java.util.List;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.utils.T;

public class Stage3 extends Stage {

    public Stage3(HasAndroidInjector injector) {
        super(injector, "smb", R.string.stages_smb_stage, R.string.stages_smb_gate);
    }

    @Override
    protected void setupTasks(List<Task> tasks) {
        tasks.add(new MinimumDurationTask(T.days(7).msecs()));
    }
}
