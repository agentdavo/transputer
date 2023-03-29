package transputer

import spinal.core._
import spinal.lib._
import spinal.core.sim._

import spinal.lib.bus.bmb._
import spinal.lib.bus.bmb.{Bmb, BmbParameter}

import spinal.lib.pipeline._
import spinal.lib.soc._
import transputer.plugin._

case class IBusConfig() extends BmbParameter(
    addressWidth = 32,
    dataWidth = 32,
    lengthWidth = 2
)

case class DBusConfigAB() extends BmbParameter(
    addressWidth = 32,
    dataWidth = 32,
    lengthWidth = 2,
    sourceWidth = 0,
    contextWidth = 0,
    alignment = BmbParameter.BurstAlignement.LENGTH,
    maximumPendingTransactionPerId = 1,
    maximumPendingTransaction = 1
)

case class DBusConfigW() extends BmbParameter(
    addressWidth = 32,
    dataWidth = 32,
    lengthWidth = 2,
    sourceWidth = 0,
    contextWidth = 0,
    alignment = BmbParameter.BurstAlignement.LENGTH,
    maximumPendingTransactionPerId = 1,
    maximumPendingTransaction = 1
)

class TransputerCore(
  config: TransputerCoreConfig,
  iBusConfig: IBusConfig,
  dBusConfigAB: DBusConfigAB,
  dBusConfigW: DBusConfigW
) extends Component {

  val io = new Bundle {
    val iBus = master(Bmb(iBusConfig))
    val dBusA = master(Bmb(dBusConfigAB))
    val dBusB = master(Bmb(dBusConfigAB))
    val dBusW = master(Bmb(dBusConfigW))
  }

  val pipeline = new TransputerPipeline()

  val registerFile = new RegisterFile()

  val workspace = new WorkspaceMemory(depth = config.workspaceDepth, config.workspaceWordWidth)

  io.dBusA <> workspace.io.dBusA
  io.dBusB <> workspace.io.dBusB
  io.iBus >> pipeline.io.iBus
  io.dBusW << pipeline.io.dBusW

  val clockDomain = ClockDomain.current
  val state = pipeline.state

  when(clockDomain.onRisingEdge()) {
    pipeline.execute(clockDomain)
    registerFile.execute(state.currentOpcode, state.result)
  }
}
