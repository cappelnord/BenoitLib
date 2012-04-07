
(defun sclang-mhub-command (command)
  "Ask for input and perform command on the current MandelHub instance"
  (let ((string (read-string (concat command ": ") 
                             nil 'sclang-mhub-chat-history " ")))
    (sclang-eval-expression (concat "MandelHub.instance." 
                                    command 
                                    "(\"" string "\")"))))
(defun sclang-mhub-shout ()
  "Send a MandelHub shout message"
  (interactive)
  (sclang-mhub-command "shout"))

(defun sclang-mclock-chat ()
  "Send a MandelHub chat message"
  (interactive)
  (sclang-mhub-command "chat"))
