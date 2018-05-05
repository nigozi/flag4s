# flag4s: A simple feature flag library for Scala
flag4s helps you manage feature flags in your application via scala functions and http apis.

flag4s consists of the following modules:
* flag4s-core: core libraries, key/val stores and scala functions.
* flag4s-api-http4s: http endpoints configuration for http4s.
* flag4s-api-akka-http: http endpoints configuration for akka-http.

# dependencies
flag4s uses IO type from cats-effect for all operations and all return types are IO.
```
libraryDependencies += "org.typelevel" %% "cats-effect" % "version"
```
 
# usage

## core
```
libraryDependencies += "io.nigo" %% "flag4s-core" % "0.1.5"
```

### choose your key/val store:
```scala
import flag4s.core.store._

implicit val store = ConsulStore("localhost", 8500)
// implicit val store = RedisStore("localhost", 6379)
// implicit val store = ConfigStore("path-to-config-file")
```

* you can choose one of the existing stores or create your own by implementing the Store trait.
* ConfigStore is not recommended as it does not support value modification. Also it only supports String type, means that all the value checks must be String.
To use the ConfigStore, put your feature flags in a .conf file in the following format:
```
features {
  featureA: true
  featureB: "on"
  ...  
} 
```

### use core functions to manage the flags:

all return types are **IO**, you should execute or compose them yourself.

**check flag**
```scala
import flag4s.core._

flag("featX") //returns the flag as type of Either[Throwable, Flag] 

fatalFlag("featX") //returns the flag or throws exception if flag doesn't exist

enabled(flag) //checks if the flag's value is true

is(flag, "on") //checks if the flag's value is "on"

withFlag("featX", true) { //executes the code block if flag's value is true
  //code block
}

//or

ifEnabled(flag) { //executes the code block if the flag's value is true
    //code block
}

ifIs(flag, "enabled") { //executes the code block if the flag's value is "enabled" 
    //code block
}

get[Double](flag) // returns the flag's value as Double
```

**create/set flag**
```scala
newFlag("featX", true) // creates a new flag with value true

switchFlag("featX", false) // sets the flag's value to false

set(flag, "off") // sets the flag's value to "off"
```

**syntax**
```scala
import flag4s.core._
import flag4s.syntax._

val flag = fatalFlag("featX").unsafeRunSync()

flag.enabled

flag.is("on")

flag.ifEnabled {
    // feature ...
}

flag.ifIs("on") {
    // feature ...
}

flag.get[Double]

flag.set("off")
```

## http Api
**http4s**
```
libraryDependencies += "io.nigo" %% "flag4s-api-http4s" % "0.1.5"
```
```scala
import flag4s.api.Http4sFlagApi

implicit val store = RedisStore("localhost", 6379)

BlazeBuilder[IO]
    .bindHttp(8080)
    .withWebSockets(true)
    .mountService(Http4sFlagApi.service(), "/")
    .serve
```

**akka-http**
```
libraryDependencies += "io.nigo" %% "flag4s-api-akka-http" % "0.1.5"
```
```scala
import flag4s.api.AkkaFlagApi

implicit val store = RedisStore("localhost", 6379)

Http().bindAndHandle(AkkaFlagApi.route(), "localhost", 8080)
```

### endpoints

**create/update a flag**
```
http PUT localhost:8080/flags key=featureA value=on
http PUT localhost:8080/flags key=featureB value:=true
```

**get a flag**
```
http localhost:8080/flags/featureA
```

**get all flags**
```
http localhost:8080/flags
```

**enable a boolean flag**
```
http POST localhost:8080/flags/featureB/enable
```

**disable a boolean flag**
```
http POST localhost:8080/flags/featureB/disable
```

**delete a flag**
```
http DELETE localhost:8080/flags/featureA
```
