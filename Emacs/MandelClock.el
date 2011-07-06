
(defun sclang-mclock-command (command)
  "Ask for input and perform command on the current MandelClock instance"
  (let ((string (read-string (concat command ": ") 
                             nil 'sclang-mclock-chat-history " ")))
    (sclang-eval-expression (concat "MandelClock.instance." 
                                    command 
                                    "(\"" string "\")"))))
(defun sclang-mclock-shout ()
  "Send a MandelClock shout message"
  (interactive)
  (sclang-mclock-command "shout"))

(defun sclang-mclock-chat ()
  "Send a MandelClock chat message"
  (interactive)
  (sclang-mclock-command "chat"))
