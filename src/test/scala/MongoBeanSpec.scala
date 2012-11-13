import org.specs2._
import specification.{Before, Context}
import com.mongodb.casbah.Imports._

class MongoBeanSpec extends Specification { def is =            sequential ^
  "This is a specification to test the mongobeans functionality"           ^
                                                                           p^
  "Instantiating a new Deal instance should"                               ^
    "allow me to set and get String properties"                            ! e1^
    "but should not save anything to the database"                         ! e2^
    "until the save method is invoked on the bean"                         ! e3^
                                                                           p^
  "The _id attribute has special behavior.  It should"                     ^
    "not be set for new instances of mongobeans"                           ! e4^
    "be automatically updated when a new bean is saved to the collection"  ! e5^
    "if set to a value by the application, a call to load() should load"   +
      "the corresponding document behind this bean"                        ! e6^
                                                                           p^
  "Concurrent modifications to different beans with the same backing doc"  ^
    "should be immediately available in both beans"                        ! e7^
                                                                           p^
  "Typed enumerations in and out of the database should"                   ^
    "allow enums to be stored like other attributes"                       ! e8^
    "have the same concurrency support as other attributes"                ! e9^
                                                                           end
    
  def e1 = {
    val d = defaultDeal
    d.source.value must_== Some("TestSource")
  }

  def e2 = {
    val d = defaultDeal
    Config.deals.count(MongoDBObject()) must_== 0
  }

  def e3 = {
    val d = defaultDeal
    d.save
    Config.deals.count(MongoDBObject()) must_== 1
  }

  def e4 = {
    val d = defaultDeal
    !(d._id.value isDefined)
  }
  
  def e5 = {
    val d = defaultDeal
    d.save
    d._id.value.isDefined
  }

  def e6 = {
    val d = defaultDeal
    d.save
    val _id = d._id.value
    val newDeal = new Deal
    newDeal._id.value = _id.get
    newDeal.load
    newDeal.source.value must_== d.source.value
  }

  def e7 = {
    val d1 = defaultDeal
    d1.save
    val _id = d1._id.value
    val d2 = new Deal
    d2._id.value = _id.get
    d2.load

    d2.number.value = 3
    d1.number.value must_== Some(3)
  }

  def e8 = {
    val d = new Deal
    d.retailer.value = RetailerEnum.Walgreens
    d.retailer.value must_== Some(RetailerEnum.Walgreens)
  }

  def e9 = {
    val d1 = new Deal
    d1.retailer.value = RetailerEnum.Walgreens
    d1.save
    val d2 = new Deal
    d2._id.value = d1._id.value.get
    d2.load
    
    d1.retailer.value = RetailerEnum.CVS
    d2.retailer.value must_== Some(RetailerEnum.CVS)
  }

  implicit val before: Context = new Before { 
    def before = Config.deals.remove(MongoDBObject())
  }
    
  def defaultDeal = {
    val d = new Deal 
    d.source.value = "TestSource"
    d
  }
}

