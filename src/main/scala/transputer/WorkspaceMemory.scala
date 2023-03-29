package transputer

import transputer.plugin._

import spinal.core._
import spinal.lib._
import spinal.lib.bus.bmb._

class WorkspaceMemory(depth: Int, wordWidth: Int) extends Component {

  val io = new Bundle {
    val dBusA = slave(Bmb(DBusConfigAB()))
    val dBusB = slave(Bmb(DBusConfigAB()))
  }

  val mem = Mem(Bits(wordWidth bits), depth)
  val writeAddress = Reg(UInt(log2Up(depth) bits))
  val readAddress1 = Reg(UInt(log2Up(depth) bits))

  // Choose between read port 1 and 2 based on readAddress1 parity
  val readData1 = mem.readAsync(readAddress1, 1)
  val readData2 = mem.readAsync(readAddress1 + 1, 1)
  val readData = Mux(readAddress1(0), readData2, readData1)

  // Connect write port
  io.dBusA.cmd.valid := False
  io.dBusA.cmd.wr := False
  io.dBusA.cmd.size := 2
  io.dBusA.cmd.address := writeAddress
  io.dBusA.cmd.context := 0
  io.dBusA.cmd.source := 0
  io.dBusA.cmd.data := io.dBusB.cmd.data

  when(io.dBusA.cmd.fire) {
    writeAddress := io.dBusA.cmd.address + 1
  }

  io.dBusA.rsp.ready := False
  io.dBusA.rsp.source := 0
  io.dBusA.rsp.context := 0
  io.dBusA.rsp.data := readData

  // Connect read ports to appropriate bus
  val interconnect = BmbInterconnectGenerator()
  interconnect.addSlave(bus = io.dBusA, mapping = AddressMapping(0, depth), littleEndianness = true)
  interconnect.addSlave(bus = io.dBusB, mapping = AddressMapping(0, depth), littleEndianness = true)
  interconnect.addMaster(bus = io.dBusA, mapping = AddressMapping(0, depth), littleEndianness = true)
  interconnect.addMaster(bus = io.dBusB, mapping = AddressMapping(0, depth), littleEndianness = true)

  // Connect workspace memory to the interconnect
  io.dBusA << interconnect.io.slaves(0)
  io.dBusB << interconnect.io.slaves(1)
  interconnect.io.masters(0) << io.dBusA.rsp
  interconnect.io.masters(1) << io.dBusB.rsp
}
