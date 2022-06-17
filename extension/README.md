[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.xpipe/extension/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.xpipe/extension)
[![javadoc](https://javadoc.io/badge2/io.xpipe/extension/javadoc.svg)](https://javadoc.io/doc/io.xpipe/extension)
[![Build Status](https://github.com/xpipe-io/xpipe_java/actions/workflows/extension.yml/badge.svg)](https://github.com/xpipe-io/xpipe_java/actions/workflows/extension.yml)

## X-Pipe Extension API

The X-Pipe extension API allows you to create extensions of any kind for X-Pipe.
This includes:
- Custom data stores, including configuration GUI and CLI
- Custom data sources, including configuration GUI and CLI
- Custom preferences entries

### Custom data sources

A custom data source type can be implemented by creating a custom
[DataSourceProvider](src/main/java/io/xpipe/extension/DataSourceProvider.java).
This provider contains all the information required for proper handling of your custom data sources,
whether you access it from the CLI, any API, or the X-Pipe commander gui.
