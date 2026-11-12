package com.jmanc3.kakounebrain

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KakStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        withContext(Dispatchers.EDT) {
            // Acquiring the service first applies the persisted enabled/disabled state.
            KakPluginService.getInstance()
            if (!project.isDisposed && KakPluginService.isEnabled()) {
                KakPluginService.getInstance().activate()
            }
        }
    }
}
