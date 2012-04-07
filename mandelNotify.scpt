on run argv
	tell application "System Events"
		set isRunning to ¬
			(count of (every process whose bundle identifier is "com.Growl.GrowlHelperApp")) > 0
	end tell
	
	if isRunning then
		tell application id "com.Growl.GrowlHelperApp"
			notify with name ¬
				"Shout" title ¬
				item 1 of argv description ¬
				item 2 of argv application name ¬
				"MandelClock"
		end tell
	end if
end run