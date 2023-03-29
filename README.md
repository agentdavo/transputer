# transputer

The TransputerPipeline[T] class is a template for building a pipeline of stages, each of which is represented by an instance of the PipelineStage[T] class. Each stage in the pipeline can have one or more plugins that are used to execute instructions or perform other operations on the data passing through the pipeline.

To use the TransputerPipeline[T] class, you must first create instances of the PipelineStage[T] class that represent the stages of your pipeline. You can then connect the stages together in the order that they should be executed, and add any necessary plugins to each stage.

Each plugin that you create should extend the PipelinePlugin[T] trait. This trait defines four methods that are called at different points during the execution of the pipeline:

    preEval:       Called before the stage is executed.
    postEval:      Called after the stage is executed.
    beforeExecute: Called before the plugin's instruction is executed.
    afterExecute:  Called after the plugin's instruction is executed.

These methods allow you to modify the state of the pipeline or perform other operations before and after each stage or instruction is executed.

Once you have defined your pipeline and plugins, you can create an instance of the TransputerPipeline[T] class and pass in the stages and plugins that you have created. You can then call the execute method on the pipeline instance to begin executing the instructions.

The TransputerPipeline[T] class also provides several other methods that allow you to manipulate the state of the pipeline, including the ability to pause and resume execution, and the ability to add or remove plugins dynamically.

Overall, the TransputerPipeline[T] class provides a powerful and flexible framework for building pipelines of stages that can execute complex operations in a highly efficient and scalable manner.

In this example, we define a TransputerState bundle that holds the registers of the Transputer. We then create a TransputerPipeline[TransputerState] instance and define five pipeline stages: fetch, decode, execute, memAccess, and writeback. Each pipeline stage extends the PipelineStage[TransputerState] class and overrides the preEval and eval methods. The preEval method updates the pipeline output payload with the input state, while the eval method sets the pipeline output valid signal to the input valid signal.

The writeback pipeline stage also updates the Transputer state with the output payload if the instruction pointer matches.

```
import spinal.core._
import spinal.lib._

case class TransputerState() extends Bundle {
  val w = UInt(32 bits)
  val i = UInt(32 bits)
  val reg0 = Bits(32 bits)
  val a = Bits(32 bits)
  val b = Bits(32 bits)
  val c = Bits(32 bits)
}

class TransputerPipelineTopLevel extends Component {
  val io = new Bundle {
    val instructionBus = slave(Stream(Bits(32 bits)))
    val dataBus = slave(Stream(Bits(32 bits)))
  }

  val pipeline = new TransputerPipeline[TransputerState]

  val fetch = new PipelineStage[TransputerState]("fetch") {
    override def preEval(): Unit = {
      input.output.payload.i := input.state.i
    }

    override def eval(): Unit = {
      if (input.output.valid) {
        input.output.payload := input.state
      } else {
        input.output.payload.i := input.state.i + 4
      }

      input.output.valid := input.input.valid
    }
  }

  val decode = new PipelineStage[TransputerState]("decode") {
    override def preEval(): Unit = {
      input.output.payload := input.state
    }

    override def eval(): Unit = {
      input.output.valid := input.input.valid
    }
  }

  val execute = new PipelineStage[TransputerState]("execute") {
    override def preEval(): Unit = {
      input.output.payload := input.state
    }

    override def eval(): Unit = {
      input.output.valid := input.input.valid
    }
  }

  val memAccess = new PipelineStage[TransputerState]("memAccess") {
    override def preEval(): Unit = {
      input.output.payload := input.state
    }

    override def eval(): Unit = {
      input.output.valid := input.input.valid
    }
  }

  val writeback = new PipelineStage[TransputerState]("writeback") {
    override def preEval(): Unit = {
      input.output.payload := input.state
    }

    override def eval(): Unit = {
      if (input.output.valid && input.output.payload.i === input.state.i) {
        input.output.payload.w := input.state.w
        input.output.payload.reg0 := input.state.reg0
        input.output.payload.a := input.state.a
        input.output.payload.b := input.state.b
        input.output.payload.c := input.state.c
      }
      
      input.output.valid := input.input.valid
    }
  }

  fetch.io.output >> decode.io.input
  decode.io.output >> execute.io.input
  execute.io.output >> memAccess.io.input
  memAccess.io.output >> writeback.io.input

  io.instructionBus >> fetch.io.input
  io.dataBus >> memAccess.io.input
}
```
