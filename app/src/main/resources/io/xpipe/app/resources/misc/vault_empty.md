# XPipe Vault (Keep this repository private!)

It works! The git remote push succeeded. However, no connections have been pushed to this git repository yet.

## Adding connections to the repository

By default, no connection categories are set to sync so that you have explicit control on what connections to commit.

To have your connections of a category put inside your git repository, you first need to change its sync configuration.
In your `Connections` tab under the category overview on the left side, you can open the category configuration menu either by right-clicking the category or click on the `⚙️` icon when hovering over the category, and then clicking on the `🔧` configure button.

Then, set the `Sync with git repository` value to `Yes` to sync the category and connections to your git repository.
This will add all syncable connections in that category to the git repository.
The sync settings for a category are inherited by default from its parent if not explicitly set.

## Local connections are not synced

Any connections located under the local machine are not synced as they are only available on the local system. You can sync any other types of remote connections like SSH connections.

Some types of connection entries that are local by default, e.g. an SSH config file, can be configured to sync by syncing the underlying data, e.g. files, with this repository as well. You can find details for each type of connection entry at https://docs.xpipe.io/.
