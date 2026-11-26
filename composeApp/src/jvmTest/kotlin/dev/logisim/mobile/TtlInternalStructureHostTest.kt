package dev.logisim.mobile

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.logisim.mobile.core.CircuitComponent
import dev.logisim.mobile.core.ComponentKind
import dev.logisim.mobile.core.GridPoint
import dev.logisim.mobile.core.LogicValue
import dev.logisim.mobile.core.TtlLogic
import dev.logisim.mobile.core.TtlState
import dev.logisim.mobile.core.SimulationState
import dev.logisim.mobile.core.TtlCatalog
import dev.logisim.mobile.core.TtlInternalStructure
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * #374: draws every TTL model with ShowInternalStructure through the canvas path (drawClassicComponent) on the JVM
 * host, which uses Skia as iOS does. The PNGs, at scale 4 with a 4-unit margin like TtlInternalStructureOracle,
 * go to composeApp/build/ttl-internal-structure (or -Pttl.internal.out) for TtlInternalStructureCompare.
 */
@OptIn(InternalComposeUiApi::class)
class TtlInternalStructureHostTest {
    init {
        registerSkikoComposeImplementation()
    }

    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "fixtures/visual-export").isDirectory }
    private val output = File(System.getProperty("ttl.internal.out") ?: File(root, "composeApp/build/ttl-internal-structure").path)
    private val measurer = TextMeasurer(createFontFamilyResolver(), Density(1f), LayoutDirection.Ltr)
    private val scale = 4f
    private val margin = 4f

    private fun render(model: String, style: CircuitDrawingStyle, facing: String = "east", internal: Boolean = true, state: TtlState? = null): ImageBitmap {
        val spec = TtlCatalog.spec(model)
        val attributes = buildMap {
            put("facing", facing)
            if (internal) put("ShowInternalStructure", "true")
        }
        val component = CircuitComponent("ttl", ComponentKind("TTL_$model"), GridPoint(0, 0), attributes = attributes)
        val width = spec.pinCount * 10f
        val height = spec.height.toFloat()
        val horizontal = facing == "east" || facing == "west"
        val (w, h) = if (horizontal) width to height else height to width
        val bitmap = ImageBitmap(((w + 2 * margin) * scale).toInt(), ((h + 2 * margin) * scale).toInt(), ImageBitmapConfig.Argb8888)
        // The component's offset bounds: (0,-30) facing east, rotated about the location for other facings.
        val origin = when (facing) {
            "west" -> Offset(-width, -(height - 30f))
            "north" -> Offset(-30f, -width)
            "south" -> Offset(-(height - 30f), 0f)
            else -> Offset(0f, -30f)
        }
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(bitmap.width.toFloat(), bitmap.height.toFloat())) {
            drawRect(style.paper, blendMode = BlendMode.Src)
            withTransform({
                scale(scale, scale, Offset.Zero)
                translate(margin - origin.x, margin - origin.y)
            }) {
                if (state == null) drawClassicComponent(component, SimulationState(), false, measurer, style = style)
                else drawTtlPackage(component, measurer, state = state)
            }
        }
        return bitmap
    }

    private fun ImageBitmap.pixels() = IntArray(width * height).also { readPixels(it) }

    private fun save(bitmap: ImageBitmap, name: String) {
        val data = checkNotNull(Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG))
        File(output, "$name.png").apply { parentFile.mkdirs() }.writeBytes(data.bytes)
    }

    @Test fun everyModelDrawsItsInternalStructureInLightAndDarkThemes() {
        val light = CircuitDrawingStyle()
        val dark = CircuitDrawingStyle(colors = DarkEditorColors, darkTheme = true)
        val darkInk = dark.color(EditorColor.COMPONENT, Color.Black).toArgb()
        for (spec in TtlCatalog.specs) {
            val shapes = listOf(EditorGateShape.ANSI) + if (TtlInternalStructure.hasRectangularVariant(spec.model)) listOf(EditorGateShape.IEC) else emptyList()
            for (shape in shapes) {
                val bitmap = render(spec.model, light.copy(gateShape = shape))
                val suffix = if (shape == EditorGateShape.IEC) "-rectangular" else ""
                save(bitmap, (if (shape == EditorGateShape.IEC) "rectangular/" else "shaped/") + spec.model)
                val ink = bitmap.pixels().count { luminance(it) < 96 }
                // The package outline alone covers about 2 x (width + height) x scale pixels; the diagram adds to it.
                val outlineOnly = 2 * (spec.pinCount * 10 + spec.height) * scale
                assertTrue(ink > outlineOnly, "${spec.model}$suffix: $ink ink pixels")
                val plain = render(spec.model, light.copy(gateShape = shape), internal = false)
                assertTrue(!bitmap.pixels().contentEquals(plain.pixels()), "${spec.model}$suffix matches the package view")
            }
            val night = render(spec.model, dark)
            save(night, "dark/${spec.model}")
            val pixels = night.pixels()
            assertTrue(pixels.count { it == darkInk } > 100, "${spec.model}: dark theme ink missing")
            assertTrue(pixels.none { it == 0xff000000.toInt() }, "${spec.model}: black strokes on the dark canvas")
        }
    }

    @Test fun rotatedPackagesKeepTheDiagramInsideTheirBounds() {
        val style = CircuitDrawingStyle()
        for (model in listOf("7400", "7474", "74181", "74273")) for (facing in listOf("east", "west", "north", "south")) {
            val bitmap = render(model, style, facing)
            save(bitmap, "facing/$model-$facing")
            val pixels = bitmap.pixels()
            val border = (margin * scale).toInt() - 2
            var outside = 0
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                val inside = x in border until bitmap.width - border && y in border until bitmap.height - border
                if (!inside && luminance(pixels[y * bitmap.width + x]) < 128) outside++
            }
            assertTrue(outside == 0, "$model $facing: $outside ink pixels outside the bounds")
            assertTrue(pixels.count { luminance(it) < 96 } > 2000, "$model $facing draws nothing")
        }
    }

    @Test fun registerDotsShowTheStateAfterTheEvolutionSnapshotRow() {
        // TtlInternalStructureOracle --state draws each model after fixture row 100 (SNAPSHOT_ROW).
        val models = listOf("7474", "7476", "7493", "74107", "74123", "74161", "74163", "74164", "74165", "74166",
            "74192", "74193", "74259", "74273", "74377", "74390", "74393", "744060")
        for (model in models) {
            var state = TtlState()
            val fixture = File(root, "core/src/jvmTest/resources/ttl-fixtures/$model.tsv").readLines().drop(1)
            for ((row, line) in fixture.take(101).withIndex()) {
                val columns = line.split('\t')
                val pins = TtlCatalog.spec(model).signalPins.mapIndexed { index, pin -> pin to decode(columns[1][index]) }.toMap()
                state = TtlLogic.evaluate(model, pins, state, elapsedTimeMillis = row.toLong()).state
            }
            val bitmap = render(model, CircuitDrawingStyle(), state = state)
            save(bitmap, "state/$model")
            val values = TtlInternalStructure.stateValues(model, state)
            if (values != null) {
                val pixels = bitmap.pixels()
                for (value in values.toSet()) {
                    val color = defaultTtlValueColor(value).toArgb()
                    assertTrue(pixels.any { it == color }, "$model: no $value dot")
                }
            }
        }
    }

    private fun decode(value: Char): LogicValue = when (value) { '0' -> LogicValue.LOW; '1' -> LogicValue.HIGH; 'E' -> LogicValue.ERROR; else -> LogicValue.UNKNOWN }

    private fun luminance(argb: Int): Int {
        val r = (argb shr 16) and 0xff
        val g = (argb shr 8) and 0xff
        val b = argb and 0xff
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
