package transputer

import spinal.core._
import spinal.lib._
import spinal.lib.bus.bmb._
import spinal.lib.pipeline._

class DummyPlugin extends TransputerPlugin {
  override def pipelineStage(stage: PipelineStage): Unit = {}
  override def newCycle(): Unit = {}
}

class TransputerPipeline extends Pipeline {

  // Pipeline stages
  val fetch = new PipelineStage("Fetch")
  val decode = new PipelineStage("Decode")
  val execute = new PipelineStage("Execute")
  val memory = new PipelineStage("Memory")
  val writeback = new PipelineStage("Writeback")

  // Pipeline stages connections
  decode.io.input << fetch.io.output
  execute.io.input << decode.io.output
  memory.io.input << execute.io.output
  writeback.io.input << memory.io.output

  // Plugins
  val plugins = List[TransputerPlugin](
    new DummyPlugin()
  )

  plugins.foreach { plugin =>
    fetch.plugins += plugin
    decode.plugins += plugin
    execute.plugins += plugin
    memory.plugins += plugin
    writeback.plugins += plugin
  }

  // Default pipeline flush behavior
  fetch.flushOnJump := True
  decode.flushOnJump := True
  execute.flushOnJump := True
  memory.flushOnJump := True
  writeback.flushOnJump := True

  // Default pipeline stall behavior
  fetch.stageAllowOutput := True
  decode.stageAllowInput := True
  decode.stageAllowOutput := True
  execute.stageAllowInput := True
  execute.stageAllowOutput := True
  memory.stageAllowInput := True
  memory.stageAllowOutput := True
  writeback.stageAllowInput := True
  
}


// The Pipeline class defines a generic pipeline, which can be parameterized by the number of stages and the width of 
// the data bus. The pipeline is composed of a sequence of registers and a set of functions, called stageFunctions, 
// which represent the computation done at each stage.

// The stageFunction is a function that takes as input a PipelineStageInput object and returns a PipelineStageOutput 
// object. The PipelineStageInput object contains the data inputs to the stage, as well as the current cycle count 
// and the current stage index. The PipelineStageOutput object contains the data outputs from the stage, as well 
// as a boolean flag indicating whether the stage is ready to accept new inputs.

// The PipelineStage class is a wrapper around a stageFunction that adds some additional bookkeeping information, 
// such as the stage index and the register that holds the stage's inputs.

// The PipelineState class encapsulates the state of the pipeline, including the current cycle count and the data 
// in each stage's register.

// The Pipeline class has several convenience methods for adding new stages to the pipeline, such as insertStage, 
// appendStage, and removeStage.

// The Pipeline class also has methods for running the pipeline, including doStages and doAllStages. The doStages 
// method takes a PipelineState object as input and executes a single cycle of the pipeline. The doAllStages method 
//takes a PipelineState object as input and executes the pipeline until all stages are ready to accept new inputs.
