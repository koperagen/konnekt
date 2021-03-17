## Description

Konnekt - attempt to reimplement Retrofit as a compiler plugin. Playground for investigating meta-programming & tooling integration capabilities of Kotlin compiler & Arrow Meta.

## Tasks

- [x] Define, refine the HTTP request model
- [x] Parse request model from DSL
- [x] Provide compiler messages about erroneous models
- [x] Diagnostics and quick fixes to restrict DSL usages
- [x] Generate sources for limitet subset of annotations (basic GET, DELETE, POST etc)
- [ ] Gradle plugin to apply compiler plugin to project
- [ ] Resolve annotation & types via Typed Quotes
- [ ] Implement FormUrlEncoded & Multipart
- [ ] Register IDE Plugins via MetaIde
- [ ] Investigate IR capabilities
- [ ] Samples & docs