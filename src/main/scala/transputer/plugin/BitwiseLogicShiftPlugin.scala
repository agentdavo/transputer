package transputer.plugin

import spinal.core._
import spinal.lib._
import spinal.lib.pipeline._

class BitwiseLogicShiftPlugin extends TransputerPlugin {

  override def setup(core: TransputerCore): Unit = {
    core.execute.addHandler(executeBitwiseLogicShift)
  }

  def executeBitwiseLogicShift(state: PipelineState, clockDomain: ClockDomain): Unit = {
    val instruction = state.currentInstruction
    val opcode = instruction(7 downto 0)

    opcode match {
      // Bitwise AND
      case 0x55 =>
        state.registers.aReg := state.registers.aReg & state.registers.bReg
        state.registers.bReg := state.registers.cReg
        state.registers.iPtr := state.registers.iPtr + 1

      // Bitwise OR
      case 0x56 =>
        state.registers.aReg := state.registers.aReg | state.registers.bReg
        state.registers.bReg := state.registers.cReg
        state.registers.iPtr := state.registers.iPtr + 1

      // Bitwise XOR
      case 0x57 =>
        state.registers.aReg := state.registers.aReg ^ state.registers.bReg
        state.registers.bReg := state.registers.cReg
        state.registers.iPtr := state.registers.iPtr + 1

      // Bitwise NOT
      case 0x58 =>
        state.registers.aReg := ~state.registers.aReg
        state.registers.iPtr := state.registers.iPtr + 1

      // Shift left
      case 0x59 =>
        state.registers.aReg := state.registers.aReg |<< (state.registers.bReg).resize(5)
        state.registers.bReg := state.registers.cReg
        state.registers.iPtr := state.registers.iPtr + 1

      // Shift right
      case 0x5A =>
        state.registers.aReg := state.registers.aReg |>> (state.registers.bReg).resize(5)
        state.registers.bReg := state.registers.cReg
        state.registers.iPtr := state.registers.iPtr + 1

      case _ =>
        // Do nothing for other instructions
    }
  }
}
