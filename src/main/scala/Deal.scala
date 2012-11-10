package mongobeans

import com.mongodb.casbah.Imports._

object Config {
  val conn = MongoConnection("mongodb.circupon.internal")
  val db = conn("mongobeans")
  val deals = db("deals")
}

class Deal {
  private var dbObj: Option[BasicDBObject] = None

  def source: Option[String] = 
    dbObj.map { _.getAs[String]("source") } getOrElse None

  def source_=(s: String) = 
    dbObj.getOrElse { 
      dbObj = Some(new BasicDBObject)
      dbObj.get
    } += ("source" -> s) 

  def save = dbObj.foreach { Config.deals.save(_) }
}
