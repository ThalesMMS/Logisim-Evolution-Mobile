package dev.logisim.mobile.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** #374, #382: the Evolution internal structure recorded for every TTL model, from 21f1e158 since #382. */
class TtlInternalStructureTest {
    @Test fun everyCatalogModelHasARecordedDrawingInBothGateShapes() {
        assertEquals(TtlCatalog.specs.map { it.model }.toSet(), TtlInternalStructure.models)
        for (spec in TtlCatalog.specs) for (rectangular in listOf(false, true)) {
            val primitives = assertNotNull(TtlInternalStructure.primitives(spec.model, rectangular), spec.model)
            // Pin outlines and package, then at least one gate, wire or signal name.
            assertTrue(primitives.size > spec.pinCount + 2, "${spec.model}: ${primitives.size} primitives")
            assertTrue(primitives.any { it is VisualPrimitive.Path }, "${spec.model} draws no wiring")
        }
        assertNull(TtlInternalStructure.primitives("7483"))
    }

    @Test fun drawingsStayInsideThePackageAndOutlineEveryPinWhereThePortsAre() {
        for (spec in TtlCatalog.specs) {
            val primitives = TtlInternalStructure.primitives(spec.model)!!
            val package_ = VisualBounds(0.0, -30.0, spec.pinCount * 10.0, spec.height - 30.0)
            for (bounds in primitives.mapNotNull(::geometryBounds)) {
                assertTrue(bounds.left >= package_.left - 0.5 && bounds.right <= package_.right + 0.5 &&
                    bounds.top >= package_.top - 0.5 && bounds.bottom <= package_.bottom + 0.5, "${spec.model}: $bounds outside $package_")
            }
            val rectangles = primitives.filterIsInstance<VisualPrimitive.Rectangle>().map { it.bounds }
            for (pin in TtlGeometry.pins(spec.model, GridPoint(0, 0), powerPins = true)) {
                // AbstractTtlGate.paintBase: a PIN_WIDTH x PIN_HEIGHT (10 x 7) outline ending at the port.
                val top = if (pin.position.y < 0) pin.position.y.toDouble() else pin.position.y - 7.0
                val expected = VisualBounds(pin.position.x - 5.0, top, pin.position.x + 5.0, top + 7)
                assertTrue(expected in rectangles, "${spec.model} pin ${pin.number}: no outline at $expected")
            }
        }
    }

    @Test fun gateChipsFollowTheGateShapeAndSignalNamesKeepTheirText() {
        fun texts(model: String, rectangular: Boolean) =
            TtlInternalStructure.primitives(model, rectangular)!!.filterIsInstance<VisualPrimitive.Text>().map { it.text }
        assertEquals(listOf("Vcc", "GND"), texts("7400", false))
        assertEquals(4, texts("7400", true).count { it == "&" })
        assertEquals(4, texts("7432", true).count { it == "≥1" })
        assertTrue(TtlInternalStructure.hasRectangularVariant("7400"))
        assertTrue(!TtlInternalStructure.hasRectangularVariant("74161"))
        assertEquals(texts("74161", false), texts("74161", true))
        // Evolution 21f1e158 (PR 3220) draws the 74161 as a COUNTER block with its carry gate.
        assertTrue(texts("74161", false).containsAll(listOf("COUNTER", "&")))
        // 7474: two D flip-flops with their D, Q and clock wiring.
        assertEquals(2, texts("7474", false).count { it == "D" })
    }

    @Test fun painterInventoryMatchesTheDesktopClasses() {
        assertEquals("Ttl7400", TtlInternalStructure.painter("7400"))
        assertEquals("Ttl7400", TtlInternalStructure.painter("74132"))
        assertEquals("Ttl7410", TtlInternalStructure.painter("7411"))
        assertEquals("AbstractOctalFlops", TtlInternalStructure.painter("74273"))
        assertEquals("AbstractOctalBuffers", TtlInternalStructure.painter("74240"))
        val declaring = TtlCatalog.specs.mapNotNull { TtlInternalStructure.painter(it.model) }.toSet()
        assertEquals(68, declaring.size, declaring.sorted().toString())
    }

    @Test fun the74123ShowsThePulseWidthOfItsTimingAttributes() {
        fun labels(attributes: Map<String, String>) = TtlInternalStructure.primitives("74123", attributes = attributes)!!
            .filterIsInstance<VisualPrimitive.Text>().map { it.text }.filter { it.endsWith("s") && it.first().isDigit() }
        assertEquals(listOf("450 µs", "450 µs"), labels(emptyMap()))
        assertEquals(listOf("9 µs", "202.5 ms"), labels(mapOf("1Rext" to "2", "1Cext" to "10000", "2Rext" to "1000", "2Cext" to "450000")))
        assertEquals("1 ns", TtlInternalStructure.formatPulseWidth(TtlInternalStructure.pulseWidthNanos(0, 0)))
        assertEquals("3 s", TtlInternalStructure.formatPulseWidth(3_000_000_000))
        assertEquals("1.235 ms", TtlInternalStructure.formatPulseWidth(1_234_567))
    }

    private fun geometryBounds(primitive: VisualPrimitive): VisualBounds? = when (primitive) {
        is VisualPrimitive.Rectangle -> primitive.bounds
        is VisualPrimitive.Ellipse -> primitive.bounds
        is VisualPrimitive.Path -> primitive.commands.flatMap {
            when (it) {
                is VisualPathCommand.MoveTo -> listOf(it.point)
                is VisualPathCommand.LineTo -> listOf(it.point)
                is VisualPathCommand.QuadTo -> listOf(it.end)
                is VisualPathCommand.CubicTo -> listOf(it.end)
                VisualPathCommand.Close -> emptyList()
            }
        }.let { points -> VisualBounds(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y }) }
        is VisualPrimitive.Text -> VisualBounds(primitive.position.x, primitive.position.y, primitive.position.x, primitive.position.y)
        else -> null
    }
}
