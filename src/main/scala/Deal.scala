package mongobeans

import com.mongodb.casbah.Imports._

object Config {
  val conn = MongoConnection("mongodb.circupon.internal")
  val db = conn("mongobeans")
  val deals = db("deals")
}

trait MongoBean {
  private var dbObj: Option[BasicDBObject] = None

  class Attribute(val fieldName: String) {
    def value: Option[String] =
      dbObj.map { _.getAs[String](fieldName) } getOrElse None

    def value_=(s: String) = 
      dbObj.getOrElse { 
        dbObj = Some(new BasicDBObject)
        dbObj.get
      } += (fieldName -> s) 
  }

  def save = dbObj.foreach { Config.deals.save(_) }
}

class Deal extends MongoBean {
  val source = new Attribute("source")
  val retailer = new Attribute("retailer")
}
