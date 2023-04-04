#!/usr/bin/env bash

release_url() {
  echo "https://github.com/xpipe-io/xpipe/releases/latest/download"
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
  esac
}

download_release_from_repo() {
  local os_info="$1"
  local tmpdir="$2"
  local ending=$(get_file_ending)

  local filename="xpipe-installer-$os_info-x86_64.$ending"
  local download_file="$tmpdir/$filename"
  local archive_url="$(release_url)/$filename"

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
  esac
  return 0
}

uninstall() {
  local uname_str="$(uname -s)"
  case "$uname_str" in
    Linux)
      if [ -f "/etc/debian_version" ]; then
        DEBIAN_FRONTEND=noninteractive sudo apt-get remove -qy xpipe
      else
        sudo rpm -e xpipe
      fi
      ;;
    Darwin)
      sudo /Applications/X-Pipe.app/Contents/Resources/scripts/uninstall.sh
      ;;
    *)
      exit 1
  esac
}

install() {
  local uname_str="$(uname -s)"
  local file="$1"

  case "$uname_str" in
    Linux)
      if [ -f "/etc/debian_version" ]; then
        DEBIAN_FRONTEND=noninteractive sudo apt-get install -qy "$file"
      else
        sudo rpm -i "$file"
      fi
      ;;
    Darwin)
      sudo installer -verboseR -allowUntrusted -pkg "$file" -target /
      ;;
    *)
      exit 1
  esac
}

launch() {
  xpipe open
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
  download_release_from_repo "$os_info" "$download_dir"
}

check_architecture() {
  local arch="$1"
  case "$arch" in
    x86_64)
      return 0
      ;;
    amd64)
      return 0
      ;;
    arm64)
      if [ "$(uname -s)" = "Darwin" ]; then
        return 0
      fi
      ;;
  esac

  error "Sorry! X-Pipe currently only provides pre-built binaries for x86_64 architectures."
  return 1
}


# return if sourced (for testing the functions above)
return 0 2>/dev/null

check_architecture "$(uname -m)" || exit 1

download_archive="$(download_release; exit "$?")"
exit_status="$?"
if [ "$exit_status" != 0 ]
then
  error "Could not download X-Pipe release."
  exit "$exit_status"
fi

uninstall
install "$download_archive"
launch
