package transputer

import transputer.plugin._

import spinal.core._
import spinal.core.sim._

import spinal.lib.bus.bmb._
import spinal.lib.bus.bmb.sim._
import scala.collection.mutable.Queue
import scala.util.Random
import spinal.lib.pipeline.fork._

case class Transputer() extends Component {
  val config = TransputerCoreConfig(
    coreClockFrequency = 50 MHz,
    workspaceDepth = 1024,
    workspaceWordWidth = 32
  )

  val core = new TransputerCore(config, IBusConfig(), DBusConfigAB(), DBusConfigW())

  // Connect clock and reset to the core
  core.clockDomain.clock := clockDomain.clock
  core.clockDomain.reset := clockDomain.reset

  // Instantiate the FetchPlugin
  val fetchPlugin = new FetchPlugin()
  val instructionDecodePlugin = new InstructionDecodePlugin(core)

  // Add the FetchPlugin to the core
  core.pipeline.stages(0) := fetchPlugin
  core.pipeline.stages(1) := instructionDecodePlugin
}

object TransputerTopLevelVerilog extends App {
  SpinalConfig(targetDirectory = "rtl").generateVerilog(Transputer())
}

object TransputerTopLevelVhdl extends App {
  SpinalConfig(targetDirectory = "rtl").generateVhdl(Transputer())
}

object TransputerSim extends App {
  // Set up the simulation clock
  SimConfig.withWave
    .doSimUntilVoid(seed = 42) { dut =>
      dut.clockDomain.forkStimulus(10)

// Create a BmbMasterAgentSim to drive read/write transactions to dBusA
val dBusAAgent = BmbMasterAgentSim(dut.core.io.dBusA, dut.clockDomain)
dBusAAgent.rspQueue
dBusAAgent.startTransaction(0x1000, 0x12345678)
dBusAAgent.startTransaction(0x2000, 0x9abcdef0)

// Create a BmbMasterAgentSim to drive read/write transactions to dBusB
val dBusBAgent = BmbMasterAgentSim(dut.core.io.dBusB, dut.clockDomain)
dBusBAgent.rspQueue
dBusBAgent.startTransaction(0x3000, 0x55555555)
dBusBAgent.startTransaction(0x4000, 0xaaaaaaaa)

// Create a BmbMasterAgentSim to drive read transactions to iBus
val iBusAgent = BmbMasterAgentSim(dut.core.io.iBus, dut.clockDomain)
iBusAgent.rspQueue
iBusAgent.startTransaction(0x5000, 0x11111111, isWrite = false)
iBusAgent.startTransaction(0x6000, 0x22222222, isWrite = false)

dut.clockDomain.waitSampling(100)

// Create a BmbMasterAgentSim to drive read transactions to oBus
val dBusWAgent = BmbMasterAgentSim(dut.core.pipeline.dBusW, dut.clockDomain)
dBusWAgent.rspQueue
dBusWAgent.startTransaction(0x7000, 0x33333333, isWrite = false)
dBusWAgent.startTransaction(0x8000, 0x44444444, isWrite = false)

dut.clockDomain.waitSampling(100)


      simSuccess()
    }
}
