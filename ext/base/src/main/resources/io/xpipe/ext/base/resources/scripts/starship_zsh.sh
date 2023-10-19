dir=~/.xpipe/scriptdata/starship
export PATH="$PATH:$dir"
which starship > /dev/null 2>&1
if [ "$?" != 0 ]; then
    mkdir -p "$dir" && \
    which curl > /dev/null && \
    curl -sS https://starship.rs/install.sh | sh /dev/stdin -y --bin-dir "$dir" > /dev/null
fi
eval "$(starship init zsh)"