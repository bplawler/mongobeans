import com.mongodb.casbah.Imports._

object Config {
  val conn = MongoConnection("mongodb.circupon.internal")
  val db = conn("mongobeans")
  val deals = db("deals")

  deals.ensureIndex(MongoDBObject("href" -> 1), "idx-deals-href-unique", true)
}

