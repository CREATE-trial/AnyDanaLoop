package info.nightscout.androidaps.utils.buildHelper

import info.nightscout.androidaps.BuildConfig
import info.nightscout.androidaps.Config
import info.nightscout.androidaps.plugins.general.maintenance.LoggerUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildHelper @Inject constructor(private val config: Config) {

    private var devBranch = false
    private var engineeringMode = false
    private var xdrip = false
    private var ultrarapid = false
    private var nsBG = false
    private var medtronic = false
    private var virtualpump = false

    init {
        val extFilesDir = LoggerUtils.getLogDirectory()
        val engineeringModeSemaphore = File(extFilesDir, "admin_mode")
        val xdripModeSemaphore = File(extFilesDir, "xdrip")
        val ultrarapidModeSemaphore = File(extFilesDir, "ultra_rapid")
        val nsBGSemaphore = File(extFilesDir, "ns_bg")
        val medtronicSemaphore = File(extFilesDir, "medtronic")
        val virtualpumpSemaphore = File(extFilesDir, "virtual_pump")

        engineeringMode = engineeringModeSemaphore.exists() && engineeringModeSemaphore.isFile

        xdrip = xdripModeSemaphore.exists() && xdripModeSemaphore.isFile
        ultrarapid = ultrarapidModeSemaphore.exists() && ultrarapidModeSemaphore.isFile
        nsBG = nsBGSemaphore.exists() && nsBGSemaphore.isFile
        medtronic = medtronicSemaphore.exists() && medtronicSemaphore.isFile
        virtualpump = virtualpumpSemaphore.exists() && virtualpumpSemaphore.isFile

    }

    fun isEngineeringMode(): Boolean = engineeringMode

    fun isxDripenabled(): Boolean = xdrip

    fun isUltraRapidenabled(): Boolean = ultrarapid

    fun isNSBGenabled(): Boolean = nsBG

    fun isMedtronicenabled(): Boolean = medtronic

    fun isVirtualPumpenabled(): Boolean = virtualpump

    fun isDev(): Boolean = devBranch
}
