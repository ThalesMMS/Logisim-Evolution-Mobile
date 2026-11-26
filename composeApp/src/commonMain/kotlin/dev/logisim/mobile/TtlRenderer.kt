package dev.logisim.mobile

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import dev.logisim.mobile.core.CircuitComponent
import dev.logisim.mobile.core.LogicValue
import dev.logisim.mobile.core.TtlState
import dev.logisim.mobile.core.ValueCharacters
import dev.logisim.mobile.core.display
import dev.logisim.mobile.core.TtlCatalog
import dev.logisim.mobile.core.TtlInternalStructure
import dev.logisim.mobile.core.VisualBounds
import dev.logisim.mobile.core.VisualItem
import dev.logisim.mobile.core.VisualPaint
import dev.logisim.mobile.core.VisualPrimitive
import dev.logisim.mobile.core.VisualScene

/** Evolution's component colour, which the recorded internal structure uses for every stroke and label. */
private const val RecordedInk = 0xff000000u

/** Draws the physical TTL package; logical ports come from the same catalog used by simulation. */
internal fun DrawScope.drawTtlPackage(
    component: CircuitComponent,
    measurer: TextMeasurer,
    outline: Color = Color.Black,
    gateShape: EditorGateShape = EditorGateShape.ANSI,
    /** The simulation state of the component, or null in the printer view and before the first propagation. */
    state: TtlState? = null,
    /** Value.getColor() for the register dots and the active 74123 pulse width. */
    valueColor: (LogicValue) -> Color = ::defaultTtlValueColor,
    valueCharacters: ValueCharacters = ValueCharacters.Default,
) {
    val model=component.kind.name.removePrefix("TTL_")
    val spec=TtlCatalog.spec(model)
    val width=spec.pinCount*10f
    val height=spec.height.toFloat()
    val internal=component.attributes["ShowInternalStructure"]=="true"
    val angle=when(component.facing){"west"->180f;"north"->270f;"south"->90f;else->0f}
    val structure=if(internal)ttlInternalStructure(model,gateShape,component.attributes,outline,state,valueColor,valueCharacters) else null
    if(structure!=null) {
        // AbstractTtlGate.paintInternalBase, recorded facing east; the package rotates as a whole, like Evolution's paintBase.
        withTransform({translate(component.position.x.toFloat(),component.position.y.toFloat());rotate(angle,Offset.Zero)}) {
            drawVisualScene(VisualScene(listOf(VisualItem("ttl", VisualBounds(0.0,-30.0,width.toDouble(),height-30.0),structure))),measurer)
        }
        return
    }
    val ink=if(internal)outline else Color.White
    val fill=if(internal)Color.White else Color(0xFF353535)
    withTransform({translate(component.position.x.toFloat(),component.position.y.toFloat());rotate(angle,Offset.Zero)}) {
        drawRoundRect(fill,Offset(0f,-23f),Size(width,height-14f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(4f,4f))
        drawRoundRect(outline,Offset(0f,-23f),Size(width,height-14f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(4f,4f),style=Stroke(1.5f))
        drawCircle(if(internal)Color.White else outline,4f,Offset(2f,height/2-30f))
        drawCircle(outline,4f,Offset(2f,height/2-30f),style=Stroke(1f))
        val title=measurer.measure(AnnotatedString(model),TextStyle(color=ink,fontSize=13.sp,fontFamily=FontFamily.Monospace))
        drawCircuitText(title,topLeft=Offset((width-title.size.width)/2,(height-60-title.size.height)/2))
        for(pin in 1..spec.pinCount) {
            val low=pin<=spec.pinCount/2
            val x=if(low)(pin-1)*20f+10f else (spec.pinCount-pin)*20f+10f
            val y=if(low)height-30f else -30f
            val visible=pin !in spec.unusedPins && ((pin!=spec.gndPin&&pin!=spec.vccPin)||component.attributes["VccGndPorts"]=="true")
            val inner=if(low)y-7 else y+7
            drawLine(if(visible)outline else Color.Gray,Offset(x,y),Offset(x,inner),2f)
            val text=if(internal)spec.pinNames[pin]?.substringBefore(' ')?:pin.toString() else pin.toString()
            val label=measurer.measure(AnnotatedString(text),TextStyle(color=ink,fontSize=if(internal)6.sp else 5.sp,fontFamily=FontFamily.Monospace))
            drawCircuitText(label,topLeft=Offset(x-label.size.width/2,if(low)inner-label.size.height-1 else inner+1))
        }
    }
}

/**
 * The recorded Evolution drawing for [model] in the editor's gate shape, with the component colour replaced by
 * [ink] so the diagram follows the light and dark themes. IEC uses Evolution's rectangular gates; ANSI and DIN 40700
 * use its shaped gates, as Drawgates does for every value other than "rectangular".
 */
internal fun ttlInternalStructure(
    model: String,
    gateShape: EditorGateShape,
    attributes: Map<String, String>,
    ink: Color,
    state: TtlState? = null,
    valueColor: (LogicValue) -> Color = ::defaultTtlValueColor,
    valueCharacters: ValueCharacters = ValueCharacters.Default,
): List<VisualPrimitive>? {
    val recorded = TtlInternalStructure.primitives(model, gateShape == EditorGateShape.IEC, attributes) ?: return null
    val argb = ink.toArgb().toUInt()
    var themed = if (argb == RecordedInk) recorded else recorded.map { it.withInk(argb) }
    if (model == "74123") {
        // Ttl74123.drawTiming: the pulse width of an active section takes Value.TRUE's colour.
        val widths = themed.filterIsInstance<VisualPrimitive.Text>().filter { it.font.size == 9.0 }
        val active: Set<VisualPrimitive> = (1..2).filter { TtlInternalStructure.pulseActive(state, it) }.mapNotNull { widths.getOrNull(it - 1) }.toSet()
        if (active.isNotEmpty()) {
            val high = valueColor(LogicValue.HIGH).toArgb().toUInt()
            themed = themed.map { primitive -> if (primitive in active && primitive is VisualPrimitive.Text) primitive.copy(color = high) else primitive }
        }
    }
    val values = TtlInternalStructure.stateValues(model, state) ?: return themed
    val dots = TtlInternalStructure.stateDots(model).zip(values).flatMap { (dot, value) ->
        // drawState: Value.getColor() disc, then toDisplayString() in white.
        listOfNotNull(VisualPrimitive.Ellipse(dot.bounds, VisualPaint(fill = valueColor(value).toArgb().toUInt(), stroke = null, strokeWidth = 0.0)),
            dot.label(value.display(valueCharacters), 0xffffffffu))
    }
    return themed + dots
}

/** Evolution's default TRUE/FALSE/UNKNOWN/ERROR_COLOR, used when no editor palette is supplied. */
internal fun defaultTtlValueColor(value: LogicValue): Color = when (value) {
    LogicValue.HIGH -> Color(0xFF00D200)
    LogicValue.LOW -> Color(0xFF006400)
    LogicValue.UNKNOWN -> Color(0xFF2828FF)
    LogicValue.ERROR -> Color(0xFFC00000)
}

private fun VisualPaint.withInk(ink: UInt) = copy(
    fill = fill?.let { if (it == RecordedInk) ink else it },
    stroke = stroke?.let { if (it == RecordedInk) ink else it },
)

private fun VisualPrimitive.withInk(ink: UInt): VisualPrimitive = when (this) {
    is VisualPrimitive.Path -> copy(paint = paint.withInk(ink))
    is VisualPrimitive.Rectangle -> copy(paint = paint.withInk(ink))
    is VisualPrimitive.Ellipse -> copy(paint = paint.withInk(ink))
    is VisualPrimitive.Text -> if (color == RecordedInk) copy(color = ink) else this
    is VisualPrimitive.Group -> copy(children = children.map { it.withInk(ink) })
    is VisualPrimitive.Image -> this
}
