# Devsync 

Image for syncing multiple folders (i.e., volumes/mounts) into one.

Mount all folders you want to watch for changes into `/srcData`. 

Mount the destination into `/destData`

The *contents* of the mounted directories are synced, not the directories themselves.

if the environment variable `DAEMON_MODE` is true, runs continually - warning: this is slow with bind mounts on windows

Example: 

* /tmp/folder1/ mounted into /srcData/f1
* /tmp/folder2/ mounted into /srcData/f2
* /tmp/dest mounted into /destData

Creating file `foo.txt` as `/tmp/folder1/foo.txt` leads to `/tmp/dest/foo.txt` appearing.



