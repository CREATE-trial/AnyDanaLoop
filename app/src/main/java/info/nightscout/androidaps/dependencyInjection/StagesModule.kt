package info.nightscout.androidaps.dependencyInjection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import info.nightscout.androidaps.plugins.constraints.stages.stages.*

@Module
@Suppress("unused")
abstract class StagesModule {

    @ContributesAndroidInjector abstract fun stageInjector(): Stage
    @ContributesAndroidInjector abstract fun stage0Injector(): Stage0
    @ContributesAndroidInjector abstract fun stage1Injector(): Stage1
    @ContributesAndroidInjector abstract fun stage2Injector(): Stage2
    @ContributesAndroidInjector abstract fun stage3Injector(): Stage3

}