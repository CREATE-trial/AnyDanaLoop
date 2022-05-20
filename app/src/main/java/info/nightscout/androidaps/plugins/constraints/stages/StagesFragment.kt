package info.nightscout.androidaps.plugins.constraints.stages

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerFragment
import info.nightscout.androidaps.R
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.plugins.bus.RxBusWrapper
import info.nightscout.androidaps.dialogs.NtpProgressDialog
import info.nightscout.androidaps.events.EventNtpStatus
import info.nightscout.androidaps.plugins.constraints.stages.events.EventStagesUpdateGui
import info.nightscout.androidaps.plugins.constraints.stages.stages.Stage.ExamTask
import info.nightscout.androidaps.receivers.ReceiverStatusStore
import info.nightscout.androidaps.setupwizard.events.EventSWUpdate
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.HtmlHelper
import info.nightscout.androidaps.utils.alertDialogs.OKDialog
import info.nightscout.androidaps.utils.SntpClient
import info.nightscout.androidaps.utils.extensions.plusAssign
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.androidaps.utils.sharedPreferences.SP
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.stages_fragment.*
import javax.inject.Inject

class StagesFragment : DaggerFragment() {
    @Inject lateinit var rxBus: RxBusWrapper
    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var sp: SP
    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var fabricPrivacy: FabricPrivacy
    @Inject lateinit var stagesPlugin: StagesPlugin
    @Inject lateinit var receiverStatusStore: ReceiverStatusStore
    @Inject lateinit var dateUtil: DateUtil
    @Inject lateinit var sntpClient: SntpClient

    private val stagesAdapter = StagesAdapter()
    private val handler = Handler(Looper.getMainLooper())

    private var disposable: CompositeDisposable = CompositeDisposable()

    private val stageUpdater = object : Runnable {
        override fun run() {
            handler.postDelayed(this, (60 * 1000).toLong())
            updateGUI()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stages_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stages_recyclerview.layoutManager = LinearLayoutManager(view.context)
        stages_recyclerview.adapter = stagesAdapter
        stages_fake.setOnClickListener { updateGUI() }
        stages_reset.setOnClickListener {
            activity?.let { activity ->
                OKDialog.showConfirmation(activity, resourceHelper.gs(R.string.stages), resourceHelper.gs(R.string.doyouwantresetstartall), Runnable {
                    stagesPlugin.reset()
                    stages_recyclerview.adapter?.notifyDataSetChanged()
                    scrollToCurrentStage()
                })
            }
        }
        scrollToCurrentStage()
        startUpdateTimer()
    }

    @Synchronized
    override fun onResume() {
        super.onResume()
        disposable += rxBus
            .toObservable(EventStagesUpdateGui::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                stages_recyclerview.adapter?.notifyDataSetChanged()
            }, { fabricPrivacy.logException(it) }
            )
    }

    @Synchronized
    override fun onPause() {
        super.onPause()
        disposable.clear()
    }

