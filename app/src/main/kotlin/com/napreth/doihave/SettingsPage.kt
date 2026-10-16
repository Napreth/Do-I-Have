package com.napreth.doihave

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import java.io.IOException

internal class SettingsPage(private val activity: Activity, private val preferences: SharedPreferences, private val style: UiStyle) {
    fun render(content: LinearLayout) {
        content.addView(style.text(activity.getString(R.string.settings), 24, style.foreground, true))
        style.addSpace(content, 24)
        val items = style.column().apply { setPadding(0, 0, 0, style.dp(20)) }
        val scroll = ScrollView(activity).apply { isFillViewport = true; isVerticalScrollBarEnabled = false; addView(items) }
        content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        section(items, R.string.preferences)
        val language = preferences.getString("language", "system")
        settingsRow(items, R.id.setting_language, "globe", R.string.language, when (language) {
            "zh" -> "简体中文"
            "en" -> "English"
            else -> activity.getString(R.string.follow_system)
        }, ::chooseLanguage)
        val theme = preferences.getString("theme", "system")
        settingsRow(items, R.id.setting_theme, "theme", R.string.theme,
            activity.getString(when (theme) { "dark" -> R.string.dark; "light" -> R.string.light; else -> R.string.follow_system }), ::chooseTheme)
        style.addSpace(items, 24)
        section(items, R.string.about)
        settingsRow(items, View.NO_ID, "info", R.string.version,
            BuildConfig.VERSION_NAME, null)
        settingsRow(items, View.NO_ID, "code", R.string.github, activity.getString(R.string.github_note)) {
            openLink("https://github.com/Napreth/Do-I-Have")
        }
        settingsRow(items, R.id.setting_license, "shield", R.string.license, "MIT", ::showLicense)
    }

    private fun section(parent: LinearLayout, label: Int) {
        parent.addView(style.text(activity.getString(label), 11, style.accent, true).apply { letterSpacing = 0.07f })
        style.addSpace(parent, 12)
    }

    private fun settingsRow(parent: LinearLayout, id: Int, icon: String, title: Int, subtitle: String, action: (() -> Unit)?) {
        val item = style.row().apply {
            this.id = id
            setPadding(style.dp(16), style.dp(18), style.dp(16), style.dp(18))
            minimumHeight = style.dp(80)
        }
        item.addView(Ui.Icon(activity, icon, style.accent), LinearLayout.LayoutParams(style.dp(23), style.dp(23)))
        val words = style.column()
        words.addView(style.text(activity.getString(title), 15, style.foreground, true))
        style.addSpace(words, 7)
        words.addView(style.text(subtitle, 12, style.secondary, false))
        item.addView(words, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = style.dp(16) })
        if (action != null) {
            item.addView(Ui.Icon(activity, "arrow", style.secondary), LinearLayout.LayoutParams(style.dp(18), style.dp(18)))
            style.clickable(item, style.surface, 16, action)
        } else item.background = style.shape(style.surface, 16, false)
        parent.addView(item, LinearLayout.LayoutParams(-1, -2))
        style.addSpace(parent, 9)
    }

    private fun chooseLanguage() = choose("language", R.string.language, arrayOf("system", "zh", "en"),
        arrayOf(activity.getString(R.string.follow_system), "简体中文", "English"))

    private fun chooseTheme() = choose("theme", R.string.theme, arrayOf("system", "light", "dark"),
        arrayOf(activity.getString(R.string.follow_system), activity.getString(R.string.light), activity.getString(R.string.dark)))

    private fun choose(key: String, title: Int, values: Array<String>, labels: Array<String>) {
        AlertDialog.Builder(activity).setTitle(title)
            .setSingleChoiceItems(labels, values.indexOf(preferences.getString(key, "system"))) { dialog, which ->
                preferences.edit().putString(key, values[which]).apply()
                dialog.dismiss()
                activity.recreate()
            }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun showLicense() {
        try {
            val license = activity.assets.open("LICENSE").bufferedReader(Charsets.UTF_8).use { it.readText() }
            AlertDialog.Builder(activity).setTitle("MIT License").setMessage(license).setPositiveButton(R.string.close, null).show()
        } catch (_: IOException) { openLink("https://opensource.org/license/mit") }
    }

    private fun openLink(url: String) {
        try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: ActivityNotFoundException) { Toast.makeText(activity, url, Toast.LENGTH_LONG).show() }
        catch (_: SecurityException) { Toast.makeText(activity, url, Toast.LENGTH_LONG).show() }
    }
}
