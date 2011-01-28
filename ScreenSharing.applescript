tell application "Screen Sharing"
	set allWindows to (every window where visible is true)
	set numWindows to count of allWindows
	activate
	if numWindows < 1 then
		do shell script "echo There is no open Window!"
	else
		set i to 0
		repeat
			set curWindow to item ((i mod (numWindows)) + 1) of allWindows
			set visible of curWindow to true
			delay 10
			set visible of curWindow to false
			set i to i + 1
		end repeat
	end if
end tell