# Execution types

You can use a script in multiple different scenarios.

When enabling a script, the execution types dictate what XPipe will do with the script.

## Init scripts

When a script is designated as init script, it can be selected in shell environments.

Furthermore, if a script is enabled, it will automatically be run on init in all compatible shells.

For example, if you create a simple init script like
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
you will have access to these aliases in all compatible shell sessions if the script is enabled.

## Shell scripts

A normal shell script is intended to be called in a shell session in your terminal.
When enabled, the script will be copied to the target system and put into the PATH in all compatible shells.
This allows you to call the script from anywhere in a terminal session.
The script name will be lowercased and spaces will be replaced with underscores, allowing you to easily call the script.

For example, if you create a simple shell script named `apti` like
```
sudo apt install "$1"
```
you can call that on any compatible system with `apti.sh <pkg>` if the script is enabled.

## File scripts

Lastly, you can also run custom script with file inputs from the file browser interface.
When a file script is enabled, it will show up in the file browser to be run with file inputs.

For example, if you create a simple file script like
```
sudo apt install "$@"
```
you can run the script on selected files if the script is enabled.

## Multiple types

As the sample file script is the same as the sample shell script above,
you see that you can also tick multiple boxes for execution types of a script if they should be used in multiple scenarios.


