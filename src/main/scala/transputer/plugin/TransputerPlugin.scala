package transputer.plugin

import spinal.lib.pipeline.PipelinePlugin

trait TransputerPlugin extends PipelinePlugin {
  def setup(core: TransputerCore): Unit
  
  def secondaryOpcodeHandler(prefix: Int, opcode: Int, handler: OpcodeHandler): Unit = {
    opcodeHandlers((prefix << 4) | opcode) = handler
  }
}

//   pipeline._ notes
//
//   pipeline.executeToWriteback.ready:   Returns a Bool that indicates whether the execute-to-writeback pipeline stage is ready to accept new data.
//   pipeline.executeToWriteback.flush:   Causes the execute-to-writeback pipeline stage to flush its data and reset its state.
//   pipeline.executeToWriteback.valid:   Returns a Bool that indicates whether the execute-to-writeback pipeline stage has valid data.
//   pipeline.executeToWriteback.payload: Returns the payload of the execute-to-writeback pipeline stage.
