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

        val featureName = rawInput.lowercase()
        val className = rawInput.replaceFirstChar { it.uppercaseChar() }

        val targetDir = File(baseDir.path, featureName).apply { mkdirs() }

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

        // 0. State
        File(targetDir, "${className}State.kt").writeText(
            """
            package $fullFeaturePackage

            import androidx.compose.runtime.Immutable

            @Immutable
            internal data object ${className}State
            """.trimIndent()
        )

        // 1. Contract
        File(targetDir, "${className}Contract.kt").writeText(
            """
            package $fullFeaturePackage

            internal interface ${className}Contract
            """.trimIndent()
        )

        // 2. Screen
        File(targetDir, "${className}Screen.kt").writeText(
            """
            package $fullFeaturePackage

            import androidx.compose.runtime.Composable
            import kotlinx.collections.immutable.ImmutableSet

            @Composable
            internal fun ${className}Screen(
                state: ${className}State,
                loaders: ImmutableSet<${className}Loader>,
                contract: ${className}Contract
            ) {
            }
            """.trimIndent()
        )

        // 3. ViewModel
        File(targetDir, "${className}ViewModel.kt").writeText(
            """
            package $fullFeaturePackage
    
            import com.grippo.core.BaseViewModel

            internal class ${className}ViewModel : BaseViewModel<${className}State, ${className}Direction, ${className}Loader>(${className}State), ${className}Contract {
            }
            """.trimIndent()
        )

        // 4. Component
        File(targetDir, "${className}Component.kt").writeText(
            """
            package $fullFeaturePackage

            import com.arkivanov.decompose.ComponentContext
            import androidx.compose.runtime.Composable
            import com.grippo.core.collectAsStateMultiplatform
            import com.grippo.core.BaseComponent
            import com.arkivanov.essenty.instancekeeper.retainedInstance

            internal class ${className}Component(
                componentContext: ComponentContext,
            ) : BaseComponent<${className}Direction>(componentContext) {

                override val viewModel = componentContext.retainedInstance {
                    ${className}ViewModel()
                }

                override suspend fun eventListener(rout: ${className}Direction) {
                }

                @Composable
                override fun Render() {
                    val state = viewModel.state.collectAsStateMultiplatform()
                    val loaders = viewModel.loaders.collectAsStateMultiplatform()
                    ${className}Screen(state.value, loaders.value, viewModel)
                }
            }
            """.trimIndent()
        )

        // 5. Direction
        File(targetDir, "${className}Direction.kt").writeText(
            """
            package $fullFeaturePackage
            
            import com.grippo.core.models.BaseDirection

            internal sealed interface ${className}Direction : BaseDirection
            """.trimIndent()
        )

        File(targetDir, "${className}Loader.kt").writeText(
            """
            package $fullFeaturePackage

            import com.grippo.core.models.BaseLoader

            internal sealed interface ${className}Loader : BaseLoader
            """.trimIndent()
        )

        VfsUtil.markDirtyAndRefresh(true, true, true, targetDir)
    }
}