import org.specs2._
import specification.{Before, Context}
import com.mongodb.casbah.Imports._

class HelloWorldSpec extends Specification { def is =           sequential ^
  "This is a specification to test the mongobeans functionality"           ^
                                                                           p^
  "Instantiating a new Deal instance should"                               ^
    "allow me to set and get String properties"                            ! e1^
    "but should not save anything to the database"                         ! e2^
    "until the save method is invoked on the bean implementation"          ! e3^
                                                                           end
    
  def e1 = {
    val d = defaultDeal
    d.source.value must_== Some("TestSource")
  }

  def e2 = {
    val d = defaultDeal
    Config.deals.count() must_== 0
  }

  def e3 = pending

  implicit val before: Context = new Before { 
    def before = Config.deals.remove(MongoDBObject())
  }
    
  def defaultDeal = {
    val d = new Deal 
    d.source.value = "TestSource"
    d
  }
}

