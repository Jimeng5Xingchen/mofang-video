package com.mofangvideo.tv

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.mofangvideo.tv.browse.TvBrowseFragment
import com.mofangvideo.tv.focus.DpadKeyMapper
import com.mofangvideo.tv.focus.TvFocusEngine
import com.mofangvideo.tv.focus.TvFocusEvent
import com.mofangvideo.tv.focus.TvSection
import com.mofangvideo.tv.leanback.ClickTarget
import com.mofangvideo.tv.leanback.TvCard
import com.mofangvideo.tv.leanback.TvCardRow
import com.mofangvideo.tv.play.TvPlayActivity
import com.mofangvideo.tv.vmBackup
import com.mofangvideo.tv.vmFavorite
import com.mofangvideo.tv.vmHistory
import com.mofangvideo.tv.vmLive
import com.mofangvideo.tv.vmSettings
import com.mofangvideo.tv.vmVideo
import dagger.hilt.android.AndroidEntryPoint

/**
 * TV 主壳：顶栏 6 分节 Tab（遥控左右切换）+ 内容区（Leanback 行式 [BrowseFragment]）。
 *
 * 遥控 D-pad 为唯一主输入：
 * - 顶栏焦点时：左右切分节、中键 / 下键进入内容区。
 * - 内容区焦点时：上下切行、左右切卡片、中键触发卡片动作，由 [BrowseFragment] 焦点引擎处理；
 *   上键回到顶栏当前分节。
 */
@AndroidEntryPoint
class TvMainActivity : FragmentActivity() {

