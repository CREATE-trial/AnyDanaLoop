package info.nightscout.androidaps.plugins.constraints.stages.stages;

import java.util.List;

import javax.inject.Inject;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.plugins.configBuilder.ConstraintChecker;
import info.nightscout.androidaps.utils.DateUtil;
import info.nightscout.androidaps.utils.T;

public class Stage2 extends Stage {
    @Inject ConstraintChecker constraintChecker;

    public Stage2(HasAndroidInjector injector) {
        super(injector, "maxiob", R.string.stages_maxiob_stage, R.string.stages_maxiob_gate);
    }

    @Override
    protected void setupTasks(List<Task> tasks) {
        tasks.add(new Task(R.string.fourteendaywait) {
            @Override public boolean isCompleted() {
                long dateappstarted = sp.getLong("Stages_" + "openloop" + "_started",0L);
                long waittime = T.days(14).msecs();
                long completetime = dateappstarted + waittime;
                boolean complete = false;

                if(DateUtil.now() >= completetime) {
                    complete = true;
                }
                return complete;
            }
            @Override
            public String getProgress() {
                long dateappstarted = sp.getLong("Stages_" + "openloop" + "_started",0L);
                long waittime = T.days(14).msecs();
                long completetime = dateappstarted + waittime;


                if (DateUtil.now() >= completetime)
                    return resourceHelper.gs(R.string.completed_well_done);
                else {
                    long days = ((waittime-(completetime - DateUtil.now()))/(24 * 60 * 60 * 1000));
                    return days + " / 14 days";
                }
            }
        });
        tasks.add(new Task(R.string.maxiobset) {
            @Override
            public boolean isCompleted() {
                double maxIOB = constraintChecker.getMaxIOBAllowed().value();
                return maxIOB > 0;
            }
        });
    }
}
