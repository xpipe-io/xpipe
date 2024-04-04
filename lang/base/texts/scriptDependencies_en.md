## Script dependencies

The scripts and script groups to run first. If an entire group is made a dependency, all scripts in this group will be considered as dependencies.

The resolved dependency graph of scripts is flattened, filtered, and made unique. I.e. only compatible scripts will be run and if a script would be executed multiple times, it will only be run the first time.
