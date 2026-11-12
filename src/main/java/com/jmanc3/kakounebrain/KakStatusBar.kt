package com.jmanc3.kakounebrain

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon

/**
 * Status bar icons. IconLoader picks the *_dark.svg variants in dark themes automatically.
 */
object KakIcons {
    val enabled: Icon = IconLoader.getIcon("/icons/kakouneEnabled.svg", KakIcons::class.java)
    val disabled: Icon = IconLoader.getIcon("/icons/kakouneDisabled.svg", KakIcons::class.java)
}

/**
 * Registers the KakouneBrain on/off indicator in the status bar
 * (same model as IdeaVim's enable/disable status icon).
 */
class KakStatusBarIconFactory : StatusBarWidgetFactory {

    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = "KakouneBrain"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = KakStatusBarIcon()

    override fun disposeWidget(widget: StatusBarWidget) {
        // Nothing: the status bar disposes the widget itself.
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    /* Users can configure the icon via the status bar context menu. */
    override fun isConfigurable(): Boolean = true

    companion object {
        /** Must match the plugin.xml extension id attribute and the widget's ID(). */
        const val WIDGET_ID = "KakouneBrain-StatusIcon"

        /** Refresh the indicator on the status bar of every open project. Called after toggling. */
        @JvmStatic
        fun updateWidgets() {
            val projectManager = ProjectManager.getInstanceIfCreated() ?: return
            for (project in projectManager.openProjects) {
                val statusBar = WindowManager.getInstance().getStatusBar(project) ?: continue
                statusBar.updateWidget(WIDGET_ID)
            }
        }
    }
}

/**
 * Renders a status bar icon that mirrors the plugin's enabled state.
 * Clicking it shows a popup whose first (and checked) entry toggles the plugin.
 */
class KakStatusBarIcon : StatusBarWidget, StatusBarWidget.IconPresentation {

    override fun ID(): String = KakStatusBarIconFactory.WIDGET_ID

    override fun install(statusBar: StatusBar) {
        // Nothing
    }

    override fun dispose() {
        // Nothing
    }

    override fun getTooltipText(): String =
        if (KakPluginService.isEnabled()) "KakouneBrain (enabled)" else "KakouneBrain (disabled)"

    override fun getIcon(): Icon =
        if (KakPluginService.isEnabled()) KakIcons.enabled else KakIcons.disabled

    override fun getClickConsumer() = Consumer<MouseEvent> { event ->
        val component = event.component
        val dataContext = DataManager.getInstance().getDataContext(component)

        val actions = DefaultActionGroup()
        actions.isPopup = true
        ActionManager.getInstance().getAction(KakToggleAction.ID)?.let { actions.add(it) }

        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            "KakouneBrain",
            actions,
            dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false,
            ActionPlaces.POPUP,
        )

        val at = Point(0, -popup.content.preferredSize.height)
        popup.show(RelativePoint(component, at))
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
}
