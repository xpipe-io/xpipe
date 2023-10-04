dir=~/.xpipe/scriptdata/starship
export PATH="$PATH:$dir"
which starship > /dev/null
if [ "$?" != 0 ]; then
    mkdir -p "$dir"
    sh <(curl -sS https://starship.rs/install.sh) -y --bin-dir "$dir" > /dev/null
fi
eval "$(starship init bash)"
