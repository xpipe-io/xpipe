## VNC target system

In addition to normal VNC features, XPipe also adds additional features through interaction with the system shell of the target system.

In a few cases the VNC server host, i.e. the remote system where the VNC server runs on, might be different from the actual system you are controlling with VNC. For example, if a VNC server is handled by a VM hypervisor like Proxmox, the server runs on the hypervisor host while the actual target system you are controlling, for example a VM, is the VM guest. In order to make sure that for example file system operations are applied on the correct system, you can manually change the target system if it differs from the VNC server host.