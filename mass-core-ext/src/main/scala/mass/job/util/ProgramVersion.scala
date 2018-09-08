package mass.job.util

import mass.model.job.Program

sealed trait ProgramVersion {
  val NAME: Program
  val VERSION: String
  def CLI: String = NAME.name.toLowerCase + VERSION
}

object ProgramVersion {
  private var _values = Vector[ProgramVersion]()
  def values: Vector[ProgramVersion] = _values

  private def register(v: ProgramVersion): Unit = {
    _values = _values :+ v
  }

  case object Scala211 extends ProgramVersion {
    override val NAME: Program = Program.SCALA
    override val VERSION: String = "2.11"
    override def CLI: String = NAME.name.toLowerCase
  }
  case object Scala212 extends ProgramVersion {
    override val NAME: Program = Program.SCALA
    override val VERSION: String = "2.12"
    override def CLI: String = NAME.name.toLowerCase
  }
  case object Java7 extends ProgramVersion {
    override val NAME: Program = Program.JAVA
    override val VERSION: String = "7"
    register(this)
  }
  case object Java8 extends ProgramVersion {
    override val NAME: Program = Program.JAVA
    override val VERSION: String = "8"
    register(this)
  }
  case object Python2_7 extends ProgramVersion {
    override val NAME: Program = Program.PYTHON
    override val VERSION: String = "2.7"
    register(this)
  }
  case object Python3_6 extends ProgramVersion {
    override val NAME: Program = Program.PYTHON
    override val VERSION: String = "3.6"
    register(this)
  }
  case object Bash extends ProgramVersion {
    override val NAME: Program = Program.SH
    override val VERSION: String = "bash"
    override val CLI: String = VERSION
    register(this)
  }
  case object Sh extends ProgramVersion {
    override val NAME: Program = Program.SH
    override val VERSION: String = "sh"
    override val CLI: String = VERSION
    register(this)
  }
  case object SqlJdbc extends ProgramVersion {
    override val NAME: Program = Program.SQL
    override val VERSION: String = "jdbc"
    override val CLI: String = "mass-jdbc-cli"
  }
  case object SqlPostgres extends ProgramVersion {
    override val NAME: Program = Program.SQL
    override val VERSION: String = "postgresql"
    override val CLI: String = "psql"
    register(this)
  }
  case object SqlMySQL extends ProgramVersion {
    override val NAME: Program = Program.SQL
    override val VERSION: String = "mysql"
    override val CLI: String = "mysql"
    register(this)
  }

  def get(program: Program, version: String): Option[ProgramVersion] = {
    ProgramVersion.values
      .find(pv => pv.NAME == program && pv.VERSION == version)
      .orElse(Option(program match {
        case Program.SCALA  => Scala212
        case Program.JAVA   => Java8
        case Program.PYTHON => Python3_6
        case Program.SQL    => SqlJdbc
        case Program.SH     => Bash
        case _              => null
      }))
  }

  def getString(program: Program, version: String): Option[String] = get(program, version).map(_.CLI)

  @inline def getStringOrElse(program: Program, version: String, deft: => String): String =
    getString(program, version).getOrElse(deft)

}
