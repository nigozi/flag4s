# flag4s: A simple feature flag library for Scala
flag4s helps you manage feature flags in your application via scala functions and http apis.

flag4s consists of the following modules:
* flag4s-core: core libraries, key/val stores and scala functions.
* flag4s-api-http4s: http endpoints configuration for http4s.
* flag4s-api-akka-http: http endpoints configuration for akka-http.

# Dependencies
flag4s uses IO type from cats-effect for all operations and all return types are IO.
```
libraryDependencies += "org.typelevel" %% "cats-effect" % "version"
```
 
# Usage

## Core
```
libraryDependencies += "io.nigo" %% "flag4s-core" % "0.1.3"
```

### Choose your key/val store:
```
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

### Use core functions to manage the flags:

**Core Functions**

all return types are IO, execute or compose them yourself.
 
```
import flag4s.core._

flag("featureA") // returns the flag as type of Either[Throwable, Flag]

fatalFlag("featureA") // returns the flag or throws exception if flag doesn't exist

withFlag("featureA", true) { // executes the function if flag's value is true
  // new feature ...
}

withFlag("featureB", "enabled") { // executes the function if the flag's value is "enabled"
  // new feature ...
}

newFlag("featureC", true) // creates a new flag with value true

enabled(flag) // checks if the flag's value is true

is(flag, "on") // checks if the flag's value is "on"

ifEnabled(flag) { // executes the given function if the flag's value is true
    // feature
}

ifIs(flag, "enabled") { // executes the function if the flag's value is "enabled" 
    // feature
}

get[Double](flag) // returns the flag's value as Double

set(flag, "off") // sets the flag's value to "off"
```

**Syntax**

there are also some syntax sugars for convenience: 
```
import flag4s.core._
import flag4s.syntax._

val flag = fatalFlag("featureA").unsafeRunSync()

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

## Http Api
**http4s**
```
libraryDependencies += "io.nigo" %% "flag4s-api-http4s" % "0.1.3"
```
```
import flag4s.api.Http4sFlagApi

implicit val store = RedisStore("localhost", 6379)

def stream(args: List[String], requestShutdown: IO[Unit]) =
for {
  exitCode <- BlazeBuilder[IO]
    .bindHttp(8080)
    .withWebSockets(true)
    .mountService(Http4sFlagApi.service(), "/")
    .serve
} yield exitCode
```

**akka-http**
```
libraryDependencies += "io.nigo" %% "flag4s-api-akka-http" % "0.1.3"
```
```
import flag4s.api.AkkaFlagApi

implicit val store = RedisStore("localhost", 6379)

Http().bindAndHandle(AkkaFlagApi.route(), "localhost", 8080)
```

### Endpoints

**create/update a flag**
```
http PUT localhost:8080/flags key=featureA value="on"
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