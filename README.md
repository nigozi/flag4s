# flag4s: A simple feature flag library for Scala
flag4s is a feature flag library which provides apis for managing the feature flags and switching them at runtime!
flag4s consists of the following modules:
* flag4s-core: provides default key/val stores and language apis for managing the flags.
* flag4s-api-http4s: provides a http4s service for flag management. 
* flag4s-api-akka-http: provides a akka-http route for flag management.

# Usage
You first need to choose the key/val store:
```
import flag4s.core.store._

implicit val store = ConsulStore("localhost", 8500)
// implicit val store = RedisStore("localhost", 6379)
```

## Scala Api
import core functions and use them to manage the flags:
```
import flag4s.core._

// execute if flag is set to true
withFlag("featureA") {
  // new feature ...
}
.unsafeRunSync()

// execute if flag is set to "on"
withFlag("featureA", "on") {
  // new feature ...
}
.unsafeRunSync()

// create a new flag
newFlag("featureB", true).unsafeRunSync()
```

or use the syntax:
```
import flag4s.core._
import flag4s.syntax._

// check if flag is set to true
fatalFlag("featureA").enabled.unsafeRunSync()

// check if fag is set to the given value
fatalFlag("featureA").is("on").unsafeRunSync()

// execute if flag is set to true
fatalFlag("featureA").ifEnabled {
    // new feature ...
}.unsafeRunSync()

// execute if flag is set to the given value
fatalFlag("featureA").ifIs("on") {
    // new feature ...
}.unsafeRunSync()

// set the flag's value
fatalFlag("featureA").set("off").unsafeRunSync()
```

## Http Api
**flag4s-api-http4s** and **flag4s-api-akka-http** modules provide http endpoint configurations for http4s and akka-http.

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

### Apis

**create/update a flag**
```
http PUT localhost:8888/flags key=featureA value="on"
http PUT localhost:8888/flags key=featureB value:=true
```

**get a flag**
```
http localhost:8888/flags/featureA
```

**get all flags**
```
http localhost:8888/flags
```

**enable a boolean flag**
```
http POST localhost:8888/flags/featureB/enable
```

**disable a boolean flag**
```
http POST localhost:8888/flags/featureB/disable
```

**delete a flag**
```
http DELETE localhost:8888/flags/featureA
```