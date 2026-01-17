/*
 * CodeNodeIO IDE Plugin
 * IntelliJ Platform plugin integration
 * License: Apache 2.0
 */

package io.codenode.ideplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class CodeNodeIOStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Plugin initialization will be implemented in Phase 1
    }
}

