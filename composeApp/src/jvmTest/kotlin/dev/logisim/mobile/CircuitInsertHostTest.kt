package dev.logisim.mobile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import dev.logisim.mobile.core.localization.AppLanguage
import dev.logisim.mobile.core.localization.Messages
import dev.logisim.mobile.core.localization.message
import dev.logisim.mobile.localization.LocalAppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Inserting a project circuit from the Circuits tab at phone size (#368), with `examples/hierarchy.circ`: Insert on
 * the Inversor row picks its add tool and closes the sheet, a canvas tap places a second Inversor in Hierarchy, and
 * Hierarchy cannot be inserted into itself.
 */
@OptIn(ExperimentalTestApi::class)
class CircuitInsertHostTest {
    private val english = AppLanguage.ENGLISH
    private val hierarchy = HostScreenshots.fixture("examples/hierarchy.circ")

    private fun t(key: String, vararg arguments: Pair<String, Any?>) = message(key, *arguments).resolve(english)

    private fun ComposeUiTest.app(state: EditorState) {
        val header = EditorWorkspaceHeader("", "").also {
            it.session = WorkspaceEditorSessionBinding(state, state.exportViewState(), retain = {}, open = { _, _, _ -> false })
        }
        setContent {
            CompositionLocalProvider(LocalAppLanguage provides english) {
                LogisimTheme(dark = false) { App(initialXml = hierarchy, language = english, workspaceHeader = header) }
            }
        }
        waitForIdle()
        if (exists("editor-import-report")) {
            onAllNodes(hasText(t(Messages.ActionClose)) and hasClickAction())[0].performClick()
            waitForIdle()
        }
    }

    private fun ComposeUiTest.exists(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun SemanticsNodeInteraction.enabled() = fetchSemanticsNode().config.getOrNull(SemanticsProperties.Disabled) == null
    private fun SemanticsNodeInteraction.description() =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
    private fun SemanticsNodeInteraction.state() = fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
    private fun SemanticsNodeInteraction.tall() = getUnclippedBoundsInRoot().let { it.bottom - it.top } >= 48.dp

    private fun ComposeUiTest.openCircuitsTab() {
        onNodeWithTag("tool-library").performClick()
        waitUntil(timeoutMillis = 10_000) { exists("editor-library-sheet") }
        onNodeWithContentDescription(t("a11y_show_tree")).performClick()
        waitForIdle()
    }

    private fun EditorState.screen(x: Float, y: Float): Offset =
        Offset(x * viewport.view.scale + viewport.view.pan.x, y * viewport.view.scale + viewport.view.pan.y)

    private fun EditorState.inversors() = document.components.filter { it.circuitName == "Inversor" }

    private fun ComposeUiTest.tapCanvas(state: EditorState, x: Float, y: Float) {
        onNodeWithTag("editor-canvas").performTouchInput { click(state.screen(x, y)) }
        waitForIdle()
    }

    @Test
    fun phoneInsertButtonPlacesASecondInversorInHierarchy() = runDesktopComposeUiTest(390, 844) {
        val state = EditorState(hierarchy)
        app(state)
        assertEquals("Hierarchy", state.document.name)
        val target = state.activeTarget
        val existing = state.inversors().single()
        val inversor = state.project.circuit("Inversor")!!.id
        val main = state.project.circuit("Hierarchy")!!.id
        openCircuitsTab()
        screenshot("circuit-insert-1-circuits-tab")

        val self = onNodeWithTag("circuit-insert-$main")
        assertEquals(t("a11y_insert_circuit", "name" to "Hierarchy", "target" to "Hierarchy"), self.description())
        assertFalse(self.enabled(), "the circuit in view cannot be inserted into itself")
        assertEquals(t("circuit_insert_self", "name" to "Hierarchy"), self.state())

        val insert = onNodeWithTag("circuit-insert-$inversor")
        assertEquals(t("a11y_insert_circuit", "name" to "Inversor", "target" to "Hierarchy"), insert.description())
        assertTrue(insert.enabled())
        assertNull(insert.state())
        assertTrue(insert.tall(), "the Insert button is at least 48 dp tall")
        insert.performClick()
        waitUntil(timeoutMillis = 10_000) { !exists("editor-library-sheet") }
        waitForIdle()
        assertEquals(EditorMode.PLACE, state.mode)
        assertEquals("Inversor", state.placingTemplate?.circuitName)
        assertEquals(target, state.activeTarget, "inserting keeps Hierarchy in view")

        tapCanvas(state, 300f, 260f)
        screenshot("circuit-insert-2-placed")
        val placed = state.inversors().single { it.id != existing.id }
        assertEquals(existing.explicitPorts, placed.explicitPorts)
        assertEquals(existing.explicitBounds, placed.explicitBounds)
        assertEquals(2, EditorState(state.exportXml()).inversors().size, "the new instance survives save and reopen")
        state.undo()
        assertEquals(listOf(existing.id), state.inversors().map { it.id })
        state.redo()
        assertEquals(placed, state.inversors().single { it.id != existing.id })
    }

    @Test
    fun phoneCircuitMenuOffersInsertAndExplainsWhyItIsDisabled() = runDesktopComposeUiTest(390, 844) {
        val state = EditorState(hierarchy)
        app(state)
        val inversor = state.project.circuit("Inversor")!!.id
        val main = state.project.circuit("Hierarchy")!!.id
        openCircuitsTab()
        onNodeWithContentDescription(t(Messages.A11yManageCircuit, "name" to "Inversor")).performClick()
        waitForIdle()
        val item = onNodeWithTag("circuit-insert-menu-$inversor")
        assertTrue(item.enabled())
        assertTrue(onAllNodes(hasText(t("circuit_insert_into", "target" to "Hierarchy"))).fetchSemanticsNodes().isNotEmpty())
        item.performClick()
        waitUntil(timeoutMillis = 10_000) { !exists("editor-library-sheet") }
        assertEquals(EditorMode.PLACE, state.mode)
        assertEquals("Inversor", state.placingTemplate?.circuitName)
        tapCanvas(state, 300f, 260f)
        assertEquals(2, state.inversors().size)

        openCircuitsTab()
        onNodeWithContentDescription(t(Messages.A11yManageCircuit, "name" to "Hierarchy")).performClick()
        waitForIdle()
        screenshot("circuit-insert-3-menu-self")
        assertFalse(onNodeWithTag("circuit-insert-menu-$main").enabled())
        assertTrue(onAllNodes(hasText(t("circuit_insert_self", "name" to "Hierarchy"))).fetchSemanticsNodes().isNotEmpty())
    }
}
