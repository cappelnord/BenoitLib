tell application "Screen Sharing"
	set allWindows to (every window where visible is true)
	set numWindows to count of allWindows
	if count < 1 then
		do shell script "echo There is no open Window!"
	else
		set i to 0
		repeat
			tell window ((i mod numWindows) + 1)
				activate
			end tell
			do shell script "/bin/sleep 10"
			set i to i + 1
		end repeat
	end if
end tell