# Scalafix rules for dataclass

To develop rule:
```
sbt ~tests/test
# edit rules/src/main/scala/fix/GenerateDataClass.scala
```

Use as a source generator:

https://github.com/olafurpg/scalafix-codegen

Apply [scalafmt](https://scalameta.org/scalafmt/) to make it somewhat readable.