package info.nightscout.androidaps.dependencyInjection

import android.content.Context
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.Config
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.db.DatabaseHelperProvider
import info.nightscout.androidaps.interfaces.*
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin
import info.nightscout.androidaps.plugins.configBuilder.PluginStore
import info.nightscout.androidaps.plugins.general.nsclient.UploadQueue
import info.nightscout.androidaps.plugins.treatments.TreatmentsPlugin
import info.nightscout.androidaps.queue.CommandQueue
import info.nightscout.androidaps.utils.buildHelper.BuildHelper
import info.nightscout.androidaps.utils.androidNotification.NotificationHolder
import info.nightscout.androidaps.utils.storage.FileStorage
import info.nightscout.androidaps.utils.storage.Storage
import javax.inject.Singleton

@Module(includes = [
    AppModule.AppBindings::class
])
open class AppModule {
    private val buildHelper = BuildHelper(Config())

    @Provides
    fun providesPlugins(configInterface: ConfigInterface,
                        @PluginsModule.AllConfigs allConfigs: Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>,
                        @PluginsModule.PumpDriver pumpDrivers: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>,
                        @PluginsModule.NotNSClient notNsClient: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>,
                        @PluginsModule.Engineer engineer: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>,
                        @PluginsModule.Ultrarapid ultrarapid: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>,
                        @PluginsModule.Xdrip xdrip: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>,
                        @PluginsModule.NSBG nsbg: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>,
                        @PluginsModule.VirtualPump virtualpump: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>,
                        @PluginsModule.Medtronic medtronic: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>,
                        @PluginsModule.APS aps: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>)
        : List<@JvmSuppressWildcards PluginBase> {
        val plugins = allConfigs.toMutableMap()
        if (configInterface.PUMPDRIVERS) plugins += pumpDrivers.get()
        if (configInterface.APS) plugins += aps.get()
        if (!configInterface.NSCLIENT) plugins += notNsClient.get()

        if (buildHelper.isEngineeringMode()) plugins += engineer.get()

        if (buildHelper.isUltraRapidenabled()) plugins += ultrarapid.get()

        if (buildHelper.isxDripenabled()) plugins += xdrip.get()

        if (buildHelper.isNSBGenabled()) plugins += nsbg.get()

        if (buildHelper.isMedtronicenabled()) plugins += medtronic.get()

        if (!configInterface.APS) plugins += virtualpump.get()

        return plugins.toList().sortedBy { it.first }.map { it.second }
    }

    @Provides
    @Singleton
    fun provideStorage(): Storage {
        return FileStorage()
    }

    @Module
    interface AppBindings {

        @Binds fun bindContext(mainApp: MainApp): Context
        @Binds fun bindInjector(mainApp: MainApp): HasAndroidInjector
        @Binds fun bindActivePluginProvider(pluginStore: PluginStore): ActivePluginProvider
        @Binds fun bindCommandQueueProvider(commandQueue: CommandQueue): CommandQueueProvider
        @Binds fun bindConfigInterface(config: Config): ConfigInterface
        @Binds fun bindConfigBuilderInterface(configBuilderPlugin: ConfigBuilderPlugin): ConfigBuilderInterface
        @Binds fun bindTreatmentInterface(treatmentsPlugin: TreatmentsPlugin): TreatmentsInterface
        @Binds fun bindDatabaseHelperInterface(databaseHelperProvider: DatabaseHelperProvider): DatabaseHelperInterface
        @Binds fun bindUploadQueueInterface(uploadQueue: UploadQueue): UploadQueueInterface
        @Binds fun bindNotificationHolderInterface(notificationHolder: NotificationHolder): NotificationHolderInterface
    }
}