    private lateinit var focus: TvFocusEngine
    private lateinit var tabBar: LinearLayout
    private lateinit var tabBarItems: List<TextView>
    private lateinit var contentHost: ViewGroup
    private var activeFragment: TvBrowseFragment? = null
    private var contentFocused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        focus = TvFocusEngine()
        setContentView(R.layout.activity_tv_main)
        tabBar = findViewById(R.id.tvTabBar)
        contentHost = findViewById(R.id.tvContentHost)
        buildTabs()
        switchTo(TvSection.VIDEO, keepFocusOnHeader = true)
    }

    private fun buildTabs() {
        tabBarItems = TvSection.ALL.map { section ->
            val tab = TextView(this).apply {
                text = tabLabel(section)
                setTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_PX,
                    px(R.dimen.tv_tab_text_size).toFloat()
                )
                val h = px(R.dimen.tv_tab_height)
                val p = px(R.dimen.tv_section_padding)
                setPadding(p, 0, p, 0)
                setTextColor(0xFFE6E1E5.toInt())
                layoutParams = LinearLayout.LayoutParams(0, h, 1f)
                isFocusableInTouchMode = false
                isFocusable = true
            }
            tab.setOnClickListener { tabActivate(tab) }
            tabBar.addView(tab)
            tab
        }
        selectTab(0)
        tabBarItems[0].requestFocus()
    }

    private fun px(resId: Int): Int = resources.getDimensionPixelSize(resId)

    private fun tabLabel(section: TvSection): String = when (section) {
        TvSection.VIDEO -> getString(R.string.tv_tab_video)
        TvSection.LIVE -> getString(R.string.tv_tab_live)
        TvSection.FAVORITE -> getString(R.string.tv_tab_favorite)
        TvSection.HISTORY -> getString(R.string.tv_tab_history)
        TvSection.SETTINGS -> getString(R.string.tv_tab_settings)
        TvSection.BACKUP -> getString(R.string.tv_tab_backup)
    }

    private fun tabActivate(tab: TextView) {
        val idx = tabBarItems.indexOf(tab)
        if (idx >= 0) switchTo(TvSection.ALL[idx], keepFocusOnHeader = true)
    }

    private fun selectTab(index: Int) {
        tabBarItems.forEachIndexed { i, tab ->
            tab.setBackgroundResource(
                if (i == index) R.drawable.bg_tv_tab_selected else R.drawable.bg_tv_tab
            )
            tab.setTextColor(if (i == index) 0xFFD84DFFFF.toInt() else 0xFFE6E1E5.toInt())
        }
    }

    private fun switchTo(section: TvSection, keepFocusOnHeader: Boolean) {
        focus.setSection(section)
        selectTab(TvSection.ALL.indexOf(section))
        loadSectionData(section)
        activeFragment = browseFragmentFor(section)
        activeFragment?.onCardClick = ::handleCardClick
        val tx = supportFragmentManager.beginTransaction()
            .replace(R.id.tvContentHost, activeFragment!!, activeFragment!!.javaClass.name)
        tx.commitNow()
        contentFocused = false
        if (keepFocusOnHeader) {
            tabBarItems[TvSection.ALL.indexOf(section)].requestFocus()
        } else {
            contentHost.requestFocus()
        }
    }

    /** 进入分节前先刷新数据，让 [TvSectionRows] 取到最新值。 */
    private fun loadSectionData(section: TvSection) {
        when (section) {
            TvSection.VIDEO -> vmVideo().search("")
            TvSection.LIVE -> { vmLive().loadGroups(); vmLive().loadChannels() }
            TvSection.FAVORITE -> vmFavorite().loadAll()
            TvSection.HISTORY -> vmHistory().load()
            TvSection.SETTINGS -> Unit
            TvSection.BACKUP -> Unit
        }
    }

    private fun browseFragmentFor(section: TvSection): TvBrowseFragment {
        return TvBrowseFragment(tabLabel(section), browseRowsFor(section))
    }

    private fun browseRowsFor(section: TvSection): List<TvCardRow> = when (section) {
        TvSection.VIDEO -> TvVideoRows(this).rows()
        TvSection.LIVE -> TvLiveRows(this).rows()
        TvSection.FAVORITE -> TvFavoriteRows(this).rows()
        TvSection.HISTORY -> TvHistoryRows(this).rows()
        TvSection.SETTINGS -> TvSettingsRows(this).rows()
        TvSection.BACKUP -> TvBackupRows(this).rows()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (DpadKeyMapper.map(event.keyCode)) {
                TvFocusEvent.UP -> {
                    if (contentFocused && event.repeatCount == 0) {
                        contentFocused = false
                        tabBarItems[TvSection.ALL.indexOf(focus.section)].requestFocus()
                        return true
                    }
                }
                TvFocusEvent.DOWN -> {
                    if (!contentFocused && event.repeatCount == 0) {
                        contentFocused = true
                        contentHost.requestFocus()
                        return true
                    }
                }
                TvFocusEvent.LEFT -> {
                    if (!contentFocused && focus.moveLeft()) {
                        switchTo(focus.section, keepFocusOnHeader = true)
                        return true
                    }
                }
                TvFocusEvent.RIGHT -> {
                    if (!contentFocused && focus.moveRight()) {
                        switchTo(focus.section, keepFocusOnHeader = true)
                        return true
                    }
                }
                else -> Unit
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /** 卡片中键：按 [ClickTarget] 打开播放页。 */
    fun handleCardClick(card: TvCard) {
        when (val target = card.onClick) {
            is ClickTarget.OpenVideo ->
                startActivity(
                    Intent(this, TvPlayActivity::class.java).apply {
                        putExtra(TvPlayActivity.EXTRA_SOURCE_ID, target.sourceId)
                        putExtra(TvPlayActivity.EXTRA_VIDEO_ID, target.videoId)
                    }
                )
            is ClickTarget.OpenLive ->
                startActivity(
                    Intent(this, TvPlayActivity::class.java).apply {
                        putExtra(TvPlayActivity.EXTRA_CHANNEL_ID, target.channel.id)
                        putExtra(TvPlayActivity.EXTRA_CHANNEL_URL, target.channel.url)
                        putExtra(TvPlayActivity.EXTRA_CHANNEL_NAME, target.channel.name)
                        putExtra(TvPlayActivity.EXTRA_CHANNEL_LOGO, target.channel.logo)
                        putExtra(TvPlayActivity.EXTRA_IS_LIVE, true)
                    }
                )
            ClickTarget.None -> Unit
        }
    }
}
