on run argv
tell application "GrowlHelperApp"

 notify with name "Shout" title item 1 of argv description item 2 of argv application name "MandelClock" 

end tell
end run