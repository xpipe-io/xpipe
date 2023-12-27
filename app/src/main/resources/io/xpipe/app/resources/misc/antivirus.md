### Information about antivirus programs

XPipe detected that you are running %s. As a result, you might run into a few problems due to %s detecting suspicious activity when XPipe is interacting with your shell programs. This can range from severy slowdowns, notifications, up to full isolation of XPipe and your shell programs, effectively making the application unusable and causing errors.

%s

### Threat analysis

All artifacts of every release are automatically uploaded and analyzed on [VirusTotal](https://virustotal.com), so uploading the release you downloaded to VirusTotal should instantly show complete analysis results. From there you should be able to get a more accurate overview over the threat level of XPipe to you.
You can also find the analysis results listed at the bottom of every release on GitHub, i.e. at [https://github.com/xpipe-io/xpipe/releases/tag/%s](https://github.com/xpipe-io/xpipe/releases/tag/%s)

Furthermore, you can find detailed information about the security model of XPipe at [https://docs.xpipe.io/security](https://docs.xpipe.io/security). From there you should be able to get a more accurate overview over the threat level of XPipe to you if you are feeling uneasy about turning any protection off.

### What you can do

If such a protection kicks in or false-positive happens on your end, you might have to explicitly whitelist XPipe or disable certain protections in order for it to work correctly. Accessing shells is necessary for XPipe, there is no fallback alternative built in that does not launch shells.

If you choose to continue from here, XPipe will start calling shell programs to properly initialize, which might provoke %s.