package dev.logisim.mobile.core

import kotlin.math.roundToLong

/**
 * The logic diagram that Evolution's AbstractTtlGate.paintInternalBase draws when ShowInternalStructure is true:
 * pin outlines, package outline and notch, Vcc/GND labels, gates, wiring to the pins and signal names.
 *
 * Coordinates are relative to the component location with the package facing east, the frame of
 * [TtlGeometry.pins]; the renderer rotates them with the package. Colours are the recorded ones, where opaque black
 * is Evolution's component colour. The register value dots that some sequential models paint from their state, and
 * the gate-shape preference, are not part of the display list: callers choose the IEC drawing with `rectangular`.
 */
object TtlInternalStructure {
    /** The models that have a recorded drawing, which is every model in [TtlCatalog]. */
    val models: Set<String> get() = TtlInternalStructureData.shaped.keys

    /** Declaring class of paintInternal in the desktop implementation, for the inventory and tests. */
    fun painter(model: String): String? = TtlInternalStructureData.painters[model]

    /** Whether IEC (rectangular) gates change the drawing of [model]. */
    fun hasRectangularVariant(model: String): Boolean = model in TtlInternalStructureData.rectangular

    private val shapedCache by lazy { TtlInternalStructureData.shaped.mapValues { parse(it.key, it.value) } }
    private val rectangularCache by lazy { TtlInternalStructureData.rectangular.mapValues { parse(it.key, it.value) } }

    /**
     * The recorded primitives for [model], or null when the model has none. [attributes] supplies the 74123 timing
     * resistors and capacitors, whose pulse widths Evolution prints inside the package.
     */
    fun primitives(model: String, rectangular: Boolean = false, attributes: Map<String, String> = emptyMap()): List<VisualPrimitive>? {
        val recorded = (if (rectangular) rectangularCache[model] else null) ?: shapedCache[model] ?: return null
        if (model != "74123") return recorded
        val labels = (1..2).associate { section ->
            "{pulse$section}" to formatPulseWidth(pulseWidthNanos(
                attributes["${section}Rext"]?.toIntOrNull() ?: 10, attributes["${section}Cext"]?.toIntOrNull() ?: 100_000))
        }
        return recorded.map { primitive ->
            if (primitive is VisualPrimitive.Text && primitive.text in labels) primitive.copy(text = labels.getValue(primitive.text)) else primitive
        }
    }

    /**
     * A register value dot that drawState, structureDot or drawBit paints from the model state: a filled circle in
     * Value.getColor() and, when [label] is set, the value character in white on it.
     */
    data class StateDot(val bounds: VisualBounds, val label: VisualPrimitive?, val key: String, val bit: Int) {
        /** The dot's value label showing [text] in [color]: a text, or a rotated group holding it. */
        fun label(text: String, color: UInt): VisualPrimitive? = label?.let { relabel(it, text, color) }

        private fun relabel(primitive: VisualPrimitive, text: String, color: UInt): VisualPrimitive = when (primitive) {
            is VisualPrimitive.Text -> primitive.copy(text = text, color = color)
            is VisualPrimitive.Group -> primitive.copy(children = primitive.children.map { relabel(it, text, color) })
            else -> primitive
        }
    }

    /**
     * Which bit of [TtlState.data] each Evolution dot shows, in painting order. TtlInternalStateTest replays the
     * sequential fixtures and checks every dot against the Evolution values after each row.
     */
    private val stateBindings: Map<String, List<Pair<String, Int>>> = run {
        fun word(key: String, bits: IntProgression) = bits.map { key to it }
        val flops = listOf("q0" to 0, "q1" to 0)
        val nibble = word("q", 3 downTo 0)
        val byteHighFirst = word("q", 7 downTo 0)
        val byteLowFirst = word("q", 0..7)
        mapOf(
            // 7476, 7493, 74161/74163 and 74390 paint no value dots since Evolution 21f1e158 (#382).
            "7474" to flops, "74107" to flops,
            "74192" to nibble, "74193" to nibble,
            "74164" to byteHighFirst, "74165" to byteHighFirst, "74166" to byteHighFirst,
            "74259" to listOf("q" to 0),
            "74273" to byteLowFirst, "74377" to byteLowFirst,
            "74393" to word("q0", 0..3) + word("q1", 0..3),
            "744060" to word("q", 13 downTo 0),
        )
    }

