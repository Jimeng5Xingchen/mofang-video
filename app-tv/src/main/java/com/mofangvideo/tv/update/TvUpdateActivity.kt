package com.mofangvideo.tv.update

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mofangvideo.core.update.UpdateChannel
import com.mofangvideo.feature.settings.UpdateUiState
import com.mofangvideo.feature.settings.UpdateViewModel
import com.mofangvideo.tv.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * TV 检查更新页：渠道选择（D-pad 左右）、检查、自动下载、安装引导。
 *
 * 复用 [UpdateViewModel]（Hilt），遥控中键 = 确认 / 安装，返回键退出。
 */
@AndroidEntryPoint
class TvUpdateActivity : ComponentActivity() {

    private val viewModel: UpdateViewModel by lazy {
        ViewModelProvider(this)[UpdateViewModel::class.java]
    }

    private lateinit var currentVersion: TextView
    private lateinit var channelView: TextView
    private lateinit var statusView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var notesLabel: TextView
    private lateinit var notesView: TextView
    private lateinit var checkButton: Button
    private lateinit var installButton: Button
    private lateinit var retryButton: Button

    private var readyFile: java.io.File? = null

    private val currentVersionName: String
        get() = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_update)

        currentVersion = findViewById(R.id.currentVersion)
        channelView = findViewById(R.id.channelView)
        statusView = findViewById(R.id.statusView)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        notesLabel = findViewById(R.id.notesLabel)
        notesView = findViewById(R.id.notesView)
        checkButton = findViewById(R.id.checkButton)
        installButton = findViewById(R.id.installButton)
        retryButton = findViewById(R.id.retryButton)

        currentVersion.text = getString(R.string.tv_update_current, currentVersionName)
        checkButton.setOnClickListener { viewModel.check(currentVersionName) }
        retryButton.setOnClickListener { viewModel.retry(currentVersionName) }
        installButton.setOnClickListener { installApk() }

        viewModel.channel.observe(this) { channel ->
            channelView.text = getString(
                if (channel == UpdateChannel.BETA) R.string.tv_update_channel_beta
                else R.string.tv_update_channel_stable
            )
        }

        viewModel.state.observe(this) { render(it) }
    }

    /** 左右键切换更新渠道（TV 无 Spinner，用 D-pad）。 */
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT ||
            keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT
        ) {
            val next = if (viewModel.channel.value == UpdateChannel.BETA)
                UpdateChannel.STABLE else UpdateChannel.BETA
            viewModel.setChannel(next)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun render(state: UpdateUiState) {
        checkButton.visibility = View.GONE
        retryButton.visibility = View.GONE
        installButton.visibility = View.GONE
        progressBar.visibility = View.GONE
        progressText.visibility = View.GONE
        notesLabel.visibility = View.GONE
        notesView.visibility = View.GONE

        when (state) {
            is UpdateUiState.Idle -> {
                statusView.setText(R.string.tv_update_check)
                checkButton.visibility = View.VISIBLE
            }
            is UpdateUiState.Checking -> {
                progressBar.isIndeterminate = true
                progressBar.visibility = View.VISIBLE
                statusView.setText(R.string.tv_update_checking)
            }
            is UpdateUiState.Latest -> {
                statusView.text = getString(R.string.tv_update_latest, state.version)
                checkButton.visibility = View.VISIBLE
            }
            is UpdateUiState.Available -> {
                statusView.text = getString(R.string.tv_update_available, state.update.versionName)
                progressBar.isIndeterminate = false
                progressBar.visibility = View.VISIBLE
                progressText.visibility = View.VISIBLE
                progressText.text =
                    getString(R.string.tv_update_size, state.update.apkSizeBytes / 1048576.0)
                notesLabel.visibility = View.VISIBLE
                notesView.visibility = View.VISIBLE
                notesView.text = state.update.releaseNotes.ifBlank { "—" }
            }
            is UpdateUiState.Downloading -> {
                progressBar.isIndeterminate = false
                progressBar.progress = state.percent
                progressBar.visibility = View.VISIBLE
                progressText.visibility = View.VISIBLE
                progressText.text = getString(R.string.tv_update_downloading, state.percent)
            }
            is UpdateUiState.ReadyToInstall -> {
                readyFile = state.file
                statusView.setText(R.string.tv_update_download_done)
                installButton.visibility = View.VISIBLE
            }
            is UpdateUiState.Failed -> {
                statusView.text = getString(R.string.tv_update_failed, state.message)
                retryButton.visibility = View.VISIBLE
            }
        }
    }

    private fun installApk() {
        val file = readyFile ?: return
        val ok = viewModel.install(this, file)
        if (!ok) {
            Toast.makeText(this, R.string.tv_update_need_permission, Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:$packageName"))
            )
        }
    }
}
