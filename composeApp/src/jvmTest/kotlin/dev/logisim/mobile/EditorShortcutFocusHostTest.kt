package dev.logisim.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * #379: after a field inside the editor let go of focus (Search tab, then a result that opened a dialog), nothing in the
 * window held focus and the hardware-keyboard shortcuts stopped until the canvas was tapped.
 */
@OptIn(ExperimentalTestApi::class)
class EditorShortcutFocusHostTest {
    @Test
    fun shortcutsReturnOnceAFocusedFieldLetsGo() = runDesktopComposeUiTest(400, 300) {
        val fired = mutableListOf<EditorShortcut>()
        lateinit var focusManager: FocusManager
        setContent {
            focusManager = LocalFocusManager.current
            var text by remember { mutableStateOf("") }
            Box(Modifier.fillMaxSize().editorKeyboardShortcuts(EditorShortcutScheme.CONTROL, emptyMap(), enabled = { true }) {
                fired += it
                true
            }) {
                Column {
                    BasicTextField(text, { text = it }, Modifier.size(200.dp, 40.dp).testTag("search"))
                }
            }
        }
        waitForIdle()
        onRoot().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Two) } }
        waitForIdle()
        assertEquals(listOf(EditorShortcut.TOOL_SELECT_2), fired, "the editor takes focus when it appears")

        onNodeWithTag("search").performSemanticsAction(SemanticsActions.RequestFocus)
        waitForIdle()
        onNodeWithTag("search").assertIsFocused()
        runOnIdle { focusManager.clearFocus() }
        waitForIdle()

        onRoot().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Three) } }
        waitForIdle()
        assertEquals(listOf(EditorShortcut.TOOL_SELECT_2, EditorShortcut.TOOL_SELECT_3), fired,
            "shortcuts work again once the field has let go of focus")
    }
}
