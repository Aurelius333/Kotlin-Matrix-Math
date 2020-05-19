package matrix

import complex.Complex
import complex.complex
import complex.conjugate
import complex.times
import utils.Size
import vector.Vector
import kotlin.math.absoluteValue

abstract class NumberMatrix<T>(dim: Size, initBlock: (r: Int, c: Int) -> T): Matrix<T>(dim = dim, initBlock = initBlock) where T: Number {
    constructor(x: Int, y: Int, initBlock: (Int) -> T): this(dim = Size(
        x,
        y
    ), initBlock = { _, _ -> initBlock(0)})

    constructor(vector: Vector<T>): this(dim = Size(1, vector.length), initBlock = { _, i -> vector[i]  })

    constructor(matListOfVector: Vector<Vector<T>>, unused: Int = 0): this(
        dim = Size(matListOfVector.length, matListOfVector[0].length),
        initBlock = { r, c -> matListOfVector[r][c] }
    )

    constructor(dim1: Size, emptyOrSingle: Boolean, initBlock: (Int) -> T): this(dim = dim1, initBlock = { _, i -> initBlock(i)})

    abstract operator fun plus(other: NumberMatrix<T>): NumberMatrix<T>

    abstract operator fun minus(other: NumberMatrix<T>): NumberMatrix<T>

    abstract operator fun times(other: NumberMatrix<T>): NumberMatrix<T>

    abstract operator fun div(other: NumberMatrix<T>): NumberMatrix<T>

    abstract operator fun rem(other: NumberMatrix<T>): NumberMatrix<T>

    abstract fun pow(other: NumberMatrix<T>): NumberMatrix<T>

    abstract operator fun plusAssign(other: NumberMatrix<T>)

    abstract operator fun minusAssign(other: NumberMatrix<T>)

    abstract operator fun timesAssign(other: NumberMatrix<T>)

    abstract operator fun divAssign(other: NumberMatrix<T>)

    abstract operator fun remAssign(other: NumberMatrix<T>)

    abstract fun powAssign(other: NumberMatrix<T>)

    abstract fun dot(other: NumberMatrix<T>): NumberMatrix<T>

    abstract fun cross(other: NumberMatrix<T>): NumberMatrix<T>

    abstract fun matMul(other: NumberMatrix<T>): NumberMatrix<T>

    abstract fun matDiv(other: NumberMatrix<T>): NumberMatrix<T>

    abstract fun trace(): T

    @Suppress("UNCHECKED_CAST")
    fun rank(): T {
        val a = this.toList().toMutableList()
        val lastCol = this.colLength - 1
        val lastRow = this.rowLength - 1
        var pivotRow = 0
        var prevPivot = 1
        for (k in 0..lastCol) {
            val switchRow = (pivotRow .. lastRow).find { a[it][k].toInt() != 0 }

            if (switchRow != null) {
                if (pivotRow != switchRow) {
                    val temp = a[switchRow]
                    a[switchRow] = a[pivotRow]
                    a[pivotRow] = temp
                }
                val pivot = a[pivotRow][k]
                for (i in (pivotRow+1)..lastRow) {
                    val ai = a[i].toMutableList()
                    for (j in (k+1)..lastCol) {
                        ai[j] = ((pivot.toDouble() * ai[j].toDouble() - ai[k].toDouble() * a[pivotRow][j].toDouble()) / prevPivot) as T
                    }
                }
                pivotRow += 1
                prevPivot = pivot.toInt()
            }
        }
        return pivotRow as T
    }


    abstract fun inverse(): NumberMatrix<T>

    abstract fun determinant(): Double

    abstract fun cofactor(row: Int, col: Int): T

    abstract fun firstMinor(row: Int, col: Int): NumberMatrix<T>

    abstract fun adjugate(): NumberMatrix<T>

    abstract fun laplaceExpansion(row: Int = -1, col: Int = -1): T

    abstract fun toArray(): Array<Array<T>>

    abstract val lup: LUPDecomposition

