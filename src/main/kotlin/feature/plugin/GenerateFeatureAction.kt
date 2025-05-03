package feature.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import java.io.File

class GenerateFeatureAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val baseDir =
            e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE) ?: return

        val rawInput = Messages.showInputDialog(
            project,
            "Enter feature name (e.g. login):",
            "Compose Feature Generator",
            Messages.getQuestionIcon()
        ) ?: return

        val featureName = rawInput.lowercase() // folder and package
        val className = rawInput.replaceFirstChar { it.uppercaseChar() } // for class names

        val targetDir = File(baseDir.path, featureName).apply { mkdirs() }

        // Try to extract the correct package
        val srcIndex = baseDir.path.indexOf("src/")
        val packagePath = if (srcIndex != -1) {
            baseDir.path
                .substring(srcIndex)
                .substringAfter("/kotlin/")
                .replace("/", ".")
                .removeSuffix(".$featureName")
        } else {
            Messages.showErrorDialog(project, "Cannot determine package name", "Error")
            return
        }

        val fullFeaturePackage = "$packagePath.$featureName"

        // 0. Contract
        File(targetDir, "${className}Contract.kt").writeText(
            """
            package $fullFeaturePackage

            internal interface ${className}Contract {

                companion object Empty : ${className}Contract
            }
            """.trimIndent()
        )

        // 1. Screen
        File(targetDir, "${className}Screen.kt").writeText(
            """
            package $fullFeaturePackage

            import androidx.compose.runtime.Composable

            @Composable
            internal fun ${className}Screen() {
            }
            """.trimIndent()
        )

        // 2. ViewModel
        File(targetDir, "${className}ViewModel.kt").writeText(
            """
            package $fullFeaturePackage

            public class ${className}ViewModel : BaseViewModel<Unit, ${className}Direction>(Unit), ${className}Contract
            """.trimIndent()
        )

        // 3. Component
        File(targetDir, "${className}Component.kt").writeText(
            """
            package $fullFeaturePackage

            import com.arkivanov.decompose.ComponentContext
            import androidx.compose.runtime.Composable

            public class ${className}Component(
                componentContext: ComponentContext,
            ) : BaseComponent<${className}Direction>(componentContext) {

                override val viewModel = componentContext.retainedInstance {
                    ${className}ViewModel()
                }

                override suspend fun eventListener(rout: ${className}Direction) {
                }

                @Composable
                override fun Render() {
                    ${className}Screen()
                }
            }
            """.trimIndent()
        )

        // 4. Direction
        File(targetDir, "${className}Direction.kt").writeText(
            """
            package $fullFeaturePackage

            public sealed interface ${className}Direction : BaseDirection
            """.trimIndent()
        )

        VfsUtil.markDirtyAndRefresh(true, true, true, targetDir)
    }
}