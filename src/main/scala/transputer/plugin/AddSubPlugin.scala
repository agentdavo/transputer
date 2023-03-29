package transputer.plugin

import spinal.core._
import spinal.lib._
import spinal.lib.pipeline._

class AddSubPlugin extends TransputerPlugin {

  override def setup(core: TransputerCore): Unit = {
    core.execute.addHandler(executeAddSub)
  }

  def executeAddSub(state: PipelineState, clockDomain: ClockDomain): Unit = {
    val instruction = state.currentInstruction
    val opcode = instruction(7 downto 0)

    // Extract operands from registers
    val a = state.registers.aReg
    val b = state.registers.bReg
    val c = state.registers.cReg

    // Compute modified operand B for subtraction operations
    val isSubtraction = opcode == 0x03 || opcode == 0x04 || opcode == 0x07
    val bModified = Mux(isSubtraction, ~b, b)

    // Adder and subtractor
    val tempResult = Bits(32 bits)
    val tempOverflow = Bool
    val tempCarryOut = Bool

    // Multiplexer for add/sub operand selection
    val opBModified = Bits(32 bits)
    opBModified := Mux(isSubtraction, ~b, b)

    // Common operations for all add/sub instructions
    tempResult := a + opBModified + isSubtraction
    tempOverflow := (tempResult.msb ^ a.msb ^ bModified.msb) & ~(isSubtraction.asBool ^ a.msb)
    tempCarryOut := tempResult(31) ^ a(31) ^ bModified(31)

    // Additional operations for ADDC/SUBC
    if (opcode == 0x04 || opcode == 0x05) {
      tempResult := a + opBModified + state.overflow
      tempOverflow := (tempResult.msb ^ a.msb ^ bModified.msb) & ~(isSubtraction.asBool ^ a.msb ^ state.overflow)
      tempCarryOut := tempResult(31) ^ a(31) ^ bModified(31) ^ state.overflow
    }

    // Additional operations for ADDL/SUBL
    if (opcode == 0x06 || opcode == 0x07) {
      val tempLongResult = a.asSInt.resize(64) + b.asSInt.resize(64)
      state.longResult := tempLongResult.asBits
      state.overflow := False
      state.carryOut := False
    }

    // Write back result to A register
    state.registers.aReg := tempResult.asBits
    state.overflow := tempOverflow
    state.carryOut := tempCarryOut

    // Update instruction pointer
    state.registers.iPtr := state.registers.iPtr + 1
  }
}