    /** Models whose paintInternal creates its register data, so Evolution shows the dots before any propagation. */
    private val dotsWithoutState = setOf("74164", "74165", "74166")

    private val stateDotCache by lazy {
        TtlInternalStructureData.stateDots.mapValues { (model, source) ->
            val keys = stateBindings[model] ?: error("TTL $model: state dots without a binding")
            val dots = ArrayList<Pair<VisualBounds, VisualPrimitive?>>()
            for (line in source.lineSequence().filter { it.isNotBlank() }) {
                val f = line.substring(2).split(' ')
                when (line[0]) {
                    'd' -> {
                        val v = f.map(String::toDouble)
                        dots += VisualBounds(v[0], v[1], v[0] + v[2], v[1] + v[3]) to null
                    }
                    't' -> {
                        val text = VisualPrimitive.Text("0", VisualPoint(0.0, 0.0),
                            VisualFont(family = f[0], size = f[1].toDouble(), bold = 'b' in f[2], italic = 'i' in f[2]),
                            0xffffffffu, VisualTextAlign.CENTER, VisualTextVerticalAlign.BASELINE)
                        val x = f[3].toDouble()
                        val y = f[4].toDouble()
                        val rotation = f[5].toDouble()
                        // The octal flip-flops paint their dots a quarter turn left, like their "C1"/"1D" labels.
                        dots[dots.lastIndex] = dots.last().first to if (rotation == 0.0) text.copy(position = VisualPoint(x, y))
                        else VisualPrimitive.Group(VisualTransform(translateX = x, translateY = y, rotationDegrees = rotation), listOf(text))
                    }
                    else -> error("TTL $model: unknown state operation '${line[0]}'")
                }
            }
            require(dots.size == keys.size) { "TTL $model: ${dots.size} dots for ${keys.size} bindings" }
            dots.mapIndexed { i, (bounds, label) -> StateDot(bounds, label, keys[i].first, keys[i].second) }
        }
    }

    /** The value dots of [model], empty for models whose drawing has none. */
    fun stateDots(model: String): List<StateDot> = stateDotCache[model].orEmpty()

    /**
     * The values the dots show for [state], or null when Evolution draws no dots: the model has none, or it has no
     * register data yet and does not create it while painting. A bit absent from the state is LOW, the value of the
     * freshly created desktop register.
     */
    fun stateValues(model: String, state: TtlState?): List<LogicValue>? {
        val dots = stateDots(model).ifEmpty { return null }
        if (state == null && model !in dotsWithoutState) return null
        return dots.map { dot -> state?.data?.get(dot.key)?.takeIf { dot.bit < it.width }?.bit(dot.bit) ?: LogicValue.LOW }
    }

    /** Whether the 74123 monostable [section] (1 or 2) is in its pulse, when Ttl74123.drawTiming paints the width in Value.TRUE's colour. */
    fun pulseActive(state: TtlState?, section: Int): Boolean = state?.data?.get("mono${section - 1}")?.bit(0) == LogicValue.HIGH

    /** Ttl74123.widthNanos: t = 0.45 · Rext · Cext, with Rext in kΩ and Cext in pF. */
    fun pulseWidthNanos(rextKiloOhm: Int, cextPicofarad: Int): Long =
        (0.45e-9 * rextKiloOhm * cextPicofarad * 1_000_000_000.0).roundToLong().coerceAtLeast(1)