    @Synchronized
    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(stageUpdater)
    }

    private fun startUpdateTimer() {
        handler.removeCallbacks(stageUpdater)
        for (stage in stagesPlugin.stages) {
            if (stage.isStarted && !stage.isAccomplished) {
                val timeTillNextMinute = (System.currentTimeMillis() - stage.startedOn) % (60 * 1000)
                handler.postDelayed(stageUpdater, timeTillNextMinute)
                break
            }
        }
    }

    private fun scrollToCurrentStage() {
        activity?.runOnUiThread {
            for (i in 0 until stagesPlugin.stages.size) {
                val stage = stagesPlugin.stages[i]
                if (!stage.isStarted || !stage.isAccomplished) {
                    context?.let {
                        val smoothScroller = object : LinearSmoothScroller(it) {
                            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
                            override fun calculateTimeForScrolling(dx: Int): Int = super.calculateTimeForScrolling(dx) * 4
                        }
                        smoothScroller.targetPosition = i
                        stages_recyclerview.layoutManager?.startSmoothScroll(smoothScroller)
                    }
                    break
                }
            }
        }
    }

    private inner class StagesAdapter : RecyclerView.Adapter<StagesAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.stages_item, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val stage = stagesPlugin.stages[position]
            holder.title.text = resourceHelper.gs(R.string.nth_stage, position + 1)
            if (stage.stage != 0) {
                holder.stage.visibility = View.VISIBLE
                holder.stage.text = resourceHelper.gs(stage.stage)
            } else
                holder.stage.visibility = View.GONE
            if (stage.gate != 0) {
                holder.gate.visibility = View.VISIBLE
                holder.gate.text = resourceHelper.gs(stage.gate)
            } else
                holder.gate.visibility = View.GONE
            if (!stage.isStarted) {
                holder.gate.setTextColor(-0x1)
                holder.verify.visibility = View.GONE
                holder.progress.visibility = View.GONE
                holder.accomplished.visibility = View.GONE
                holder.unFinish.visibility = View.GONE
                holder.unStart.visibility = View.GONE
                if (position == 0 || stagesPlugin.allPriorAccomplished(position))
                    holder.start.visibility = View.VISIBLE
                else
                    holder.start.visibility = View.GONE
            } else if (stage.isAccomplished) {
                holder.gate.setTextColor(-0xb350b0)
                holder.verify.visibility = View.GONE
                holder.progress.visibility = View.GONE
                holder.start.visibility = View.GONE
                holder.accomplished.visibility = View.VISIBLE
                holder.unFinish.visibility = View.VISIBLE
                holder.unStart.visibility = View.GONE
            } else if (stage.isStarted) {
                holder.gate.setTextColor(-0x1)
                holder.verify.visibility = View.VISIBLE
                holder.verify.isEnabled = stage.isCompleted || stages_fake.isChecked
                holder.start.visibility = View.GONE
                holder.accomplished.visibility = View.GONE
                holder.unFinish.visibility = View.GONE
                holder.unStart.visibility = View.VISIBLE
                holder.progress.visibility = View.VISIBLE
                holder.progress.removeAllViews()
                for (task in stage.tasks) {
                    if (task.shouldBeIgnored()) continue
                    // name
                    val name = TextView(holder.progress.context)
                    @Suppress("SetTextlI8n")
                    name.text = resourceHelper.gs(task.task) + ":"
                    name.setTextColor(-0x1)
                    holder.progress.addView(name, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    // hint
                    task.hints.forEach { h ->
                        if (!task.isCompleted)
                            holder.progress.addView(h.generate(context))
                    }
                    // state
                    val state = TextView(holder.progress.context)
                    state.setTextColor(-0x1)
                    val basicHTML = "<font color=\"%1\$s\"><b>%2\$s</b></font>"
                    val formattedHTML = String.format(basicHTML, if (task.isCompleted) "#4CAF50" else "#FF9800", task.progress)
                    state.text = HtmlHelper.fromHtml(formattedHTML)
                    state.gravity = Gravity.END
                    holder.progress.addView(state, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    // horizontal line
                    val separator = View(holder.progress.context)
                    separator.setBackgroundColor(Color.DKGRAY)
                    holder.progress.addView(separator, LinearLayout.LayoutParams.MATCH_PARENT, 2)
                }
            }
            holder.accomplished.text = resourceHelper.gs(R.string.accomplished, dateUtil.dateAndTimeString(stage.accomplishedOn))
            holder.accomplished.setTextColor(-0x3e3e3f)
            holder.verify.setOnClickListener {
                receiverStatusStore.updateNetworkStatus()
                if (stages_fake.isChecked) {
                    stage.accomplishedOn = DateUtil.now()
                    scrollToCurrentStage()
                    startUpdateTimer()
                    rxBus.send(EventStagesUpdateGui())
                    rxBus.send(EventSWUpdate(false))
                } else {
                    // move out of UI thread
                    Thread {
                        NtpProgressDialog().show((context as AppCompatActivity).supportFragmentManager, "NtpCheck")
                        rxBus.send(EventNtpStatus(resourceHelper.gs(R.string.timedetection), 0))
                        sntpClient.ntpTime(object : SntpClient.Callback() {
                            override fun run() {
                                aapsLogger.debug("NTP time: $time System time: ${DateUtil.now()}")
                                SystemClock.sleep(300)
                                if (!networkConnected) {
                                    rxBus.send(EventNtpStatus(resourceHelper.gs(R.string.notconnected), 99))
                                } else if (success) {
                                    if (stage.isCompleted(time)) {
                                        stage.accomplishedOn = time
                                        rxBus.send(EventNtpStatus(resourceHelper.gs(R.string.success), 100))
                                        SystemClock.sleep(1000)
                                        rxBus.send(EventStagesUpdateGui())
                                        rxBus.send(EventSWUpdate(false))
                                        SystemClock.sleep(100)
                                        scrollToCurrentStage()
                                    } else {
                                        rxBus.send(EventNtpStatus(resourceHelper.gs(R.string.requirementnotmet), 99))
                                    }
                                } else {
                                    rxBus.send(EventNtpStatus(resourceHelper.gs(R.string.failedretrievetime), 99))
                                }
                            }
                        }, receiverStatusStore.isConnected)
                    }.start()
                }
            }
            holder.start.setOnClickListener {
                receiverStatusStore.updateNetworkStatus()
                if (stages_fake.isChecked) {
                    stage.startedOn = DateUtil.now()
                    scrollToCurrentStage()
                    startUpdateTimer()
                    rxBus.send(EventStagesUpdateGui())
                    rxBus.send(EventSWUpdate(false))
                } else
                // move out of UI thread
                    Thread {
                        NtpProgressDialog().show((context as AppCompatActivity).supportFragmentManager, "NtpCheck")
                        rxBus.send(EventNtpStatus(resourceHelper.gs(R.string.timedetection), 0))
                        sntpClient.ntpTime(object : SntpClient.Callback() {
                            override fun run() {
                                aapsLogger.debug("NTP time: $time System time: ${DateUtil.now()}")
                                SystemClock.sleep(300)
                                if (!networkConnected) {
                                    rxBus.send(EventNtpStatus(resourceHelper.gs(R.string.notconnected), 99))
                                } else if (success) {
                                    stage.startedOn = time
                                    rxBus.send(EventNtpStatus(resourceHelper.gs(R.string.success), 100))
                                    SystemClock.sleep(1000)
                                    rxBus.send(EventStagesUpdateGui())
                                    rxBus.send(EventSWUpdate(false))
                                    SystemClock.sleep(100)
                                    scrollToCurrentStage()
                                } else {
                                    rxBus.send(EventNtpStatus(resourceHelper.gs(R.string.failedretrievetime), 99))
                                }
                            }
                        }, receiverStatusStore.isConnected)
                    }.start()
            }
            holder.unStart.setOnClickListener {
                activity?.let { activity ->
                    OKDialog.showConfirmation(activity, resourceHelper.gs(R.string.stages), resourceHelper.gs(R.string.doyouwantresetstart), Runnable {
                        stage.startedOn = 0
                        scrollToCurrentStage()
                        rxBus.send(EventStagesUpdateGui())
                        rxBus.send(EventSWUpdate(false))
                    })
                }
            }
            holder.unFinish.setOnClickListener {
                stage.accomplishedOn = 0
                scrollToCurrentStage()
                rxBus.send(EventStagesUpdateGui())
                rxBus.send(EventSWUpdate(false))
            }
            if (stage.hasSpecialInput && !stage.isAccomplished && stage.isStarted && stage.specialActionEnabled()) {
                // generate random request code if none exists
                val request = sp.getString(R.string.key_stages_request_code, String.format("%1$05d", (Math.random() * 99999).toInt()))
                sp.putString(R.string.key_stages_request_code, request)
                holder.requestCode.text = resourceHelper.gs(R.string.requestcode, request)
                holder.requestCode.visibility = View.VISIBLE
                holder.enterButton.visibility = View.VISIBLE
                holder.input.visibility = View.VISIBLE
                holder.inputHint.visibility = View.VISIBLE
                holder.enterButton.setOnClickListener {
                    val input = holder.input.text.toString()
                    stage.specialAction(activity, input)
                    rxBus.send(EventStagesUpdateGui())
                }
            } else {
                holder.enterButton.visibility = View.GONE
                holder.input.visibility = View.GONE
                holder.inputHint.visibility = View.GONE
                holder.requestCode.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int {
            return stagesPlugin.stages.size
        }

        inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.stage_title)
            val stage: TextView = itemView.findViewById(R.id.stage_stage)
            val gate: TextView = itemView.findViewById(R.id.stage_gate)
            val accomplished: TextView = itemView.findViewById(R.id.stage_accomplished)
            val progress: LinearLayout = itemView.findViewById(R.id.stage_progress)
            val verify: Button = itemView.findViewById(R.id.stage_verify)
            val start: Button = itemView.findViewById(R.id.stage_start)
            val unFinish: Button = itemView.findViewById(R.id.stage_unfinish)
            val unStart: Button = itemView.findViewById(R.id.stage_unstart)
            val inputHint: TextView = itemView.findViewById(R.id.stage_inputhint)
            val input: EditText = itemView.findViewById(R.id.stage_input)
            val enterButton: Button = itemView.findViewById(R.id.stage_enterbutton)
            val requestCode: TextView = itemView.findViewById(R.id.stage_requestcode)
        }
    }

    fun updateGUI() {
        activity?.runOnUiThread { stagesAdapter.notifyDataSetChanged() }
    }
}
