package mongobeans

trait Logging {
  self: MongoBean =>

  val logMessages = new ListAttribute[String]("logMessages")

  def clearHistory: Unit = logMessages.unset

  def log(msg: String): Unit = {
    val m = "%s: %s".format(new java.util.Date, msg)
    logMessages.add(msg)
  }

  def prettyPrint = {
    logMessages.value.getOrElse(List()).mkString("\n")
  }
}
