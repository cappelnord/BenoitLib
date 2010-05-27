tell application "GrowlHelperApp"
 set the allNotificationsList to {"Shout"}
 set the enabledNotificationsList to {"Shout"}

 register as application "MandelClock" all notifications allNotificationsList default notifications enabledNotificationsList icon of application "SuperCollider"
end tell