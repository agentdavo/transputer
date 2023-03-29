package transputer.plugin

import transputer._
import spinal.core._
import spinal.lib._
import spinal.lib.pipeline._

import spinal.core._
import spinal.lib._

// MulPlugin is a hardware module that implements a pipelined architecture for efficient multiplication
// operations. The module contains six methods for performing different types of multiplication operations
// on input operands, which are fed into four 8x8 multipliers in the execute stage and four 8x8 multipliers
// in the writeback stage.

// To improve the efficiency of the pipeline, the plugin divides the input operands into smaller segments
// and performs parallel multiplications using the multipliers. The result of each parallel multiplication
// is then combined to form the final output, which is written back to the pipeline.

class Multiplier(width: Int) extends Component {
  val io = new Bundle {
    val signed = in Bool
    val a = in SInt (width bits)
    val b = in SInt (width bits)
    val result = out SInt ((2 * width) bits)
  }

  // Resize inputs for multiplication
  val aResized = io.a.resize(2 * width)
  val bResized = io.b.resize(2 * width)

  // Unsigned multiplication
  val product = Reg(UInt((2 * width) bits)) init (0)
  product := (aResized.asUInt * bResized.asUInt)

  // Shifting result
  val result = (product >> width).asSInt.resize(2 * width)

  // Sign handling
  io.result := Mux(
    io.signed, {
      val isPositive = io.a.msb === io.b.msb
      Mux(isPositive, result, (~result + 1))
    },
    result
  )
}

class MulPlugin extends TransputerPlugin {

  // Two 8x8 multipliers in execute stage
  val mul8x8Execute = Seq.fill(2)(new Multiplier(8))

  // Two 8x8 multipliers in writeback stage
  val mul8x8Writeback = Seq.fill(2)(new Multiplier(8))

  override def setup(pipeline: Pipeline, state: PipelineState): Unit = {
    addSecondaryOpcodeHandler(0x53, executeMul8(true) _)
    addSecondaryOpcodeHandler(0x59, executeMul8(false) _)
    addSecondaryOpcodeHandler(0x5b, executeMulU8(false) _)
    addSecondaryOpcodeHandler(0x5f, executeMul32(true) _)
    addSecondaryOpcodeHandler(0x7f, executeMulU32(false) _)
    addSecondaryOpcodeHandler(0x23, executeLongMul(false) _)
  }

  def performMultiplications(a: SInt, b: SInt, signed: Boolean, segments: Int)(
      idx: Int
  ): List[SInt] = {
    val aSegments =
      (0 until segments).map(i => a((8 * (i + 1) - 1) downto (8 * i)))
    val bSegments =
      (0 until segments).map(i => b((8 * (i + 1) - 1) downto (8 * i)))

    aSegments.flatMap { aSeg =>
      bSegments.map { bSeg =>
        mul8x8Execute(idx).io.signed := signed
        mul8x8Execute(idx).io.a := aSeg
        mul8x8Execute(idx).io.b := bSeg
        mul8x8Execute(idx).io.result
      }
    }
  }
  
  
  def executeMul8(signed: Boolean)(state: PipelineState): Unit = {
    val a = state.registers.aReg.asSInt.resize(16)
    val b = state.registers.bReg.asSInt.resize(16)
    val idx = state.currentInstruction & 0x3

    when(pipeline.executeToWriteback.ready) {
      val results = performMultiplications(a, b, signed, 1)(idx)
      val combinedResult = results.head.resize(16)
      pipeline.executeToWriteback.payload := combinedResult
      state.registers.aReg := combinedResult
      state.registers.iPtr +:= 1

      when(pipeline.writebackToMemory.ready) {
        mul8x8Writeback(idx).io.signed := signed
        mul8x8Writeback(idx).io.a := a(7 downto 0)
        mul8x8Writeback(idx).io.b := b(7 downto 0)
        pipeline.writebackToMemory.payload := mul8x8Writeback(idx).io.result(7 downto 0)
        pipeline.writebackToMemory.valid := True
      }
    }
  }

  def executeMulU8(signed: Boolean)(state: PipelineState): Unit = {
    executeMul8(false)(state)
  }

  def executeMul32(signed: Boolean)(state: PipelineState): Unit = {
    executeLongMul(signed)(state)
  }

  def executeMulU32(signed: Boolean)(state: PipelineState): Unit = {
    executeLongMul(false)(state)
  }

  def executeLongMul(signed: Boolean)(state: PipelineState): Unit = {
    val a = state.registers.aReg.asSInt.resize(64)
    val b = state.registers.bReg.asSInt.resize(64)
    val idx = state.currentInstruction & 0x3

    when(pipeline.executeToWriteback.ready) {
      val results = performMultiplications(a, b, signed, 4)(idx)
      val combinedResult = results.zipWithIndex
        .map { case (result, i) =>
          result.resize(64) << (i * 8)
        }
        .reduce(_ + _)

      pipeline.executeToWriteback.payload := combinedResult
      state.registers.aReg := combinedResult
      state.registers.iPtr +:= 1
    }
  }
}