    fun isDiagonal(): Boolean {
        if (!this.isSquare()) throw Error.DimensionMisMatch()
        return this.all(Selector.OFF_DIAGONAL) { x -> x == 0.0 }
    }

    fun isEmpty(): Boolean =
        (this.rowLength == 0) || (this.colLength == 0)

    fun isHermitian(): Boolean {
        if (!this.isSquare()) throw Error.DimensionMisMatch()
        var ret = true
        this.forEachIndexed(Selector.UPPER) { v, r, c -> ret = ret && (Complex(v) == Complex(v).conjugate) }
        return ret
    }

    fun isLowerTriangular(): Boolean =
        this.all(Selector.STRICT_UPPER) { x -> x == 0.0 }

    fun isNormal(): Boolean {
        if (!this.isSquare()) throw Error.DimensionMisMatch()
        var ret = true
        this.vector.forEachIndexed { rowI: Vector<T>, i: Int ->
            this.vector.forEachIndexed { rowJ: Vector<T>, j: Int ->
                var s = Complex.ZERO
                this.vector.forEachIndexed { rowK: Vector<T>, k: Int ->
                    s += rowI[k] * rowJ[k].conjugate - rowK[i].conjugate * rowK[j]
                }
                if (s != Complex.ZERO) {
                    ret = false
                }
            }
        }
        return ret
    }

    fun isOrthogonal(): Boolean {
        if (!this.isSquare()) throw Error.DimensionMisMatch()
        var ret = true
        this.vector.forEachIndexed { row: Vector<T>, i: Int ->
            for (j in 0 until this.colLength) {
                var s = Complex.ZERO
                for (k in 0 until this.rowLength) {
                    s += row[k].complex * this[k, j].complex
                }
                if (s == (if (i == j) Complex.ONE else Complex.ZERO)) {
                    ret = false
                }
            }
        }
        return ret
    }

    fun isPermutation(): Boolean {
        if (!this.isSquare()) throw Error.DimensionMisMatch()
        val cols = Array(this.colLength) { false }
        var ret = true
        this.vector.forEachIndexed { row: Vector<T>, _ ->
            var found = false
            row.forEachIndexed { e, j ->
                if (e == 1) {
                    if (found || cols[j]) {
                        ret = false
                    }
                    cols[j] = true
                    found = true
                }
                else if (e != 0) {
                    ret = false
                }
            }
            if (!found) {
                ret = false
            }
        }
        return ret
    }

    fun isRegular(): Boolean =
        !this.isSingular()

    fun isSingular(): Boolean =
        this.determinant() == 0.0

    fun isSquare(): Boolean =
        this.rowLength == this.colLength

    fun isSymmetric(): Boolean {
        if (!this.isSquare()) throw Error.DimensionMisMatch()
        var ret = true
        this.forEachIndexed(Selector.STRICT_UPPER) { e, row, col ->
            if (e != this[col, row]) {
                ret = false
            }
        }
        return ret
    }

    fun isAntiSymmetric(): Boolean {
        if (!this.isSquare()) throw Error.DimensionMisMatch()
        var ret = true
        this.forEachIndexed(Selector.UPPER) { e, row, col ->
            if (e.complex != -this[col, row].complex) {
                ret = false
            }
        }
        return ret
    }

    fun isUnitary(): Boolean {
        if (!this.isSquare()) throw Error.DimensionMisMatch()
        var ret = true
        this.vector.forEachIndexed { row: Vector<T>, i: Int ->
            for (j in 0 until this.colLength) {
                var s = Complex.ZERO
                for (k in 0 until this.rowLength) {
                    s += row[k].conjugate * this[i, j].complex
                }
                if (s == (if (i == j) Complex.ONE else Complex.ZERO)) {
                    ret = false
                }
            }
        }
        return ret
    }

    fun isUpperTriangular(): Boolean =
        this.all(Selector.STRICT_LOWER) { x -> x == 0.0 }

    fun isZero(): Boolean =
        this.all { x -> x == 0.0 }
}