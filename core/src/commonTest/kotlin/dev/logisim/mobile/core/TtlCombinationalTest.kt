package dev.logisim.mobile.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TtlCombinationalTest {
    @Test fun metadataHasEveryActiveDesktopModelAndRealPowerPins() {
        assertEquals(85, TtlCatalog.specs.size)
        assertEquals(85, TtlCatalog.specs.map { it.model }.toSet().size)
        assertEquals(5, TtlCatalog.spec("7493").vccPin)
        assertEquals(10, TtlCatalog.spec("7493").gndPin)
        TtlCatalog.specs.forEach { spec ->
            assertTrue(spec.drivenPins.all { it in spec.signalPins }, spec.model)
            assertTrue(spec.gndPin !in spec.signalPins && spec.vccPin !in spec.signalPins)
        }
    }
    @Test fun allCombinationalModelsEvaluateEveryDeclaredOutput() {
        TtlCombinational.supportedModels.forEach { model ->
            val spec=TtlCatalog.spec(model)
            val result=assertNotNull(TtlCombinational.evaluate(model,spec.signalPins.associateWith { LogicValue.LOW }),model)
            assertEquals(spec.drivenPins,result.outputs.keys,model)
        }
    }
    @Test fun quadXorFollowsPhysicalPinsAndUnknowns() {
        val pins=mapOf(1 to LogicValue.HIGH,2 to LogicValue.LOW,4 to LogicValue.HIGH,5 to LogicValue.HIGH,9 to LogicValue.LOW,10 to LogicValue.UNKNOWN,12 to LogicValue.ERROR,13 to LogicValue.HIGH)
        assertEquals(mapOf(3 to LogicValue.HIGH,6 to LogicValue.LOW,8 to LogicValue.ERROR,11 to LogicValue.ERROR), TtlLogic.evaluate("7486",pins).outputs)
    }
    /** Evolution c2a06deb (PR 3209): 2Y is pin 4 from pins 5 and 6, 3Y is pin 10 from pins 8 and 9. */
    @Test fun quadOpenCollectorXnor74266FollowsTheDatasheetPinout() {
        val h = LogicValue.HIGH; val l = LogicValue.LOW; val u = LogicValue.UNKNOWN
        val pins = mapOf(1 to h, 2 to h, 5 to h, 6 to l, 8 to l, 9 to l, 12 to l, 13 to h)
        assertEquals(mapOf(3 to u, 4 to l, 10 to u, 11 to l), TtlLogic.evaluate("74266", pins).outputs)
        assertEquals(setOf(3, 4, 10, 11), TtlCatalog.spec("74266").drivenPins)
        assertEquals(5 to 13, TtlCatalog.spec("7476").let { it.vccPin to it.gndPin })
    }
    @Test fun fullAdderProducesFiveBitCarryAcrossPhysicalPins() {
        val pins=TtlCatalog.spec("74283").signalPins.associateWith { LogicValue.HIGH }
        val result=TtlLogic.evaluate("74283",pins).outputs
        assertEquals(mapOf(1 to LogicValue.HIGH,4 to LogicValue.HIGH,9 to LogicValue.HIGH,10 to LogicValue.HIGH,13 to LogicValue.HIGH),result)
    }
    @Test fun explicitPowerFailureReleasesOutputs() {
        val pins=mapOf(1 to LogicValue.HIGH,2 to LogicValue.HIGH,7 to LogicValue.LOW,14 to LogicValue.LOW)
        assertEquals(setOf(LogicValue.UNKNOWN),TtlLogic.evaluate("7400",pins,attributes=mapOf("VccGndPorts" to "true")).outputs.values.toSet())
    }
    @Test fun openCollectorAndBufferPreserveTheirDistinctUnknownPolicies() {
        assertEquals(LogicValue.UNKNOWN,TtlLogic.evaluate("7438",mapOf(1 to LogicValue.UNKNOWN,2 to LogicValue.HIGH)).outputs[3])
        assertEquals(LogicValue.ERROR,TtlLogic.evaluate("7405",mapOf(1 to LogicValue.UNKNOWN)).outputs[2])
        assertEquals(LogicValue.HIGH,TtlLogic.evaluate("74125",mapOf(1 to LogicValue.UNKNOWN,2 to LogicValue.HIGH)).outputs[3])
    }
}
