# XPipe Git Vault

XPipe can synchronize all your connection data with your own git remote repository. You can sync with this repository in all XPipe application instances the same way, every change you make in one instance will be reflected in the repository.

First of all, you need to create a remote repository with your favourite git provider of choice. This repository has to be private.
You can then just copy and paste the URL into the XPipe remote repository setting.

You also need to have a locally installed `git` client ready on your local machine. You can try running `git` in a local terminal to check.
If you don't have one, you can visit [https://git-scm.com](https://git-scm.com/) to install git.

## Authenticating to the remote repository

There are multiple ways to authenticate. Most repositories use HTTPS where you have to specify a username and password.
Some providers also support the SSH protocol, which is also supported by XPipe.
If you use SSH for git, you probably know how to configure it, so this section will cover HTTPS only.

You need to set up your git CLI to be able to authenticate with your remote git repository via HTTPS. There are multiple ways to do that.
You can check if that is already done by restarting XPipe once a remote repository is configured.
If it asks you for your login credentials, you need to set it up.

Many special tools like this [GitHub CLI](https://cli.github.com/) do everything automatically for you when installed.
Some newer git client versions can also authenticate via special web services where you just have to log in into your account in your browser.

There are also manual ways to authenticate via a username and token.
Nowadays, most providers require a personal access token (PAT) to authenticate from the command-line instead of traditional passwords.
You can find common (PAT) pages here:
- **GitHub**: [Personal access tokens (classic)](https://github.com/settings/tokens)
- **GitLab**: [Personal access token](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket**: [Personal access token](https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea**: `Settings -> Applications -> Manage Access Tokens section`
Set the token permission for repository to Read and Write. The rest of the token permissions can be set as Read.
Even if your git client prompts you for a password, you should enter your token unless your provider still uses passwords.
- Most providers do not support passwords anymore.

If you don't want to enter your credentials every time, you can use any git credentials manager for that.
For more information, see for example:
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

Some modern git clients also take care of storing credentials automatically.

If everything works out, XPipe should push a commit to your remote repository.

## Adding categories to the repository

By default, no connection categories are set to sync so that you have explicit control on what connections to commit.
So at the start, your remote repository will be empty.

To have your connections of a category put inside your git repository,
you need to click on the gear icon (when hovering over the category)
in your `Connections` tab under the category overview on the left side.
Then click on `Add to git repository` to sync the category and connections to your git repository.
This will add all syncable connections to the git repository.

## Local connections are not synced

Any connection located under the local machine can not be shared as it refers to connections and data that are only available on the local system.

Certain connections that are based on a local file, for example SSH configs, can be shared via git if the underlying data, in this case the file, have been added to the git repository as well.

## Adding files to git

When everything is set up, you have the option to add any additional files such as SSH keys to git as well.
Next to every file choice is a git button that will add the file to the git repository.
These files are also encrypted when pushed.
