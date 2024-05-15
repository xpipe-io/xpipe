# XPipe Vault (Keep this repository private!)

⚠️ No connections have been pushed to this git repository.
The push was successful but no connections were added.

## Troubleshooting

### Adding categories to the repository

To have your connections of a category put inside your git repository,
you need to click on the `⚙️` icon (when hovering over the category)
in your `Connections` tab under the category overview on the left side.
Then click on `Add to git repository`, to sync the category and connections to your git repository.

### Local connections are not synced

Any connection under the local machine can not be shared as it refers to connections and data that are only available on a specific system.

Certain file-based connections, for example SSH configs, can be shared via git if the underlying data, in this case the file, have been added to the git repository as well in the `data` directory.

### Other issues

If you encounter any other issues, you can try interacting with the cloned repository manually.
You can find it at `%%USERPROFILE%%\.xpipe\storage\` or `~/.xpipe/storage/`.
XPipe will call your installed git client, so any potential issues with your local git client also transfer to XPipe.

To understand what went wrong, you can also launch XPipe in debug mode at `Settings -> Troubleshoot -> Launch in debug mode`.
This will tell you in detail what git commands are executed.
