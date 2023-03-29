package transputer.plugin

import spinal.core._
import spinal.lib._
import spinal.lib.pipeline._
import spinal.lib.bus.bmb._
import spinal.lib.bus.bmb.{Bmb, BmbParameter}
import spinal.lib.bus.bmb.Bmb.Cmd

class FetchPlugin extends Stageable[PipelineData] with TransputerPlugin {
  val burstSize = 32 // Fetch 32 instructions at once
  val burstCounter = Reg(UInt(log2Up(burstSize) bits)) init (0)
  val readRequestState = Reg(UInt(2 bits)) init (0)

  override def setup(core: TransputerCore): Unit = {
    core.pipeline.addStage(this, core.fetch)
  }

  override def stageLogic(pipeline: Pipeline, stage: PipelineStage, inputData: PipelineData, outputData: PipelineData, clockDomain: ClockDomain): Unit = {
    // Read the instruction from iBus
    val instruction = readInstruction(inputData.registers.iPtr, inputData.iBus)

    // Set the current instruction in the pipeline data
    outputData.currentInstruction := instruction

    // Increment the instruction address if the fetch stage is complete
    if (stage.isStageComplete) {
      outputData.registers.iPtr := inputData.registers.iPtr + 4
    }
  }

  def readInstruction(address: UInt, iBus: Bmb): Bits = {
    val instruction = Reg(Bits(32 bits))
    instruction := 0

    // Define a read request state machine
    switch(readRequestState) {
      is(0) {
        // Send a memory read request
        iBus.cmd.valid := True
        iBus.cmd.address := address
        iBus.cmd.opcode := Bmb.Cmd.Opcode.READ
        iBus.cmd.length := burstSize - 1
        when(iBus.cmd.ready) {
          readRequestState := 1
        }
      }
      is(1) {
        // Wait for the read response
        iBus.rsp.ready := True
        when(iBus.rsp.valid) {
          instruction := iBus.rsp.data
          burstCounter := burstCounter + 1
          when(burstCounter === burstSize - 1) {
            readRequestState := 0
            burstCounter := 0
          }
        }
      }
    }

    instruction
  }
}
