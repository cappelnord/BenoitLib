tell application "System Events"
	set isRunning to Â
		(count of (every process whose bundle identifier is "com.Growl.GrowlHelperApp")) > 0
end tell

if isRunning then
	tell application id "com.Growl.GrowlHelperApp"
		set the allNotificationsList to {"Shout"}
		set the enabledNotificationsList to {"Shout"}
		register as application Â
			"MandelHub" all notifications allNotificationsList Â
			default notifications enabledNotificationsList Â
			icon of application "SuperCollider"
	end tell
end if