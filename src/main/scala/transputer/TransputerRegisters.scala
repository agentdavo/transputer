package transputer

import spinal.core._

case class TransputerRegisters() extends Bundle {
  val aReg = Bits(32 bits)
  val bReg = Bits(32 bits)
  val cReg = Bits(32 bits)
  val oReg = Bits(32 bits)
  val wPtr = Bits(32 bits)
  val iPtr = Bits(32 bits)
}

class RegisterFile {
  
  val registers = Reg(TransputerRegisters())
  
}
