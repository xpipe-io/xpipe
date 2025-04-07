test -f ~/.local/bin/oh-my-posh
if [ $? != 0 ]; then
  which brew >/dev/null 2>&1 && brew install jandedobbeleer/oh-my-posh/oh-my-posh || curl -s https://ohmyposh.dev/install.sh | bash -s;
fi
eval "$(~/.local/bin/oh-my-posh init $(~/.local/bin/oh-my-posh get shell))"
