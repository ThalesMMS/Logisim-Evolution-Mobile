package dev.logisim.mobile.core

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

/** Mirrors Evolution's ExponentiatorTest from a3f8856c against BigInteger.modPow. */
class ArithmeticLogicPowerTest {
    private fun power(width: Int, base: Long, exponent: Long, unsigned: Boolean) =
        ArithmeticLogic.evaluate(ArithmeticOperation.EXPONENTIATOR, width,
            listOf(LogicVector.of(width, base.toULong()), LogicVector.of(width, exponent.toULong())),
            if (unsigned) "unsigned" else "twosComplement")

    private fun check(width: Int, base: Long, exponent: Long, unsigned: Boolean) {
        val mask = BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE)
        val full = BigInteger.valueOf(base).and(mask)
            .modPow(BigInteger.valueOf(exponent).and(mask), BigInteger.ONE.shiftLeft(2 * width))
        val result = power(width, base, exponent, unsigned)
        val label = "width=$width base=$base exponent=$exponent unsigned=$unsigned"
        assertEquals(full.and(mask).toLong().toULong(), result[0].value, label)
        assertEquals(full.shiftRight(width).and(mask).toLong().toULong(), result[1].value, label)
    }

    @Test fun largeUnsignedExponent() {
        // The exponent used to be cast to int, so these gave 1.
        check(32, 3, 0x80000000L, true)
        check(32, 3, 0x7fffffffL, true)
        check(64, 5, -1L, true)
        check(40, 7, (1L shl 39) + 1, true)
        check(64, 3, 1L shl 31, true)
    }

    @Test fun smallExponent() {
        check(32, 3, 13, true)
        check(48, 3, 13, false)
        check(64, 12345, 7, true)
    }

    @Test fun negativeExponentIsZero() {
        for (width in listOf(8, 32, 40, 64)) {
            val result = power(width, 3, -1L, unsigned = false)
            assertEquals(listOf(LogicVector.low(width), LogicVector.low(width)), result, "width=$width")
        }
    }
}