    /** Ttl74123.formatWidth, such as `450 µs` or `3 s`. */
    fun formatPulseWidth(nanos: Long): String = when {
        nanos >= 1_000_000_000L -> trim(nanos / 1_000_000_000.0) + " s"
        nanos >= 1_000_000L -> trim(nanos / 1_000_000.0) + " ms"
        nanos >= 1_000L -> trim(nanos / 1_000.0) + " µs"
        else -> "$nanos ns"
    }

    private fun trim(value: Double): String {
        val thousandths = (value * 1000).roundToLong()
        val whole = thousandths / 1000
        val fraction = (thousandths % 1000).toString().padStart(3, '0').trimEnd('0')
        return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
    }

    private fun parse(model: String, source: String): List<VisualPrimitive> {
        val result = ArrayList<VisualPrimitive>()
        var width = 1.0
        var color = 0xff000000u
        fun stroke() = VisualPaint(fill = null, stroke = color, strokeWidth = width)
        fun fill() = VisualPaint(fill = color, stroke = null, strokeWidth = 0.0)
        for (line in source.lineSequence()) {
            if (line.isBlank()) continue
            val op = line[0]
            val rest = line.substring(2)
            when (op) {
                'w' -> width = rest.toDouble()
                'c' -> color = rest.toUInt(16)
                'r', 'R' -> {
                    val v = rest.split(' ').map(String::toDouble)
                    result += VisualPrimitive.Rectangle(VisualBounds(v[0], v[1], v[0] + v[2], v[1] + v[3]),
                        if (op == 'R') fill() else stroke(), v.getOrElse(4) { 0.0 })
                }
                'o', 'O' -> {
                    val v = rest.split(' ').map(String::toDouble)
                    result += VisualPrimitive.Ellipse(VisualBounds(v[0], v[1], v[0] + v[2], v[1] + v[3]), if (op == 'O') fill() else stroke())
                }
                'l' -> {
                    val v = rest.split(' ').map(String::toDouble)
                    val commands = (v.indices step 2).map { i ->
                        val point = VisualPoint(v[i], v[i + 1])
                        if (i == 0) VisualPathCommand.MoveTo(point) else VisualPathCommand.LineTo(point)
                    }
                    result += VisualPrimitive.Path(commands, stroke())
                }
                'p', 'P' -> result += VisualPrimitive.Path(pathCommands(rest), if (op == 'P') fill() else stroke())
                't' -> {
                    val f = rest.split(' ', limit = 7)
                    val text = VisualPrimitive.Text(f[6], VisualPoint(0.0, 0.0),
                        VisualFont(family = f[0].replace('_', ' '), size = f[1].toDouble(), bold = 'b' in f[2], italic = 'i' in f[2]),
                        color, VisualTextAlign.CENTER, VisualTextVerticalAlign.BASELINE)
                    val x = f[3].toDouble()
                    val y = f[4].toDouble()
                    val rotation = f[5].toDouble()
                    result += if (rotation == 0.0) text.copy(position = VisualPoint(x, y))
                    else VisualPrimitive.Group(VisualTransform(translateX = x, translateY = y, rotationDegrees = rotation), listOf(text))
                }
                else -> error("TTL $model: unknown internal structure operation '$op'")
            }
        }
        return result
    }

    private fun pathCommands(source: String): List<VisualPathCommand> {
        val tokens = source.split(' ')
        val commands = ArrayList<VisualPathCommand>()
        var i = 0
        fun point(): VisualPoint = VisualPoint(tokens[i].toDouble(), tokens[i + 1].toDouble()).also { i += 2 }
        while (i < tokens.size) {
            when (tokens[i++]) {
                "M" -> commands += VisualPathCommand.MoveTo(point())
                "L" -> commands += VisualPathCommand.LineTo(point())
                "Q" -> { val c = point(); commands += VisualPathCommand.QuadTo(c, point()) }
                "C" -> { val c1 = point(); val c2 = point(); commands += VisualPathCommand.CubicTo(c1, c2, point()) }
                "Z" -> commands += VisualPathCommand.Close
                else -> error("Invalid TTL path token ${tokens[i - 1]}")
            }
        }
        return commands
    }
}
