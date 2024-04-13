## Temporary containers

This will run a temporary container using the specified image that will get automatically removed once it is stopped. The container will keep running even if the container image does not have any command specified that will run.

This can be useful if you quickly want to set up a certain environment by using a certain container image. You can then enter the container as normal in XPipe, perform your operations, and stop the container once it's no longer needed. It is then removed automatically.