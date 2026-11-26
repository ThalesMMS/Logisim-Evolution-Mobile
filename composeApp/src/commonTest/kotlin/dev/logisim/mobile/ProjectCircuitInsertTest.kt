package dev.logisim.mobile

import dev.logisim.mobile.core.ComponentKind
import dev.logisim.mobile.core.GridPoint
import dev.logisim.mobile.core.PortDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Inserting a project circuit from the Circuits tab (#368): Evolution's add tool for a definition of the project. */
class ProjectCircuitInsertTest {
    /** main holds one Half; Top holds Mid, which holds Leaf. */
    private val xml = """<project source="3.8.0" version="1.0"><lib name="0" desc="#Wiring"/>
        <main name="main"/>
        <circuit name="main"><comp name="Half" loc="(300,140)"/></circuit>
        <circuit name="Half">
          <comp lib="0" name="Pin" loc="(100,100)"><a name="label" val="A"/></comp>
          <comp lib="0" name="Pin" loc="(300,100)"><a name="label" val="Y"/><a name="type" val="output"/><a name="facing" val="west"/></comp>
          <wire from="(100,100)" to="(300,100)"/>
        </circuit>
        <circuit name="Top"><comp name="Mid" loc="(200,200)"/></circuit>
        <circuit name="Mid"><comp name="Leaf" loc="(200,200)"/></circuit>
        <circuit name="Leaf"><comp lib="0" name="Pin" loc="(100,100)"><a name="label" val="L"/></comp></circuit>
        </project>"""

    private fun EditorState.id(name: String) = project.circuit(name)!!.id
    private fun EditorState.instances(name: String) = document.components.filter { it.circuitName == name }

    @Test
    fun insertingAProjectCircuitPicksItsAddToolAndATapPlacesABoundInstance() {
        val state = EditorState(xml)
        val target = state.activeTarget
        val existing = state.instances("Half").single()
        assertNull(state.projectCircuitInsertProblem(state.id("Half")))
        assertTrue(state.placeProjectCircuit(state.id("Half")))
        assertEquals(EditorMode.PLACE, state.mode)
        assertEquals("Half", state.placingTemplate?.circuitName)
        assertEquals("status_place_circuit", state.statusText.key)
        // The view stays on the circuit it was inserting into.
        assertEquals(target, state.activeTarget)

        state.tap(GridPoint(300, 300))
        val placed = state.instances("Half").single { it.id != existing.id }
        assertEquals(ComponentKind.SUBCIRCUIT, placed.kind)
        assertEquals(GridPoint(300, 300), placed.position)
        // The same symbol and pins as the instance the file declared.
        assertEquals(existing.explicitPorts, placed.explicitPorts)
        assertEquals(existing.explicitBounds, placed.explicitBounds)
        assertEquals(setOf(PortDirection.INPUT, PortDirection.OUTPUT), placed.explicitPorts.map { it.direction }.toSet())

        state.undo()
        assertEquals(listOf(existing.id), state.instances("Half").map { it.id })
        state.redo()
        assertEquals(placed, state.instances("Half").single { it.id != existing.id })

        val reopened = EditorState(state.exportXml())
        val kept = reopened.instances("Half")
        assertEquals(2, kept.size)
        val reopenedPlaced = kept.single { it.position == GridPoint(300, 300) }
        assertEquals(placed.explicitPorts, reopenedPlaced.explicitPorts)
        assertEquals(placed.explicitBounds, reopenedPlaced.explicitBounds)
    }

    @Test
    fun aCircuitCreatedInTheAppCanBeInsertedIntoAnother() {
        val state = EditorState(xml)
        assertTrue(state.createCircuit("Chip"))
        assertEquals("Chip", state.document.name)
        state.place(ComponentKind.INPUT)
        state.tap(GridPoint(100, 100))
        state.selectCircuit("main")

        assertTrue(state.placeProjectCircuit(state.id("Chip")))
        state.tap(GridPoint(400, 400))
        val placed = state.instances("Chip").single()
        assertEquals(listOf(PortDirection.INPUT), placed.explicitPorts.map { it.direction })
        assertEquals(placed.explicitPorts, EditorState(state.exportXml()).instances("Chip").single().explicitPorts)
    }

    @Test
    fun selfInsertionAndIndirectCyclesAreRefusedWithTheirReason() {
        val state = EditorState(xml)
        assertEquals("circuit_insert_self", state.projectCircuitInsertProblem(state.id("main"))?.key)
        val before = state.exportXml()
        assertFalse(state.placeProjectCircuit(state.id("main")))
        assertNotEquals(EditorMode.PLACE, state.mode)
        assertEquals("circuit_insert_self", state.statusText.key)

        // Top reaches Leaf through Mid: inserting Top into Leaf would make Leaf contain itself.
        state.selectCircuit("Leaf")
        assertEquals("circuit_insert_recursive", state.projectCircuitInsertProblem(state.id("Top"))?.key)
        assertEquals("circuit_insert_recursive", state.projectCircuitInsertProblem(state.id("Mid"))?.key)
        assertFalse(state.placeProjectCircuit(state.id("Top")))
        assertNotEquals(EditorMode.PLACE, state.mode)
        assertEquals("circuit_insert_recursive", state.statusText.key)
        assertNull(state.projectCircuitInsertProblem(state.id("Half")))
        assertEquals(before, state.exportXml())

        // A cycle an edit of this session made is refused too: once Mid holds Half, Half may not take Top.
        state.selectCircuit("Half")
        assertNull(state.projectCircuitInsertProblem(state.id("Top")))
        state.selectCircuit("Mid")
        assertTrue(state.placeProjectCircuit(state.id("Half")))
        state.tap(GridPoint(600, 600))
        state.selectCircuit("Half")
        assertEquals("circuit_insert_recursive", state.projectCircuitInsertProblem(state.id("Top"))?.key)
        assertFalse(state.placeProjectCircuit(state.id("Top")))
    }

    @Test
    fun viewingAnInstanceInsertsIntoItsDefinitionWithoutNavigating() {
        val state = EditorState(xml)
        state.viewSubcircuitInstance(state.instances("Half").single().id)
        val target = state.activeTarget
        assertTrue(target.instancePath.isNotEmpty())
        assertEquals("Half", state.document.name)
        assertEquals("circuit_insert_self", state.projectCircuitInsertProblem(state.id("Half"))?.key)
        assertEquals("circuit_insert_recursive", state.projectCircuitInsertProblem(state.id("main"))?.key)

        assertTrue(state.placeProjectCircuit(state.id("Leaf")))
        assertEquals(target, state.activeTarget)
        state.tap(GridPoint(500, 500))
        assertEquals(target, state.activeTarget)
        assertEquals(1, state.project.circuit("Half")!!.components.count { it.circuitName == "Leaf" })
    }
}
