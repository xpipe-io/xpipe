#!/usr/bin/env bash

release_url() {
  local repo="$1"
  local version="$2"
  if [[ -z "$version" ]] ; then
    echo "$repo/releases/latest/download"
  else
    echo "$repo/releases/download/$version"
  fi
}

get_file_ending() {
  local uname_str="$(uname -s)"
  case "$uname_str" in
  Linux)
    if [ -f "/etc/debian_version" ]; then
      echo "deb"
    else
      echo "rpm"
    fi
    ;;
  Darwin)
    echo "pkg"
    ;;
  *)
    exit 1
    ;;
  esac
}

download_release_from_repo() {
  local os_info="$1"
  local tmpdir="$2"
  local repo="$3"
  local version="$4"
  local arch="$5"

  local ending=$(get_file_ending)
  local release_url=$(release_url "$repo" "$version")

  local filename="xpipe-installer-$os_info-$arch.$ending"
  local download_file="$tmpdir/$filename"
  local archive_url="$release_url/$filename"

  info "Downloading file $archive_url"
  curl --progress-bar --show-error --location --fail "$archive_url" --output "$download_file" --write-out "$download_file"
}

info() {
  local action="$1"
  local details="$2"
  command printf '\033[1;32m%12s\033[0m %s\n' "$action" "$details" 1>&2
}

error() {
  command printf '\033[1;31mError\033[0m: %s\n\n' "$1" 1>&2
}

warning() {
  command printf '\033[1;33mWarning\033[0m: %s\n\n' "$1" 1>&2
}

request() {
  command printf '\033[1m%s\033[0m\n' "$1" 1>&2
}

eprintf() {
  command printf '%s\n' "$1" 1>&2
}

bold() {
  command printf '\033[1m%s\033[0m' "$1"
}

# returns the os name to be used in the packaged release
parse_os_name() {
  local uname_str="$1"
  local arch="$(uname -m)"

  case "$uname_str" in
  Linux)
    echo "linux"
    ;;
  FreeBSD)
    echo "linux"
    ;;
  Darwin)
    echo "macos"
    ;;
  *)
    return 1
    ;;
  esac
  return 0
}

install() {
  local uname_str="$(uname -s)"
  local file="$1"

  case "$uname_str" in
  Linux)
    if [ -f "/etc/debian_version" ]; then
      info "Installing file $file with apt"
      sudo apt update
      DEBIAN_FRONTEND=noninteractive sudo apt install "$file"
    elif [ -x "$(command -v zypper)" ]; then
      info "Installing file $file with zypper"
      sudo zypper install --no-gpg-checks "$file"
    elif [ -x "$(command -v dnf)" ]; then
      info "Installing file $file with dnf"
      sudo rpm --import https://xpipe.io/signatures/crschnick.asc
      sudo dnf install --refresh "$file"
    elif [ -x "$(command -v yum)" ]; then
      info "Installing file $file with yum"
      sudo rpm --import https://xpipe.io/signatures/crschnick.asc
      sudo yum clean expire-cache
      sudo yum install "$file"
    else
      info "Installing file $file with rpm"
      sudo rpm --import https://xpipe.io/signatures/crschnick.asc
      sudo rpm -U -v --force "$file"
    fi
    ;;
  Darwin)
    sudo installer -verboseR -pkg "$file" -target /
    ;;
  *)
    exit 1
    ;;
  esac
}

launch() {
  "$kebap_product_name" open
}

download_release() {
  local uname_str="$(uname -s)"
  local os_info
  os_info="$(parse_os_name "$uname_str")"
  if [ "$?" != 0 ]; then
    error "The current operating system ($uname_str) does not appear to be supported."
    return 1
  fi

  # store the downloaded archive in a temporary directory
  local download_dir="$(mktemp -d)"
  local repo="$1"
  local version="$2"
  download_release_from_repo "$os_info" "$download_dir" "$repo" "$version" "$arch"
}

check_architecture() {
  local arch="$(uname -m)"
  case "$arch" in
  x86_64)
    echo x86_64
    ;;
  amd64)
    echo x86_64
    ;;
  arm64)
    echo arm64
    ;;
  aarch64)
    echo arm64
    ;;
  *)
    exit 1
    ;;
  esac
}

# return if sourced (for testing the functions above)
return 0 2>/dev/null

arch=$(check_architecture)
exit_status="$?"
if [ "$exit_status" != 0 ]; then
  error "Sorry! $product_name currently does not support your processor architecture."
  exit "$exit_status"
fi

repo="https://github.com/xpipe-io/xpipe"
aur="https://aur.archlinux.org/xpipe.git"
product_name="XPipe"
kebap_product_name="xpipe"
version=
while getopts 'sv:' OPTION; do
  case "$OPTION" in
    s)
      repo="https://github.com/xpipe-io/xpipe-ptb"
      aur="https://aur.archlinux.org/xpipe-ptb.git"
      product_name="XPipe PTB"
      kebap_product_name="xpipe-ptb"
      ;;

    v)
      version="$OPTARG"
      ;;

    ?)
      echo "Usage: $(basename $0) [-s] [-v <version>]"
      exit 1
      ;;
  esac
done

if [ "$(uname -s)" = "Linux" ]; then
  if ! [ -x "$(command -v apt)" ] && ! [ -x "$(command -v rpm)" ] && [ -x "$(command -v pacman)" ]; then
    info "Installing from AUR at $aur"
    rm -rf "/tmp/xpipe_aur" || true
    if [[ -z "$version" ]] ; then
      git clone "$aur" /tmp/xpipe_aur
    else
      git clone --branch "$version" "$aur" /tmp/xpipe_aur
    fi
    cd "/tmp/xpipe_aur"
    makepkg -si
    launch
    exit 0
  fi

  if ! [ -x "$(command -v apt)" ] && ! [ -x "$(command -v rpm)" ] && ! [ -x "$(command -v pacman)" ]; then
    info "Installation is not supported on this system (no apt, rpm, zypper, dnf, yum, pacman). Can you try a portable version of $product_name?"
    info "https://github.com/xpipe-io/xpipe#portable"
    exit 1
  fi
fi

download_archive="$(
  download_release "$repo" "$version" "$arch"
  exit "$?"
)"
exit_status="$?"
if [ "$exit_status" != 0 ]; then
  error "Could not download $product_name release."
  exit "$exit_status"
fi

install "$download_archive"

exit_status="$?"
if [ "$exit_status" != 0 ]; then
  error "Installation failed."
  exit "$exit_status"
fi

echo ""
echo "$product_name was successfully installed. You should be able to find $product_name in your desktop environment now."

launch
