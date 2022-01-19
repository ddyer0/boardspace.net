;; Red Hat Linux default .emacs initialization file

;; Set up the keyboard so the delete key on both the regular keyboard
;; and the keypad delete the character under the cursor and to the right
;; under X, instead of the default, backspace behavior.
(setq tt keyboard-translate-table)
(setq keyboard-translate-table nil)
;(global-set-key [delete] 'backward-delete-char-untabify)
;(glob-set-key [kp-delete] 'delete-backward-char)


;; Require a final newline in a file, to avoid confusing some tools

(setq require-final-newline t)
