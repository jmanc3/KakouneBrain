package com.jmanc3.kakounebrain

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction

/**
 * Enables/disables Kakoune editing. Shared by the Tools menu and the status bar
 * icon popup (registered as "KakouneBrain.Toggle" in plugin.xml).
 */
class KakToggleAction : DumbAwareToggleAction() {

    override fun isSelected(event: AnActionEvent): Boolean = KakPluginService.isEnabled()

    override fun setSelected(event: AnActionEvent, state: Boolean) {
        KakPluginService.getInstance().setEnabled(state)
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        event.presentation.text = if (KakPluginService.isEnabled()) {
            "Disable"
        } else {
            "Enable"
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        /** Action id registered in plugin.xml; looked up by the status bar popup. */
        const val ID = "KakouneBrain.Toggle"
    }
}
