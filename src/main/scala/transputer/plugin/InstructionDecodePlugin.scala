package transputer.plugin

import transputer._
import spinal.core._
import spinal.lib._
import spinal.lib.pipeline._

class InstructionDecodePlugin extends TransputerPlugin with Stageable {

  override def setup(pipeline: Pipeline, state: PipelineState): Unit = {
    pipeline.addStage(this, pipeline.decode)

  }

  override def stageLogic(
      pipeline: Pipeline,
      state: PipelineState,
      clockDomain: ClockDomain
  ): Unit = {

    val instruction = state.pipelineData.getOrElse("Fetch", PipelineData()).currentInstruction
    val opcode = instruction(7 downto 4)
    val dataValue = instruction(3 downto 0)
    val registers = state.registers
    val core = state.core
    
    opcode match {
      case 0x00 => // j
        // Instruction: Jump to instruction pointer
        registers.iPtr := dataValue

      case 0x10 => // ldlp
        // Instruction: Load workspace pointer
        registers.wPtr := dataValue

      case 0x20 => // pfix
        // Instruction: Prefix
        val value =
          if (dataValue(3)) (dataValue.resize(32) | U"0xffffff00")
          else dataValue.resize(32)
        registers.aReg := value

      case 0x30 => // ldnl
        // Instruction: Load from workspace via bReg offset
        registers.aReg := core.workspace.readSync((registers.bReg + dataValue))

      case 0x40 => // ldc
        // Instruction: Load constant value
        registers.aReg := dataValue

      case 0x50 => // ldnlp
        // Instruction: Load from workspace via wPtr offset
        registers.aReg := core.workspace.readSync((registers.wPtr + dataValue))

      case 0x60 => // nfix
        // Instruction: Negative prefix
        val value =
          if (dataValue(3)) ((~dataValue).resize(32) | U"0xffffff00")
          else (~dataValue).resize(32)
        registers.aReg := value + 1

      case 0x70 => // ldl
        // Instruction: Load from workspace via wPtr offset
        registers.aReg := core.workspace.readSync((dataValue + registers.wPtr))

      case 0x80 => // adc
        // Instruction: Add with carry
        registers.cReg := registers.aReg.asSInt + registers.bReg.asSInt + registers.oReg.asSInt
        registers.aReg := registers.cReg(31 downto 0)
        registers.oReg := registers.cReg(32)

      case 0x90 => // call
        // Instruction: Call subroutine
        core.workspace.write((registers.wPtr - 1), registers.iPtr + 1)
        registers.wPtr := registers.wPtr - 3
        registers.iPtr := dataValue

      case 0xa0 => // cj
        // Instruction: Conditional jump
        when(registers.aReg.asSInt === 0) {
          registers.iPtr := dataValue
        }

      case 0xb0 => // ajw
        // Instruction: Jump to workspace address
        registers.wPtr := registers.aReg
        registers.iPtr := dataValue

      case 0xc0 => // eqc
        // Instruction: Compare to constant value
        when(registers.aReg === dataValue) {
          registers.oReg := 1
        }.otherwise {
          registers.oReg := 0
        }

      case 0xd0 => // stl
        // Instruction: Store to workspace via wPtr offset
        core.workspace.write(
          (registers.wPtr + dataValue),
          registers.aReg(31 downto 0)
        )

      case 0xe0 => // stnl
        // Instruction: Store to workspace via wPtr offset
        core.workspace.write((registers.wPtr + dataValue), registers.aReg)

      // This opr code constructs the secondaryOpcode by shifting the dataValue left by 3 bits and then 
      // combining it with the opcode value. It then uses the secondaryOpcode to look up the appropriate 
      // opcodeHandler from the secondaryOpcodeHandlers map, and executes it with the state parameter.
      
      // Transputer plugins can use the secondaryOpcodeHandler method to add secondary opcode handlers to 
      // the secondaryOpcodeHandlers map, which will then be used to execute the corresponding secondary 
      // opcodes in the InstructionDecodePlugin stage.
      
      case 0xf0 => // opr
        val secondaryOpcode = (dataValue << 3) | opcode
        val opcodeHandler = state.secondaryOpcodeHandlers.getOrElse(secondaryOpcode, throw new Exception(s"Unsupported secondary opcode: 0x${secondaryOpcode.toHexString}"))
        opcodeHandler.execute(state)
      
      case _ =>
        // unsupported instruction, raise an error
        throw new Exception(
          s"Unsupported instruction opcode: 0x${opcode.toHexString}"
        )
    }
  }
}
