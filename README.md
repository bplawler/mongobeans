## Welcome to mongobeans

Mongobeans is not so much a library as it is an experiment in finding a 
good programming pattern.  I have been using Scala and MongoDB for about a
year now, and as I look back the code that I have written using Scala and
Mongo's Casbah library, I see all kinds of problems.  My biggest concern
is that I use the Casbah API all over the place in my Mongo beans, there
is no encapsulation or delegation of work.  The result is that I end up
directly accessing the database all over the place, and I have all kinds of
code baked right into my domain model that relies heavily on the Casbah API.

One of the most egregious fouls I have committed in my code is that my 
beans don't really have attributes.  Rather, accessor methods simply pass calls 
through to the Casbah API and retrieve values directly from memory:

```
  def deal = Config.deals.findOne(dealId).
    getOrElse(ex("deal with id %s not found".format(oid)))

  ...

  def retailer: Option[String] = deal.getAs[String]("retailer")
```

That's right.  Any time the application invokes `obj.retailer` to see what
the `retailer` value is for this instance, the `retailer` method invokes 
the `deal` method which goes to the `Config.deals` collection to get the 
deal -- EVERY TIME!  Performance has been fine so far, so this hasn't been
a problem, but this is clearly not the best solution out there.

Another issue is type safety.  The above example is a good illustration of
the problem.  The `retailer` field is of type String, but in my application
it is really more of an enumeration.  To support new retailers, new code
needs to be written.  Therefore it is fair to say that the legal values of
`retailer` would fall into a small set of possibilities, best implemented 
in Scala with case objects extending from a sealed base class.  My first 
pass at Mongo + Casbah pretty much threw that out the window.  String is
what gets stored, so String is what I used.

My last major problem is in the area of maintainability, particularly 
maintainability of code that is creating new Mongo documents.  The way my
code ended up working, I would basically build arbitrary maps, mapping
`String` to `Any` and I would then send those maps off to the right
collection and go from there.  

## Patterns

### Encapsulate direct access to Mongo collections.  


