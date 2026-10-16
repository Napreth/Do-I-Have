package com.napreth.doihave

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

internal class EntryAdapter(private val context: Context, private val style: UiStyle, private val permissionsPage: Boolean) : BaseAdapter() {
    private val permissions = mutableListOf<PermissionRepository.Entry>()
    private val operations = mutableListOf<OpsRepository.Entry>()

    fun updatePermissions(entries: List<PermissionRepository.Entry>, filter: PermissionFilter, search: String) {
        val query = search.trim().lowercase(Locale.ROOT)
        permissions.clear()
        permissions += entries.filter { filter.matches(it.state) && "${it.name} ${it.label}".lowercase(Locale.ROOT).contains(query) }
        notifyDataSetChanged()
    }

    fun updateOperations(entries: List<OpsRepository.Entry>, filter: OpsFilter, search: String) {
        val query = search.trim().lowercase(Locale.ROOT)
        operations.clear()
        operations += entries.filter { filter.matches(it.mode) && "${it.name} ${it.id}".lowercase(Locale.ROOT).contains(query) }
        notifyDataSetChanged()
    }

    fun permissionAt(position: Int) = permissions[position]
    fun operationAt(position: Int) = operations[position]
    override fun getCount() = if (permissionsPage) permissions.size else operations.size
    override fun getItem(position: Int): Any = if (permissionsPage) permissionAt(position) else operationAt(position)
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, recycled: View?, parent: ViewGroup): View = if (permissionsPage) {
        val holder = recycled?.tag as? PermissionRow ?: PermissionRow()
        holder.bind(permissionAt(position))
        holder.card
    } else {
        val holder = recycled?.tag as? OperationRow ?: OperationRow()
        holder.bind(operationAt(position))
        holder.card
    }

    private fun card(height: Int) = style.row().apply {
        setPadding(style.dp(16), style.dp(17), style.dp(14), style.dp(17))
        minimumHeight = style.dp(height)
        background = style.shape(style.surface, 16, true)
    }

    private inner class PermissionRow {
        val card = card(68)
        private val title = style.text("", 15, style.foreground, true)
        private val status = FrameLayout(context)
        private val icon = Ui.Icon(context, "arrow", style.accent)
        private val unknown = style.text(context.getString(R.string.grant_unknown), 11, style.secondary, false)
        private val grantedBackground = style.shape(style.tint, 16, false)

        init {
            title.maxLines = 3
            card.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
            status.addView(icon, FrameLayout.LayoutParams(style.dp(19), style.dp(19), Gravity.CENTER))
            unknown.gravity = Gravity.CENTER
            unknown.setSingleLine(true)
            unknown.setPadding(style.dp(4), 0, style.dp(4), 0)
            status.minimumWidth = style.dp(32)
            status.addView(unknown, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
            card.addView(status, LinearLayout.LayoutParams(-2, style.dp(32)).apply { leftMargin = style.dp(12) })
            card.tag = this
        }

        fun bind(entry: PermissionRepository.Entry) {
            title.text = entry.label
            icon.name = if (entry.granted) "check" else "arrow"
            icon.visibility = if (entry.state.authorization == GrantState.UNKNOWN) View.GONE else View.VISIBLE
            unknown.visibility = if (entry.state.authorization == GrantState.UNKNOWN) View.VISIBLE else View.GONE
            status.background = if (entry.granted) grantedBackground else null
            card.contentDescription = "${entry.label}, ${entry.name}, ${context.getString(entry.state.authorization.label)}"
        }
    }

    private inner class OperationRow {
        val card = card(82)
        private val number = style.text("", 11, style.accent, true)
        private val title = style.text("", 13, style.foreground, true)
        private val name = style.text("", 11, style.secondary, false)
        private val status = style.text("", 12, style.foreground, true)

        init {
            val details = style.column()
            val heading = style.row()
            number.setPadding(style.dp(8), style.dp(5), style.dp(8), style.dp(5))
            number.background = style.shape(style.tint, 7, false)
            heading.addView(number)
            title.typeface = Typeface.MONOSPACE
            title.maxLines = 2
            heading.addView(title, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = style.dp(10) })
            details.addView(heading)
            style.addSpace(details, 8)
            details.addView(name)
            card.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            status.id = R.id.op_status
            status.gravity = Gravity.CENTER
            status.maxWidth = style.dp(132)
            status.setPadding(style.dp(12), style.dp(8), style.dp(12), style.dp(8))
            status.background = style.shape(style.tint, 10, false)
            card.addView(status, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = style.dp(12) })
            card.tag = this
        }

        fun bind(entry: OpsRepository.Entry) {
            number.text = context.getString(R.string.op_number, entry.id)
            title.text = entry.simpleName
            name.text = entry.name
            val mode = OpsRepository.modeLabel(context, entry.mode)
            status.text = mode
            card.contentDescription = "#${entry.id}, ${entry.name}, $mode"
        }
    }
}

internal val GrantState.label: Int get() = when (this) {
    GrantState.GRANTED -> R.string.granted
    GrantState.DENIED -> R.string.not_granted
    GrantState.UNKNOWN -> R.string.grant_unknown
}
