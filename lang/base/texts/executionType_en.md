# Execution types

You can use a script in multiple different scenarios.

When enabling a script via its enable toggle button, the execution types dictate what XPipe will do with the script.

## Init script type

When a script is designated as init script, it can be selected in shell environments.

Furthermore, if a script is enabled, it will automatically be run on init in all compatible shells.

For example, if you create a simple init script like
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
you will have access to these aliases in all compatible shell sessions if the script is enabled.

## Session script type

A shell session script is intended to be called in a shell session in your terminal.
When enabled, the script will be copied to the target system and put into the PATH in all compatible shells.
This allows you to call the script from anywhere in a terminal session.
The script name will be lowercased and spaces will be replaced with underscores, allowing you to easily call the script.

For example, if you create a simple shell script named `apti` with
```
sudo apt install "$1"
```
you can call the script on any compatible system with `apti.sh <pkg>` if the script is enabled.

## File script type

Lastly, you can also run custom script with file inputs from the file browser interface.
When a file script is enabled, it will show up in the file browser to be run with file inputs.

For example, if you create a simple file script like
```
diff "$1" "$2"
```
you can run the script on selected files if the script is enabled.
In this example, the script will only run successfully if you have exactly two files selected.
Otherwise, the diff command will fail.

## Multiple types

You can also tick multiple boxes for execution types of a script if they should be used in multiple scenarios.

For example in many cases, you can use file scripts also as normal shell scripts
to bring into your shell sessions to call them with specific arguments manually.


