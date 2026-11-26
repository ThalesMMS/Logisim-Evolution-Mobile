package dev.logisim.mobile.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * #374: replays the sequential fixtures and checks, after every row, the register value dots and the 74123 timing
 * labels against what Evolution 21f1e158 paints (resources/ttl-internal-state.tsv, from
 * TtlInternalStructureOracle --state).
 */
class TtlInternalStateTest {
    private val resources = listOf(File("src/jvmTest/resources"), File("core/src/jvmTest/resources")).first { it.isDirectory }

    @Test fun dotsShowTheSameRegisterValuesAsEvolutionAfterEveryFixtureRow() {
        val expected = File(resources, "ttl-internal-state.tsv").readLines().drop(1).map { it.split('\t') }.groupBy { it[0] }
        assertEquals(18, expected.size)
        // Since Evolution 21f1e158 the redrawn 7476, 7493, 74161/74163 and 74390 paint no value dots: their rows stay empty.
        val withDots = expected.filterValues { rows -> rows.any { it[2].isNotEmpty() } }.keys
        assertEquals(setOf("7474", "74107", "74164", "74165", "74166", "74192", "74193", "74259", "74273", "74377", "74393", "744060"), withDots)
        assertEquals(withDots, TtlInternalStructure.models.filter { TtlInternalStructure.stateDots(it).isNotEmpty() }.toSet())
        for ((model, rows) in expected) {
            val fixture = File(resources, "ttl-fixtures/$model.tsv").readLines().drop(1)
            assertEquals(fixture.size, rows.size, model)
            var state = TtlState()
            for ((row, line) in fixture.withIndex()) {
                val columns = line.split('\t')
                val pins = TtlCatalog.spec(model).signalPins.mapIndexed { index, pin -> pin to decode(columns[1][index]) }.toMap()
                state = TtlLogic.evaluate(model, pins, state, elapsedTimeMillis = row.toLong()).state
                val dots = TtlInternalStructure.stateValues(model, state)?.joinToString("") { it.display(ValueCharacters.Default) } ?: ""
                assertEquals(rows[row][2], dots, "$model row $row dots")
                if (model == "74123") {
                    assertEquals(rows[row][3], (1..2).joinToString("") { if (TtlInternalStructure.pulseActive(state, it)) "1" else "0" }, "74123 row $row")
                }
            }
        }
    }

    @Test fun dotsBeforeTheFirstPropagationFollowEvolution() {
        // Ttl74164/5/6.paintInternal create their ShiftRegisterData, so a new chip shows eight LOW dots.
        for (model in listOf("74164", "74165", "74166")) assertEquals(List(8) { LogicValue.LOW }, TtlInternalStructure.stateValues(model, null))
        assertNull(TtlInternalStructure.stateValues("7474", null))
        assertNull(TtlInternalStructure.stateValues("7400", TtlState()))
        val octal = TtlInternalStructure.stateDots("74273")
        assertEquals(8, octal.size)
        assertTrue(octal.all { it.label is VisualPrimitive.Group }, "74273 labels turn with the octal register")
        assertEquals(8, TtlInternalStructure.stateDots("74393").size)
        assertTrue(TtlInternalStructure.stateDots("74393").all { it.label == null }, "74393 drawBit paints no digit")
    }

    private fun decode(value: Char): LogicValue = when (value) { '0' -> LogicValue.LOW; '1' -> LogicValue.HIGH; 'E' -> LogicValue.ERROR; else -> LogicValue.UNKNOWN }
}
