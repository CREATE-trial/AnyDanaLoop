package info.nightscout.androidaps.plugins.constraints.stages.stages;

import androidx.fragment.app.FragmentActivity;

import java.util.List;

import javax.inject.Inject;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.plugins.constraints.stages.StagesPlugin;
import info.nightscout.androidaps.plugins.general.nsclient.NSClientPlugin;
import info.nightscout.androidaps.utils.T;
import info.nightscout.androidaps.utils.resources.ResourceHelper;
import info.nightscout.androidaps.utils.sharedPreferences.SP;

public class Stage0 extends Stage {
    @Inject SP sp;
    @Inject StagesPlugin stagesPlugin;
    @Inject ResourceHelper resourceHelper;
    @Inject NSClientPlugin nsClientPlugin;

    private final int MANUAL_ENACTS_NEEDED = 10;

    @Inject
    public Stage0(HasAndroidInjector injector) {
        super(injector, "openloop", R.string.stages_openloop_stage, R.string.stages_openloop_gate);
        hasSpecialInput = true;
    }

    @Override
    protected void setupTasks(List<Task> tasks) {
        tasks.add(new Task(R.string.stages_manualenacts) {
            @Override
            public boolean isCompleted() {
                return sp.getInt(R.string.key_StagesmanualEnacts, 0) >= MANUAL_ENACTS_NEEDED;
            }

            @Override
            public String getProgress() {
                if (sp.getInt(R.string.key_StagesmanualEnacts, 0) >= MANUAL_ENACTS_NEEDED)
                    return resourceHelper.gs(R.string.completed_well_done);
                else
                    return sp.getInt(R.string.key_StagesmanualEnacts, 0) + " / " + MANUAL_ENACTS_NEEDED;
            }
        });
    }

    @Override
    public boolean specialActionEnabled() {
        return nsClientPlugin.nsClientService.isConnected && nsClientPlugin.nsClientService.hasWriteAuth;
    }

    @Override
    public void specialAction(FragmentActivity activity, String input) {
        stagesPlugin.completeStages(activity, input);
    }
}
