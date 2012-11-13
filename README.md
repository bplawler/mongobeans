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
collection and go from there.  This again was a side effect of my beans
just being wrappers around database calls and not actually being beans
with attributes.  The biggest problem with this (there are many) is that
the pieces of code that are responsible for creating new objects need to 
know what the attributes are and what types need to be populated into them.
The compiler is of no help here because everything is a String or an Any,
sort of like writing Java code that works only with Objects.

## Not a Library

This project doesn't contain all that much code.  It's really just a 
project that I started from scratch so that I could try to address my 
problems outside of the context of my application.  This project will not
try to be yet another ORM tool for mapping scala objects to Mongo documents.
But I will try to write some code here that does a far better job of using 
Mongo, Casbah, and Scala to create maintainable code.

## Enumerations are a Pain

A problem that I always had with Java and Hibernate which has now followed
me over to the world of Mongo and Scala is that of storing enumerated values
in the database.  Getting them in there is pretty easy (toString anyone) but
getting them back out and having to map the String value back to an instance
of the item in the enumeration always required unsavory boilerplate code 
that had to be created per enumeration.  In this project I am looking to find
a way to minimize that boilerplate code.

## Concurrency

As I said, I don't want for this thing to be a huge complex piece of library
code that tries to take into account all situtations and environments.  
Concurrency is one example of a piece of functionalty that can be really 
difficult to deal with absent any knowledge of the application.  This 
application was initially built making having use of Mongo's atomic operations
for doing most of the database updates.  But as I mentioned before, code
that was responsible for actually creating new documents was completely 
separate from the code being used to maintain those documents, which resulted
in not only a dual maintenance nightmare, but also in a non type safe 
implementation that was very proned to maintenance problems.
