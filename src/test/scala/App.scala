package mongobeans

trait Capitalize extends ValueConverter[String] {
  protected override def makeValidatedValue(s:String): String = {
    s.toUpperCase
  }
}

sealed class Retailer extends StringEnumItem(RetailerEnum) 

object RetailerEnum extends StringEnum[Retailer] {
  Walgreens ; case object Walgreens extends Retailer 
  CVS       ; case object CVS extends Retailer
  RiteAid   ; case object RiteAid extends Retailer
}

class Deal extends CircuponMongoBean {
  val coll = Config.deals

  val _id = new Attribute[org.bson.types.ObjectId]("_id")
  val brands = new LockableSetAttribute[String]("brand")
  val caps = new AssertedAttribute[String]("caps") with Capitalize
  val href = new Attribute[String]("href") 
  val number = new Attribute[Long]("number")
  val retailer = new EnumAttribute[Retailer]("retailer", RetailerEnum)
  val source = new Attribute[String]("source") 
  val title = new AssertedAttribute[String]("title") 
  val validTo = new AssertedAttribute[java.util.Date]("validTo")
  val zipCodes = new Attribute[Set[String]]("zipCode")
  val history = new ListAttribute[String]("history")
}

class Survey extends MongoBean {
  val coll = Config.survey

  val _id = new Attribute[org.bson.types.ObjectId]("_id")
  val question = new Attribute[String]("question")
  val answers = new MapAttribute[String]("answers")
}

/*
class Answer extends MongoBean {
  val answer = new Attribute[String]("answer")
  val email = new SetAttribute[String]("emails")
}
*/
