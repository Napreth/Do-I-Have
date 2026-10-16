package com.napreth.doihave

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.max

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var permissions = emptyList<PermissionRepository.Entry>()
    private var operations = emptyList<OpsRepository.Entry>()
    private lateinit var preferences: SharedPreferences
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var empty: TextView
    private var adapter: EntryAdapter? = null
    private var page = 0
    private var permissionFilter = 0
    private var opsFilter = 0
    private var permissionQuery = ""
    private var opsQuery = ""
    private var loading = true
    private var loadFailed = false
    private lateinit var style: UiStyle
    private lateinit var permissionController: PermissionController
    private var generation = 0

    override fun attachBaseContext(base: Context) {
        val language = base.getSharedPreferences("preferences", MODE_PRIVATE).getString("language", "system") ?: "system"
        val configuration = Configuration(base.resources.configuration)
        if (language != "system") configuration.setLocale(Locale.forLanguageTag(language))
        super.attachBaseContext(base.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = getSharedPreferences("preferences", MODE_PRIVATE)
        val theme = preferences.getString("theme", "system")
        val dark = theme == "dark" || (theme == "system" &&
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        setTheme(if (dark) R.style.AppTheme_Dark else R.style.AppTheme)
        super.onCreate(savedInstanceState)
        style = UiStyle(this, dark)
        permissionController = PermissionController(this, preferences)
        savedInstanceState?.let {
            page = it.getInt("page").coerceIn(0, 2)
            permissionFilter = PermissionFilter.at(it.getInt("permissionFilter")).ordinal
            opsFilter = OpsFilter.at(it.getInt("opsFilter")).ordinal
            permissionQuery = it.getString("permissionQuery", "")
            opsQuery = it.getString("opsQuery", "")
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        refresh() // Read-only. Authorization is triggered exclusively by a row click.
    }

    override fun onDestroy() {
        generation++
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun onSaveInstanceState(state: Bundle) {
        state.putInt("page", page)
        state.putInt("permissionFilter", permissionFilter)
        state.putInt("opsFilter", opsFilter)
        state.putString("permissionQuery", permissionQuery)
        state.putString("opsQuery", opsQuery)
        super.onSaveInstanceState(state)
    }

    @Suppress("DEPRECATION")
    private fun render() {
        root = style.column().apply { setBackgroundColor(style.background) }
        val horizontal = max(style.dp(20), (resources.displayMetrics.widthPixels - style.dp(720)) / 2)
        root.setPadding(horizontal, 0, horizontal, 0)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val top: Int
            val bottom: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                top = bars.top
                bottom = max(bars.bottom, insets.getInsets(WindowInsets.Type.ime()).bottom)
            } else {
                top = insets.systemWindowInsetTop
                bottom = insets.systemWindowInsetBottom
            }
            view.setPadding(horizontal, top, horizontal, bottom)
            insets
        }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            (if (style.dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        if (Build.VERSION.SDK_INT >= 30) {
            val appearance = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (style.dark) 0 else appearance, appearance)
        }
        setContentView(root)
        root.requestApplyInsets()
        content = style.column().apply { setPadding(0, style.dp(12), 0, 0) }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        if (page == 2) SettingsPage(this, preferences, style).render(content) else renderList()
        renderNavigation()
    }

    private fun renderList() {
        val search = style.row().apply {
            background = style.shape(style.surface, 14, true)
            setPadding(style.dp(16), 0, style.dp(12), 0)
        }
        search.addView(Ui.Icon(this, "search", style.secondary), LinearLayout.LayoutParams(style.dp(20), style.dp(20)))
        val input = EditText(this).apply {
            id = R.id.search_input
            setSingleLine(true)
            textSize = 14f
            setTextColor(style.foreground)
            setHintTextColor(style.secondary)
            setPadding(style.dp(12), 0, 0, 0)
            setBackgroundColor(Color.TRANSPARENT)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_DONE
            setHint(if (page == 0) R.string.search_permissions else R.string.search_ops)
            setText(if (page == 0) permissionQuery else opsQuery)
        }
        val clear = FrameLayout(this).apply {
            id = R.id.clear_search
            contentDescription = getString(R.string.clear_search)
            addView(Ui.Icon(this@MainActivity, "close", style.secondary), FrameLayout.LayoutParams(style.dp(16), style.dp(16), Gravity.CENTER))
            visibility = if (input.length() == 0) View.GONE else View.VISIBLE
        }
        style.clickable(clear, Color.TRANSPARENT, 22) { input.setText("") }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                if (page == 0) permissionQuery = query else opsQuery = query
                clear.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                updateList()
            }
        })
        search.addView(input, LinearLayout.LayoutParams(0, style.dp(52), 1f))
        search.addView(clear, LinearLayout.LayoutParams(style.dp(44), style.dp(48)))
        content.addView(search)
        style.addSpace(content, 12)

        val filters = Ui.Flow(this, style.dp(8)).apply { id = R.id.state_filters }
        val labels = if (page == 0) PermissionFilter.labels() else OpsFilter.labels()
        labels.forEachIndexed { index, label ->
            val selected = index == if (page == 0) permissionFilter else opsFilter
            val chip = style.filterChip(label, selected) {
                if (page == 0) permissionFilter = index else opsFilter = index
                for (j in 0 until filters.childCount) {
                    filters.getChildAt(j).isSelected = j == index
                }
                updateList()
            }
            filters.addView(chip, ViewGroup.LayoutParams(-2, style.dp(48)))
        }
        content.addView(filters, LinearLayout.LayoutParams(-1, -2))
        style.addSpace(content, 12)
        val listArea = FrameLayout(this)
        val currentAdapter = EntryAdapter(this, style, page == 0)
        adapter = currentAdapter
        val list = ListView(this).apply {
            id = R.id.entry_list
            divider = ColorDrawable(Color.TRANSPARENT)
            dividerHeight = style.dp(9)
            setSelector(android.R.color.transparent)
            clipToPadding = false
            setPadding(0, 0, 0, style.dp(12))
            isVerticalScrollBarEnabled = false
            adapter = currentAdapter
            setOnItemClickListener { _, _, position, _ ->
                hideKeyboard()
                if (page == 0) permissionController.onClick(currentAdapter.permissionAt(position))
                else showOp(currentAdapter.operationAt(position))
            }
        }
        listArea.addView(list, FrameLayout.LayoutParams(-1, -1))
        empty = style.text("", 14, style.secondary, false).apply { gravity = Gravity.CENTER; setOnClickListener { refresh() } }
        listArea.addView(empty, FrameLayout.LayoutParams(-1, -1))
        list.emptyView = empty
        content.addView(listArea, LinearLayout.LayoutParams(-1, 0, 1f))
        updateList()
        content.isFocusableInTouchMode = true
        content.requestFocus()
    }

    private fun renderNavigation() {
        root.addView(View(this).apply { setBackgroundColor(style.outline) }, LinearLayout.LayoutParams(-1, style.dp(1)))
        val navigation = style.row().apply { isBaselineAligned = false; setPadding(0, style.dp(8), 0, style.dp(6)) }
        val labels = intArrayOf(R.string.permissions, R.string.ops, R.string.settings)
        val ids = intArrayOf(R.id.nav_permissions, R.id.nav_ops, R.id.nav_settings)
        val icons = arrayOf("shield", "ops", "settings")
        labels.forEachIndexed { index, label ->
            val color = if (page == index) style.accent else style.secondary
            val item = style.column().apply { id = ids[index]; gravity = Gravity.CENTER; contentDescription = getString(label); isSelected = page == index }
            val pill = FrameLayout(this).apply {
                background = style.shape(if (page == index) style.tint else Color.TRANSPARENT, 16, false)
                addView(Ui.Icon(this@MainActivity, icons[index], color), FrameLayout.LayoutParams(style.dp(22), style.dp(22), Gravity.CENTER))
            }
            item.addView(pill, LinearLayout.LayoutParams(style.dp(64), style.dp(32)))
            style.addSpace(item, 5)
            item.addView(style.text(getString(label), 12, color, page == index).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2))
            style.clickable(item, Color.TRANSPARENT, 16) { hideKeyboard(); if (page != index) { page = index; render() } }
            navigation.addView(item, LinearLayout.LayoutParams(0, style.dp(62), 1f))
        }
        root.addView(navigation)
    }

    private fun updateList() {
        if (page == 2) return
        val currentAdapter = adapter ?: return
        if (page == 0) currentAdapter.updatePermissions(permissions, PermissionFilter.at(permissionFilter), permissionQuery)
        else currentAdapter.updateOperations(operations, OpsFilter.at(opsFilter), opsQuery)
        empty.setText(if (loading) R.string.loading else if (loadFailed) R.string.load_failed else R.string.empty)
    }

    private fun refresh() {
        val request = ++generation
        loading = true
        updateList()
        worker.execute {
            try {
                val nextPermissions = PermissionRepository.load(this)
                val nextOperations = OpsRepository.load(this)
                runOnUiThread {
                    if (isDestroyed || request != generation) return@runOnUiThread
                    permissions = nextPermissions
                    operations = nextOperations
                    loading = false
                    loadFailed = false
                    updateList()
                }
            } catch (unavailable: Exception) {
                android.util.Log.e("DoIHave", "Could not read permission state", unavailable)
                runOnUiThread {
                    if (isDestroyed || request != generation) return@runOnUiThread
                    loading = false
                    loadFailed = true
                    updateList()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, names: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, names, results)
        if (requestCode == PermissionController.REQUEST_CODE) refresh()
    }

    private fun showOp(entry: OpsRepository.Entry) {
        var details = getString(R.string.op_detail_body, entry.name, OpsRepository.modeLabel(this, entry.mode),
            entry.permission.ifEmpty { getString(R.string.none) }, entry.permissionGrant?.let { getString(it.label) } ?: getString(R.string.none))
        if (PermissionPolicy.supportsSpecialSettings(entry.permission, Build.VERSION.SDK_INT)) {
            val state = PermissionRepository.state(this, entry.permission, PermissionRepository.Kind.SPECIAL)
            val capability = getString(when (state.capabilityAvailable) {
                true -> R.string.capability_available
                false -> R.string.capability_unavailable
                null -> R.string.grant_unknown
            })
            details += getString(R.string.special_access_details, getString(state.authorization.label), capability)
        }
        AlertDialog.Builder(this).setTitle(getString(R.string.op_details, entry.id)).setMessage(details)
            .setPositiveButton(R.string.close, null).show()
    }

    private fun hideKeyboard() {
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(root.windowToken, 0)
        root.clearFocus()
    }
}
