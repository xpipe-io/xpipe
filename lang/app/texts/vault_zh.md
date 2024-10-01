# XPipe Git Vault

XPipe可以将所有连接数据与您自己的git远程仓库同步。您可以在所有 XPipe 应用程序实例中以相同的方式与该版本库同步，您在一个实例中所做的每一项更改都将反映在版本库中。

首先，您需要使用自己喜欢的 git 提供商创建一个远程仓库。该仓库必须是私有的。
然后，您只需将 URL 复制并粘贴到 XPipe 远程仓库设置中即可。

您还需要在本地计算机上安装 `git` 客户端。您可以尝试在本地终端运行 `git` 进行检查。
如果没有，可以访问 [https://git-scm.com](https://git-scm.com/)安装 git。

## 验证远程仓库

有多种认证方式。大多数版本库使用 HTTPS，需要指定用户名和密码。
有些提供商还支持 SSH 协议，XPipe 也支持该协议。
如果您在 git 中使用 SSH，可能已经知道如何配置，因此本节将只介绍 HTTPS。

您需要设置 git CLI，以便能通过 HTTPS 与远程 git 仓库进行身份验证。有多种方法可以做到这一点。
您可以在配置好远程仓库后重启 XPipe 来检查是否已经完成。
如果系统要求您提供登录凭证，您就需要进行设置。

许多特殊工具，如 [GitHub CLI](https://cli.github.com/)，在安装后会自动完成所有操作。
一些较新的 Git 客户端版本还能通过特殊的网络服务进行身份验证，只需在浏览器中登录账户即可。

也有通过用户名和令牌手动认证的方法。
如今，大多数服务提供商都要求使用个人访问令牌（PAT），而不是传统的密码来进行命令行身份验证。
您可以在这里找到常用的 (PAT) 页面：
- **GitHub**：[个人访问令牌（经典）](https://github.com/settings/tokens)
- **GitLab**：[个人访问令牌](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket**：[个人访问令牌](https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea**：`设置 -> 应用程序 -> 管理访问令牌部分`。
将存储库的令牌权限设置为 "读取 "和 "写入"。其他令牌权限可设置为 "读取"。
即使 git 客户端提示您输入密码，您也应该输入令牌，除非您的提供商仍然使用密码。
- 大多数提供商已经不支持密码了。

如果不想每次都输入凭据，可以使用任何 git 凭据管理器。
更多信息，请参阅
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

一些现代的 git 客户端也会自动存储凭据。

如果一切正常，XPipe 就会向您的远程仓库推送提交。

## 为版本库添加类别

默认情况下，不会将连接类别设置为同步，这样您就可以明确控制要提交的连接。
因此，一开始，您的远程版本库将是空的。

要将某个类别的连接放到您的 git 仓库中，您需要点击齿轮图标、
需要点击齿轮图标（悬停在类别上时）
点击左侧类别概览下的 `Connections` 标签中的齿轮图标。
然后点击`添加到 git 仓库`，将类别和连接同步到 git 仓库。
这将把所有可同步的连接添加到 git 仓库。

##本地连接不会同步

任何位于本地机器下的连接都不能共享，因为它涉及的连接和数据只在本地系统上可用。

某些基于本地文件的连接（例如 SSH 配置）可以通过 git 共享，前提是底层数据（这里指文件）也已添加到 git 仓库中。

## 添加文件到 git

一切就绪后，您还可以向 git 添加 SSH 密钥等其他文件。
在每个文件选项旁都有一个 git 按钮，可以将文件添加到 git 仓库。
这些文件在推送时也会加密。
