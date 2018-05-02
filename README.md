# flag4s: A simple feature flag library for Scala
flag4s is a feature flag library which provides apis for managing the feature flags and switching them at runtime!
flag4s consists of the following modules:
```
libraryDependencies += "io.nigo" %% "flag4s-core" % "0.1.1"
libraryDependencies += "io.nigo" %% "flag4s-api-http4s" % "0.1.1"
libraryDependencies += "io.nigo" %% "flag4s-api-akka-http" % "0.1.1"
```
* flag4s-core: core libraries and scala apis for managing the flags.
* flag4s-api-http4s: http endpoints configuration for http4s.
* flag4s-api-akka-http: http endpoints configuration for akka-http.

# Usage
## Core

### Choose your key/val store:
```
import flag4s.core.store._

implicit val store = ConsulStore("localhost", 8500)
// implicit val store = RedisStore("localhost", 6379)
// implicit val store = ConfigStore("path-to-config-file")
```

* you can choose one of the existing stores or create your own by implementing the Store trait.
* ConfigStore is not recommended as it doesn't support value modification.

### Use core functions to manage the flags:
**Core Functions**
```
import flag4s.core._
```

```
flag("featureA").unsafeRunSync() // get an existing flag as IO[Either[Throwable, Flag]]

fatalFlag("featureA").unsafeRunSync() // get an existing flag and throw exception if flag doesn't exist

withFlag("featureA") { // execute the given function if the flag is on
  // new feature ...
}.unsafeRunSync()

withFlag("featureA", "enabled") { // execute if the flag is set to "enabled"
  // new feature ...
}.unsafeRunSync()

newFlag("featureB", true).unsafeRunSync() // create a new flag with true as value

enabled(flag) // check if the boolean flag is on

is(flag, "on") // check if non-boolean flag is set to the given value

ifEnabled(flag) { // execute if the boolean flag is on
    // feature
}

ifIs(flag, "on") { // execute if the non-boolean flag is set to the given value
    // feature
}

get[Double](flag) // return the flag's value as Double

set(flag, "off") // set the flag's value to "off"
```

**Syntax**
```
import flag4s.syntax._
```
```
fatalFlag("featureA").enabled.unsafeRunSync()

fatalFlag("featureA").is("on").unsafeRunSync()

fatalFlag("featureA").ifEnabled {
    // feature ...
}.unsafeRunSync()

fatalFlag("featureA").ifIs("on") {
    // feature ...
}.unsafeRunSync()

fatalFlag("featureA").get[Double]

fatalFlag("featureA").set("off").unsafeRunSync()
```

## Http Api
**http4s**
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