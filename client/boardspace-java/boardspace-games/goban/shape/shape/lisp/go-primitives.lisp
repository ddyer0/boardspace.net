;; -*- Mode: LISP; Package: GO; Base: 10.; Syntax: Common-lisp -*-

D,#TD1PsT[Begin using 006 escapes](1 0 (NIL 0) (NIL :BOLD :LARGE) "MEDFNTB");;
;; some basic constants
;;

0;elements of the environment
(defvar (2 0 (NIL 0) (NIL :BOLD NIL) "CPTFONTCB")all-games0 nil)
(defvar 2*game-being-loaded*0 nil)
(defvar 2*empty*0)
(defvar 2*game*0)
(defvar 2*in-search*0 nil) ;bound while in searches, to inhibit long-term descision making
(defvar 2*go-cons-area* 
0	(make-area :name '*go-cons-area* 
		   :capacity-ratio 1.0
		   :gc :ephemeral
		   :n-levels 4
		   :room t))

(defvar 2*go-static-area*
0	(make-area :name '*go-static-area*
		   :gc :dynamic
		   :room t))

(defmacro2 defvar-value0 (a value)
  `(progn
     (defvar ,a)
     (eval-when (load eval)
       (setq ,a ,value))))


(defvar-value 2*game-directory-cache*
0	      (make-instance 'color:directory-cache
			     :file-types '(:go :bin :wo :we :prv :go-scribe :go-standard
					       :nemesis :dumb-go :mgt)))

1;;
;; Property List Mixin
;;

0(defflavor 2property-list-mixin0 ((properties nil)) ()
  (:conc-name nil)
  :writable-instance-variables
  :initable-instance-variables)

(defmethod (2get-prop0 property-list-mixin) (name)
  (loop for (prop-name val) on properties by #'cddr
	    when (eql name prop-name) return (values val t)))

(defmethod (2(setf get-prop)0 property-list-mixin) (id val)
  (setf (getf properties id) val))

(defmethod (2put-prop0 property-list-mixin) (val id)
  (3 0 (NIL 0) (NIL :ITALIC NIL) "CPTFONTI");keep this despite the unstylishness, because it's more convenient for
0  3;reversible-change-mixin
0  (setf (getf properties id) val))

(defmethod (2rem-prop0 property-list-mixin) (id)
  (remf properties id))

(defmethod (2rem-props0 property-list-mixin) (props)
  ;remove a short list of properties in one whack
  (loop with prev = nil
	as this = properties then next
	while this
	as (pn . pv) = this
	as next = (cdr pv)
	do
	(if (member pn props)

	    (when prev (setf (cdr prev) next))

	    (unless prev
	      (setq properties this))
	    (setq prev pv)
	    )))

(defmethod (2strip-properties0 property-list-mixin) (props)
  (tree-walk-down self
		  #'(lambda (n) 
		      (remove-stripable-properties n props))))

(defmethod (2remove-stripable-properties0 property-list-mixin) (props)
  (let* ((s (successors self))
	 (p (predecessors self))
	 (p-s (when p (successors (first p)))))
    (unless (or (null s)  ;terminal node
		(null p)  ;top node
		(cdr p)   ;join node
		(cdr p-s) ;branch node
		(cdr s)   ;parent node
		)
      (loop for prop in props 
	    as v = (get-prop self prop) 
	      when v do
		(rem-prop self prop)))))

(defmethod (2get-stripped-property0 property-list-mixin) (name)
  3;stripped properties exist on nodes with multiple successors,
0  3;their successros, and on terminal nodes.
0  (or (get-prop self name)
      (let ((s (first-successor self)))
	(cond ((null s) nil) 3;terminal node, should have had the property here
0	      ((alternative-successors s) 3;multiple-children; should have had the property here
0	       nil)  
	      (t (get-stripped-property s name))
	      ))))

(defmethod (2unify-property0 property-list-mixin) 
	   (key &key 
		(from-nodes (successors self))
		(test #'eql))
  (when from-nodes
    (loop with val = nil				
	  for snode in from-nodes
	  as clas = (get-prop snode key)
	  do (setq val (union (if (listp clas) clas (list clas))
			      val
			      :test test))
	  finally (when val
		    (let ((old (get-prop self key)))
		      (unless (sets-equal (if (listp old) old (list old))
					val
					:test test)
			;(format t "~&~S: old: ~S new ~S" self old val)
			(put-prop self val key))))))
  (let ((val (get-prop self key)))
    (when (and (consp val)(null (cdr val)))
      (put-prop self (first val) key)))
  )

(defmethod (2unify-property-and-count0 property-list-mixin) 
	   (key &key 
		(from-nodes (successors self)))
  (cond ((null from-nodes)
	 )
	((null (cdr from-nodes))
	 (setf (get-prop self key)
	       (get-prop (first from-nodes) key))
	 )
	((cdr from-nodes)			;more than one
	 (loop with val = nil				
	       for snode in from-nodes
	       as class = (get-prop snode key)
	       do (cond ((null class))
			((nlistp class) (setq class (list class 1))))
		  (loop for (n v) on class by #'cddr
			do (incf (getf val n 0) 
				 v))
	       finally (when val (put-prop self val key))))))
  

(defmethod (2propagate-property-upward0 property-list-mixin) (property &key (test #'eql))
  (tree-walk-down 
    self
    #'(lambda (node)
	(unify-property node property :test test)
	)
    :call-fn-before nil)
  (strip-properties self `(,property))
  )


(defmethod (2propagate-property-upward-and-count0 property-list-mixin) 
	   (property )
  (tree-walk-down 
    self
    #'(lambda (node)
	(unify-property-and-count node property)
	)
    :call-fn-before nil)
  (strip-properties self `(,property))
  )

3;;
;; Come 'de revolution, convert this to use clos class variables
;;
0(defun 2interesting-property-names-of-flavor0 (flav &optional key)
  (let ((val (tv:get-flavor-property flav ':property-list-methods)))
    (if (null key)
	val
	(or (tv:get-flavor-property flav ':interesting-property-list-methods)
	    (tv:set-flavor-property
	      flav
	      (loop for (prop prop-key) in val
		    when (eq prop-key key)
		      collect prop)
	      :interesting-property-list-methods)))))

(defmethod (2interesting-property-names0 property-list-mixin)
	   (&optional (key t) (flav (type-of self)))
  (interesting-property-names-of-flavor flav key))


(defmethod (2remove-uninteresting-properties0 property-list-mixin) ()
  (let ((int (interesting-property-names self)))
    (setq properties
	  (loop for (p n ) on properties by #'cddr
		when (member p int) nconc (list p n)))))

(defun 2make-instance-printable 0(item)
  (cond ((typep item 'fs:pathname)
	 (string item))
	(t (make-instance 'tv:alias-printer
		 :actual item
		 :representation (make-init-list item :printable t)
		 :properties '(:prefix "#.")))))

(defun 2printable-interesting-properties0 (who printable additional-properties)
  (let ((props (interesting-properties  who :additional-properties additional-properties)))
    (if printable
	(labels ((make-printable
		   (item)
		   (cond ((consp item)
			  (loop for i in item collect (make-printable i)))
			 ((instancep item)
			  (make-instance-printable item))
			 (t item))))
	  (loop for (name prop) on props by #'cddr
		collect name
		collect (make-printable prop)))
	props)))

(defmethod (2interesting-properties0 property-list-mixin)
	   (&key (key t) (flavor (type-of self)) additional-properties)
  (loop for i in (append additional-properties
			 (get-prop self :interesting-properties)
			 (interesting-property-names self nil flavor))
	as k = (if (consp i) (and (eql (second i) key) (car i)) i)
	as j = (setq j (getf properties k))
	when (and k j)
	nconc `(,k ,j)))

(defun 2add-property-method0 (id flav key &aux pr p)
  (setq pr (tv:get-flavor-property flav ':property-list-methods))
  (cond ((setq p (assoc id pr))
	 (rplaca (cdr p) key))
	(t (setq pr (cons-in-area (list-in-area flavor::*flavor-static-area* id key)
				  pr flavor::*flavor-static-area*))))
  (tv:set-flavor-property flav nil ':interesting-property-list-methods)
  (tv:set-flavor-property flav pr ':property-list-methods))


(defmacro 2def-prop-method0 (id flavor &optional interesting-p)
  (let ((keyword-id (intern (string id) (symbol-package ':foo))))
    `(progn 'compile
	  (add-property-method ',keyword-id ',flavor ',interesting-p)
	  (defmethod (,id ,flavor)
		     ()
	    (get-prop self ',keyword-id ))
	  (defmethod (,(intern (string-append "SET-" id) (symbol-package id)) ,flavor)
		     (new)
	    (setf (get-prop self ',keyword-id) new))

	  (defmethod ((setf ,id) ,flavor) (new)
	    (setf (get-prop self ',keyword-id) new))

	  )))

1;;
;; A basic error flavor
;;

0(defflavor 2go-error0 () (zl:ferror)
  :initable-instance-variables
  )

(defmacro 2signal-error0 ((&rest format-args)
			   &body proceed-clauses)
  `(signal-proceed-case (() 'go-error
			 :format-string (or ,(first format-args) "Internal Go error")
			 :format-args (list ,@(cdr format-args)))
     ,@proceed-clauses
     ))

(defwhopper (:2bug-report-description0 go-error) (stream &rest args)
  ;(describe-state game stream)
  (lexpr-continue-whopper stream args))

(defmethod (2sys:proceed0 go-error 2:repair0) ()
  "Try to repair the damage, and proceed"
  (values :repair))

(defmethod (2sys:proceed0 go-error 2:ignore0) ()
  "Ignore the error, and continue"
  (values :ignore))

(defmethod (2sys:proceed0 go-error 2:skip0) ()
  "Skip this operation, and continue"
  (values :skip))

(defmethod (2sys:proceed0 go-error 2:first-alternative0) ()
  "Use the first alternative"
  (values :first-alternative))

(defmethod (2sys:proceed0 go-error 2:second-alternative0) ()
  "Use the second alternative"
  (values :second-alternative))

(compile-flavor-methods go-error)

1;;
;; Random lisp functions
;;
0(defun 2sort-on-third-item0 (a b)
  (> (third a) (third b)))
(defun 2sort-up-on-third-item0 (a b)
  (< (third a) (third b)))
(defun 2sort-on-second-item0 (a b)
  (> (second a) (second b)))
(defun 2sort-up-on-second-item0 (a b)
  (< (second a) (second b)))
(defun 2sort-on-first-item0 (a b)
  (> (first a) (first b)))
(defun 2sort-up-on-first-item0 (a b)
  (< (car a)(car b)))

(defun 2init-list-equal 0(a b)
  (cond ((consp a)
	 (and (consp b)
	      (init-list-equal (car a)(car b))
	      (init-list-equal (cdr a)(cdr b)))
	 )
	((symbolp a) (eql a b))
	((stringp a) (when (stringp b) (string-equal a b)))
	((numberp a) (when (numberp b) (= a b)))
	((arrayp a)
	 (and (arrayp b)
	      (let ((alen (array-total-size a))
		    (blen (array-total-size b)))
		(when (= alen blen)
		  (let ((a a)
			(b b))
		    (declare (sys:array-register a b))
		    (loop for i from 0 below alen
			  unless (init-list-equal (aref a i)(aref b i))
			    return nil
			  finally (return t))))))
	 )
	(t (signal-error ("Not expecting ~A" a)))
	))

(defun 2compare-property-lists
0       (form1 form2
	&key
	ignore-1-missing-props 
	ignore-2-missing-props
	ignore-props
	change-props
	(a-name "old")
	(b-name "new")
	(print-summary t)
	(test #'equal-enough)
	(print-details t))

  (declare (values difference not-in-1 not-in-2))

  (let* (not-in-1 not-in-2 dif
	 (form1 (copy-list form1))
	 (form2 (copy-list form2))
	 (p1 (getf form1 :properties))
	 (p2 (getf form2 :properties)))
    (remf form1 :properties)
    (remf form2 :properties)
    (when p1 (setq form1 (append p1 form1)))
    (when p2 (setq form2 (append p2 form2)))
    (loop for (rprop val1) on form1 by #'cddr
	  as prop = (or (second (assoc rprop change-props)) rprop)
	  as val2 = (getf form2 prop)
	  unless (or (member prop ignore-props)
		     (member prop ignore-2-missing-props))
	  do
      (cond ((funcall test val1 val2)
	     (remf form2 prop))
	    ((and (eql prop ':moves)
		  (null (compare-sequence-of-moves val1 val2 :test test))))
	    ((null val2) (push prop not-in-2))
	    (t ; (break "prop ~S ~S ~S" prop val1 val2)
	     (push prop dif))))
    (loop for (prop val2) on form2 by #'cddr
	  when (and val2
		    (not (member prop ignore-1-missing-props))
		    (not (member prop ignore-props))
		    (null (getf form1 prop)))
	    do (push prop not-in-1))

    (when (or print-details print-summary)
      (when not-in-1
	  (format t "~&2Properties missing in0 ~A: ~S~{, ~S~}" a-name
		  (first not-in-1)(cdr not-in-1)))
      (when not-in-2
	  (format t "~&2Properties missing in0 ~A: ~S~{, ~S~}" b-name
		  (first not-in-2)(cdr not-in-2)))
      (when dif
	(format t "~&2Properties different in ~A and ~A0: ~S~{, ~S~}2"
0		a-name b-name (first dif) (cdr dif))))

    (when print-details
      (loop for prop in dif
	    as val-1 = (getf form1 prop)
	    as val-2 = (getf form2 prop)
	    do
	(if (eql prop ':moves)
	    (print (compare-sequence-of-moves val-1 val-2))
	    (format t "~&~%~A ~S : ~S~&~A ~S: ~S~&"
		       a-name prop val-1 b-name prop val-2))))
    (values dif not-in-1 not-in-2)))


1;
; A mechanism for keeping track of adjustable parameters
;

0(defvar 2*go-parameters*0 (make-hash-table :locking nil :area *go-static-area*))

(eval-when (compile eval load)

(defvar-value 2*parameter-properties*0 
	      '(:min :max :documentation :type :resettable :compile-time-constant
		     :wide-search
		     :counter :locatable :debug)))

(defflavor 2go-parameter0 (name value flavors standard-value) (property-list-mixin)
  :initable-instance-variables
  :writable-instance-variables
  (:init-keywords . #.*parameter-properties*))

(defmethod (2sys:print-self0 go-parameter) (stream slash prin)
  (ignore slash prin)
  (sys:printing-random-object (self stream :no-pointer :typep)
    (format stream "~A" name)))

(defmethod (2:fasd-form0 go-parameter) ()
  `(make-parameter
     :name ',name :value ',value :flavor ',flavors
     ,@(loop for name in *parameter-properties*
	     as val = (get-prop self name)
	     when val
	       nconc `(,name ',val))))

(defmethod (2parameters-equal0 go-parameter) (to)
  (init-list-equal (send self :fasd-form) (send to :fasd-form)))

(defmethod (2:init0 go-parameter :after) (plist)
  (loop for pl on (cdr plist) by #'cddr
	as (id val) = pl
	when (member id *parameter-properties*)
	  do (put-prop self val id))
  (unless (variable-boundp standard-value) (setq standard-value value))
  (let ((old (gethash name *go-parameters*)))
    (when (and old (not (parameters-equal self old)))
      (format t "~&Redefining parameter from ~S to ~S" old self)))
  (setf (gethash name *go-parameters*) self)
  )

(defun 2make-parameter0 (&rest keywords)
  (cl:apply #'make-instance 'go-parameter keywords))

(defmacro 2def-go-parameter0 (name value flavors &rest keywords &key documentation min max type resettable compile-time-constant debug wide-search locatable counter)
  (ignore min max type documentation wide-search debug locatable)
  (setq flavors (if (listp flavors) (copy-list flavors) (list flavors)))
  (when type (unless (dw::presentation-type-name-p (if (listp type) (car type) type))
	       (compiler:warn "for ~S, ~S isn't a presentation type name"
			      name type)))
  `(progn
     ,@(cond ((member :global flavors)
	      `((,(cond (compile-time-constant 'defparameter)
			(resettable 'sys:defvar-resettable)
			(t 'defvar-value))
		 ,name ,value))))
     (eval-when (compile load eval)
       (make-parameter
	 :name ',name
	 :value ,value
	 :flavors ',flavors
	 ,@(when counter `(:counter ,counter))
	 ,@(loop for (key val) on keywords by #'cddr collect key collect `(quote ,val))
	 ))))

(defun 2parameter-value0 (a)
  (multiple-value-bind (val foundp)
      (gethash a *go-parameters*)
    (cond ((null foundp)
	   (signal-error ("No parameter named ~S exists" a)
	     (:ignore)))
	  ((member :global (go-parameter-flavors val))
	   (values (symbol-value a) val :global))
	  (t (values (go-parameter-value val) val)))))
	  
(defun 2(setf parameter-value)0 (a b)
  (declare (special trace-parameter-changes))
  (multiple-value-bind (val inst global-p)
      (parameter-value a)
    (when (cond ((eql t trace-parameter-changes))
		((consp trace-parameter-changes)
		 (member a trace-parameter-changes))
		((eql a trace-parameter-changes)))
      (format t "~&Change Parameter ~S from ~S to ~S"
	      a val b))
    (when global-p (setf (symbol-value a) b))
    (setf (go-parameter-value inst) b)
    (values b inst global-p)
    ))

  
(defflavor 2common-parameters-mixin0 ((%cached-parameter-names)
				    (%cached-parameter-timestamp -1)
				    (parameter-watch)
				    )
	   ()
  (:required-methods my-parameter-names))

(eval-when (compile eval load)
(defvar *parameter-timestamp* 0))

(defwhopper (2my-parameter-names0 common-parameters-mixin) ()
  (unless (eql %cached-parameter-timestamp *parameter-timestamp*)
    (setq %cached-parameter-names (continue-whopper)
	  %cached-parameter-timestamp *parameter-timestamp*))
  %cached-parameter-names)
	   

(defmethod (2show-list-of-parameter-values0 common-parameters-mixin) 
	   (pnames &key if-nonstandard)
  (loop for i in pnames 
	as val = (funcall i self)
	do
    (multiple-value-bind (pval inst global-p)
	(parameter-value i)
      (ignore pval global-p)
      (let ((sval (go-parameter-standard-value inst)))
	(when (or (null if-nonstandard)
		  (not (eql sval val)))
	  (format t "~&~6a  ~A~&        ~A"
		  val i (or (get-prop inst :documentation) "")))))))

(tv:def-menu-method 2show-counter-values0 common-parameters-mixin
  :button :mouse-middle
  :context :default
  :menu-documentation "show nonzero counter values   r: show all"
  :menu-id "show counters"
  (item x y button)
  (ignore x y item)
  (let ((pnames (my-counter-names self)))
    (show-list-of-parameter-values self pnames :if-nonstandard (neq button :mouse-right))
    self))


(defmethod (2local-parameter-value0 common-parameters-mixin) (name &optional (error-p t))
  (cond ((member name (my-parameter-names self))
	 (values (funcall name self) t))
	(error-p (signal-error ("~S isn't a local parameter of ~S" name self)))))

(defun 2setf-function0 (a)
  (with-stack-list (d 'setf a)
    (fdefinition d)))

(defmethod (2(setf local-parameter-value)0 common-parameters-mixin)
	   (name error-p to)
  (cond ((member name (my-parameter-names self))
	 (values (funcall (setf-function name) self to) t))
	(error-p (signal-error ("~S isn't a local parameter of ~S" name self)))))

(defun 2parameter-presentation-type0 (parameter-name)
  (multiple-value-bind (val inst ) (parameter-value parameter-name)
    (ignore val)
    (or (get-prop inst :type)
	'dbg:evaluated-expression)))

(defun 2show-parameter-values0 (name &key
			      (game (when (boundp '*game*) *game*))
			      (show-if-changes)
			      (show-if-nonstandard t)
			      (show-if-nonstandard-message "(4 0 (NIL 0) (NIL NIL :SMALL) "TVFONT")standard value0")
			      (set-new-value nil new-value-supplied)
			      (show-all t)
			      (set-to-standard-value nil))

  (declare (special *game* all-games))
  (multiple-value-bind (val inst global-p)
      (parameter-value name)
    (ignore val)
    (when inst
      (multiple-value-bind (standard-val standard-value-exists)
	  (cond ((or (member set-to-standard-value '(t :standard))
		     (member show-if-nonstandard '(t :standard))
		     (and (eql show-if-nonstandard :counter)
			  (get-prop inst :counter)))
		 (values (go-parameter-standard-value inst) t))
		((or (eql set-to-standard-value :debug)
		     (eql show-if-nonstandard :debug))
		 (get-prop inst :debug))
		((or (eql set-to-standard-value :wide-search)
		     (eql show-if-nonstandard :wide-search))
		 (get-prop inst :wide-search))
		(t nil))
      (let* ((new-value-supplied (or new-value-supplied
				     (and set-to-standard-value
					  standard-value-exists)))
	     (set-new-value (if (and set-to-standard-value standard-value-exists)
				standard-val
				set-new-value)))

	(labels ((print-value-set (item val &optional name)
		   (let* ((print-new-comment (and show-if-nonstandard
						  standard-value-exists
						  (cond ((eql show-if-nonstandard :counter)
							 (not (eql val 0)))
							(t (not (equal val standard-val))))))
			  (print-new-value (and new-value-supplied
						(if show-if-changes
						   (not (equal val set-new-value))
						   t)))
			  (print-name (or show-all print-new-value print-new-comment)))

		     (when print-name
		       (format t "~&")
		       (when name (format t "~A For " name))
		       (format t "~S=~S" item val))

		     (when print-new-value
		       (format t " -> ~S" set-new-value))

		     (when (and print-new-comment
				(neq show-if-nonstandard :counter))
		       
		       (if set-to-standard-value
			   (format t 4"  (0~4a value)"0 show-if-nonstandard-message)
			   (format t 4"  (~a value is ~S)"0 
				   show-if-nonstandard-message standard-val))
		       )
 		     )

		   (and new-value-supplied (not (equal set-new-value val)))
		   ))

	  (when (Print-value-set inst (go-parameter-value inst))
	    (setf (go-parameter-value inst) set-new-value))

	  (when global-p 
	    (when (print-value-set name (symbol-value name))
	      (setf (symbol-value name) set-new-value)))

	  (cond ((eq game t) (setq game all-games))
		((cl:listp game))
		(t (setq game (list game))))

	  (loop for g in game do
	    (map-over-inferiors
	      g
	      :after
	      #'(lambda (a)
		  (when (and (typep a 'common-parameters-mixin)
			     (member name (my-parameter-names a)))
		    (when (print-value-set a (local-parameter-value a name) name)
		      (setf (local-parameter-value a name nil) set-new-value))
		    )))
	    )))))))

(defun 2set-parameter-values0 (name value &key 
			     (game (when (boundp '*game*) *game*))
			     set-to-standard-value)
  (declare (special *game* all-games))
  (show-parameter-values name 
			 :game game 
			 :set-new-value value
			 :set-to-standard-value set-to-standard-value))

(defun 2parameter-names0 ()
  (let ((val)) (maphash #'(lambda (k d) (ignore d) (push k val)) *go-parameters*)
       val))
			
(defun 2update-parameter-values0 (&key (game t) set-to-standard-value)
  (loop for n in (parameter-names)
	as v  = (parameter-value n)
	do
    (show-parameter-values n :game game :show-if-changes t :set-new-value v
			   :set-to-standard-value set-to-standard-value)))

(define-presentation-type 2parameter-name0 ()
   :history t
   :parser ((stream &key initially-display-possibilities)
	    (dw:complete-from-sequence (parameter-names) stream
				       :initially-display-possibilities
				        initially-display-possibilities))
   :description "a parameter name")

(define-presentation-type 2go-game-name0 ()
   :history t
   :parser ((stream &key initially-display-possibilities)
	    (dw:complete-from-sequence all-games stream
				       :name-key #'(lambda (a) (viewer-name a))
				       :initially-display-possibilities
				        initially-display-possibilities))
   :description "a go game")


(define-cp-command (2show-parameter-values-com
0		     :command-table :global
		     :name "Show Parameter Values")
    ((parameter 'parameter-name)
     &key (game 'go-game-name :default (when (boundp '*game*) *game*)))
   (show-parameter-values parameter :game game))

(defvar *standard-value* "Standard-value")
(defvar *debug-value* "Debug-value")
(defvar *wide-search-value* "Wide-search-value")
(defvar *special-parameter-type*
	`(alist-member :alist 
		       (("standard" :value ,*standard-value*)
			("debug" :value ,*debug-value*)
			("wide-search" :values ,*wide-search-value*)
			)))

(define-cp-command (2set-parameter-values-com
0		     :command-table :global
		     :name "Set Parameter Values")
    ((parameter 'parameter-name)
     (new-value `(or ,(parameter-presentation-type parameter)
		     ,*special-parameter-type*))
     &key (game 'go-game-name :default (when (boundp '*game*) *game*)))
   (set-parameter-values parameter
			 (if (eql (parameter-presentation-type parameter)
				  'dbg:evaluated-expression)
			     (second new-value)
			     new-value)
			 :set-to-standard-value (eq new-value *standard-value*)
			 :game game))

(define-cp-command (2show-all-parameter-values-com
0		     :command-table :global
		     :name "Show All Parameter Values")
    ((type 
       '(alist-member 
	  :alist
	  (("nonstandard" :value :standard
	    :Documentation "show all parameter values not equal to their standard values")
	   ("nondebug" :value :debug
	     :documentation "show all parameter values not equal to their debug values")
	   ("counters" :value :counter
	    :documentation "show all counter values")
	   ("wide-search"  
	    :value :wide-search
	    :documentation "values suitable for least selective search")
	   ("all" :value t
		 :documentation "really show all parameter values")
	   )))
     &key
      (match 'string :default "*")
      (game 'go-game-name :default (when (boundp '*game*) *game*))
      )
   (loop for n in (parameter-names)
	 when (fs:wildcard-match match n)
	 do
     (show-parameter-values 
       n
       :game game 
       :show-all (eql type t)
       :show-if-nonstandard-message 
       (case type
	(:debug 4"debug"0)
	(:standard4 "standard"0)
	(:wide-search4 "wide search"0)
	(otherwise4 "standard"0))
       :show-if-nonstandard type)
   ))

(define-cp-command (2set-all-parameter-values-com
0		     :command-table :global
		     :name "Set All Parameter Values")
    ((type '(alist-member 
	      :alist
	      ((:standard :value :standard
		 :Documentation
		 "set all parameter values to their standard values")
	       (:debug :value :debug
		 :documentation "set all parameter values to their debug values")
	       (:wide-search :value :wide-search
			     :documentation "values suitable for least selective search")
	       )))
      &key
      (match 'string :default "*")
      (game 'go-game-name :default (when (boundp '*game*) *game*))
      )
   (loop for n in (parameter-names)
	 when (fs:wildcard-match match n)
	 do
     (show-parameter-values 
       n
       :game game 
       :show-if-changes t
       :show-all nil
       :set-to-standard-value type
       :show-if-nonstandard type)
   ))

(defmacro 2build-parameter-flavors0 ()
  (incf *parameter-timestamp*)
  (let ((flavors))
    (maphash #'(lambda (key dat)
		 (ignore key)
		 (loop for f in (go-parameter-flavors dat)
		       unless (eql f :global)
			 do
			   (setf (getf flavors f)
				 (cons dat (getf flavors f)))))
	     *go-parameters*)
    `(progn
       ,@(loop for (prop vars) on flavors by #'cddr 
	       as name = (intern (string-append (string prop) "-PARAMETERS-MIXIN") (find-package "GO"))
	       as par-names = (loop for v in vars collect (go-parameter-name v))
	       as locatable = (loop for v in vars as loc = (get-prop v :locatable)
				    when loc collect (go-parameter-name v))
	       as counter = (loop for v in vars as loc = (get-prop v :counter)
				    when loc collect (go-parameter-name v))
	       collect
		 `(defflavor ,name (,@(loop for v in par-names collect `(,v (parameter-value ',v))))
			     (common-parameters-mixin)
		    (:conc-name nil)
		    :writable-instance-variables
		    (:locatable-instance-variables ,@locatable)
		    (:method-combination my-parameter-names :append my-counter-names :append)
		    :initable-instance-variables)
	       when counter
		 nconc 
		   (loop for cn in counter 
			 as iname = (intern (string-append "INC-" (string cn) )
					    (find-package "GO"))
			 collect 
			   `(defmethod (,iname ,name) ()
			      (incf ,cn)
			      (let ((pw parameter-watch))
				(when pw
				  (let ((watch (assoc ',cn pw)))
				    (when (funcall (second watch) ,cn)
				      (break "Change ~S to ~D" ',cn ,cn))
				    )))))
	       collect `(defmethod (my-counter-names ,name) ()
			  ',counter)
	       collect
		 `(defmethod (my-parameter-names ,name) ()
		    ',par-names)
	       collect
		 `(defmethod (re-init-parameters ,name) ()
		    ,@(loop for v in par-names collect `(setf ,v (parameter-value ',v)))
		    )
		 )
       )))

1;;
;; A special font for shapes
;;
0(eval-when (compile eval load)

(defvar put-isomers-in-shape-font t) ;; if T, put the isomers in the shape font -
					;; potentially 4000 of them.  If NIL, just build
					;; the basic shape font, about 600
(defvar go-character-set-size (if go:put-isomers-in-shape-font 4000 600))

)

(defun make-go-font (&key
		     (name (gensym))
		     (height 12)
		     (width  (* 2 height))
		     )

  (setq name (intern (string name) 'fonts))

  (let ((new-font (tv:make-font
		    make-array (:dimensions (list height
						  (* 32 (ceiling (* go-character-set-size
								    24)
								 32)))
				:area *go-static-area*
				:type 'sys:art-1b)
		    tv:fill-pointer 0
		    tv:name name
		    tv:char-height height
		    tv:char-width width
		    tv:blinker-height height
		    tv:blinker-width width
		    tv:raster-height height
		    tv:raster-width width
		    tv:baseline (- height 2))))
    (set name new-font)
    new-font))

(defflavor shape-character-set () (si:character-set-no-case-mixin si:basic-character-set)
  (:default-init-plist
   :name "GO-SHAPES"
   :n-characters go-character-set-size
   :characters nil
   ))

(defmethod (:char-equal shape-character-set) (index1 index2)
  (= index1 index2))

(defmethod (:char-lessp shape-character-set) (&rest ignore)
  (error ":CHAR-LESSP is not meaningful in the shape character set."))

(defmethod (:alpha-char-p shape-character-set) (ignore)
  nil)

(defmethod (:graphic-char-p shape-character-set) (ignore)
  t)

(compile-flavor-methods shape-character-set)


(defvar shape-font-index 0)
(defvar shape-font-1 (make-go-font :name "SHAPE-FONT-1" ))

(defvar shape-character-set (make-instance 'shape-character-set :area *go-static-area*))

;; this is a private style for shape characters.  
(si:define-character-style-families si:*b&w-screen* si:*standard-character-set*
  `(:family :shapes (:face :normal (:size :normal fonts:shape-font-1))))
(si:define-character-style-families si:*b&w-screen* shape-character-set
  `(:family :shapes (:face :normal (:size :normal fonts:shape-font-1))))

(loop for fam in si:*valid-families*
      when (neq fam :device-font)
      do
  (loop for face in si:*valid-faces*
	do
    (si:define-character-style-families si:*b&w-screen* shape-character-set
      `(:family ,fam (:face ,face (:size si:* (:style :shapes :normal :normal)))))))


(5 0 (NIL 0) (NIL :BOLD :VERY-LARGE) "BIGFNTB");;
;; String Utilities
;;

0(defun 2first-non-blank0 (new &optional (initial-index 0))
  (when new
    (loop as j from initial-index below (length new)
	  as a = (aref new j)
	  unless (or (char-equal a #\SPACE) (char-equal a #\tab))
	    do (loop-finish)
	  finally (return j))))

(defun 2first-blank0 (new &optional (initial-index 0))
  (when new
    (loop as j from initial-index below (length new)
	  as a = (aref new j)
	  when (or (char-equal a #\SPACE) (char-equal a #\tab))
	    do (loop-finish)
	  finally (return j))))

(defun 2trim-trailing-whitespace0 (s) (string-right-trim "
	 " s))

(defun 2string-member0 (a l) (member a l :test #'string-equal))

(defun 2substring-either0 (a b)
  (if ( (string-length a)(string-length b))
      (string-search b a)
      (string-search a b)))

(defun 2substring-member0 (a l) (member a l :test #'substring-either))

(defun 2string-intersection0 (a l) (intersection a l :test #'string-equal))

(defun 2string-union0 (a l) (union a l :test #'string-equal))

(defun 2remove-words0 (list &rest lists)
  (loop	for l in lists
	with val = list
	do (setq val (set-difference val l :test #'string-equal))
	   finally (return val)))

(defun 2segment-into-lines0 (string)
  (when string
    (loop with from = 0
	  as next = (string-search-char #\newline string :start from)
	  when (and next (> next (1+ (or from 0))))
	  collect (substring string from next) into val
	  while next
	  do (setq from (1+ next))
	     finally (when (and from (< from (string-length string)))
		       (setq val (nconc val (list (substring string from )))))
		     (return val)
	     )))

(defun 2string-subst0 (new-string old-string base-string &key (error-p t) (ntimes 1) (start 0))
  (let ((len (string-length old-string))
	(loc (string-search old-string base-string :start2 start)))
    (when error-p (unless loc (error "not found")))
    (if loc
      (string-append (substring base-string start loc)
		     new-string
		     (if ( ntimes 1)
			 (string-subst new-string old-string base-string 
				       :start (+ loc len)
				       :ntimes (1- ntimes)
				       :error-p error-p)
			 (substring base-string (+ loc len))
			 ))
      (substring base-string start))))

(defun 2unix-name0 (na)
  (string-subst
    "-" ":"
  (string-subst
    "-" "--"
    (string-subst 
      "-" 
      "."
      (string-subst 
	"" ","
	(string-subst "-" "/"
		      (string-subst "-" " "  na :error-p nil :ntimes -1)
		      :error-p nil
		      :ntimes -1)
	:error-p nil
	:ntimes -1)
      :error-p nil
      :ntimes -1)
    :error-p nil
    :ntimes -1)
  :error-p nil
  :ntimes -1))

(defvar-value 2*punctuation-chars*0 `(#\, #\. #\: #\; #\' #\`
			     #\open #\close))
(defvar-value 2*numbers*0 '(#\0 #\1 #\2 #\3 #\4 #\5 #\6 #\7 #\8 #\9))

(defun 2remove-punctuation-substring0 (string &optional (start 0)(end (length string)))
  (loop for i from start below end
	as chr = (aref string i)
	when (member chr *punctuation-chars* :test #'char-equal)
	  do (setf (aref string i) #\space))
  string
  )

(defun 2strip-punctuation-substring0 (string &optional (from 0) to)
  (loop for n from (1- (or to (length string))) by -1
	while (> n from)
	as c = (aref string n)
	while (member c *punctuation-chars*
			 :test #'char-equal)
	do ()
	finally
	  (incf n)
	  (return
	    (when (> n from)
	      (loop for f from from
		    while (< f n)
		    as c = (aref string f)
		    while (member c *punctuation-chars* :test #'char-equal)
		    do ()
		    finally
		      (return (if (> n f) (substring string f n))))))))

3;strip matching parenthesis and all inbetween
0(defun 2strip-parenthetic-remarks0 (string &optional (from 0))
  (loop as paren = (string-search-char #\open string :start from)
	as rem = (when paren (strip-parenthetic-remarks string (1+ paren)))
	as match = (when rem (string-search-char #\close rem))
	return (cond ((and paren match)
		      (string-append
			(substring string from paren)
			(substring rem (1+ match))))
		     ((and paren rem)
		      (string-append (substring string from (1+ paren)) rem))
		     (t (substring string from)))))

(defun 2segment-into-words0 (string &optional (start 0) end (word-breaks `(#\space #\tab)))
  (when string
    (loop with from = start
	  as next = (string-search-set word-breaks string :start from :end end)
	  as v = (strip-punctuation-substring string from (or next end))
	  when v  collect v
	  while (and next (or (null end) (< next end)))
	  as br = (aref string next)
	  unless (member br '(#\space #\tab) :test #'char-equal)
	    collect (substring string next (1+ next))
	  do 
      (setq from (+ next 1)))))

(defun 2concatinate-words 0(strings)
  (format nil "~A~{ ~A~}" (first strings) (cdr strings)))

(defun 2read-first-word-of-string0 (string &optional eof-error-p eof-value  
				  &key (start 0) end)
  (let* ((start (or start 0))
	 (end (or end (length string))))
    (loop while (< start end)
	  as ch = (aref string start)
	  when (or (alpha-char-p ch)
		   (digit-char-p ch))
	    do (loop-finish)
	  do (incf start))
    (let ((real-start start))
      (loop while (< start end)
	    as ch = (aref string start)
	    unless (or (alpha-char-p ch)
		       (digit-char-p ch))
	      do (loop-finish)
	    do (incf start)
	       )
      (read-from-string string eof-error-p eof-value 
			:start real-start
			:end start))))


#||
 *
 * This implementation of the Soundex algorithm is released to the public
 * domain: anyone may use it for any purpose.  See if I care.
 * (original in C, translated to lisp by ddyer@netcom.com)
 *
 * N. Dean Pentcheff
 * 1/13/89
 * Dept. of Zoology
 * University of California
 * Berkeley, CA  94720
 * dean@violet.berkeley.edu
 *
 * char * soundex( char * )
 *
 * Given as argument: Pointer to a character string.
 * Returns: Pointer to a static string, 4 characters long, plus a terminal
 *    '\0'.  This string is the Soundex key for the argument string.
 * Side effects and limitations:
 *    Does not clobber the string passed in as the argument.
 *    No limit on argument string length.
 *    Assumes a character set with continuously ascending and contiguous
 *       letters within each case and within the digits (e.g. this works for
 *       ASCII and bombs in EBCDIC.  But then, most things do.).
 * Reference: Adapted from Knuth, D.E. (1973) The art of computer programming;
 *    Volume 3: Sorting and searching.  Addison-Wesley Publishing Company:
 *    Reading, Mass. Page 392.
 * Special cases:
 *    Leading or embedded spaces, numerals, or punctuation are squeezed out
 *       before encoding begins.
 *    Null strings or those with no encodable letters return the code 'Z000'.
 * Test data from Knuth (1973):
 *    Euler   Gauss   Hilbert Knuth   Lloyd   Lukasiewicz
 *    E460    G200    H416    K530    L300    L222
 */
||#

(defvar 2soundex-key
0	 (make-array 26 :element-type '(unsigned-byte 8)
		     ;;              a b c d e f g h i j k l m n o p q r s t u v w x y z 
		:Initial-contents '( 0 1 2 3 0 1 2 0 0 2 2 4 5 5 0 1 2 6 2 3 0 1 0 2 0 2 )))

(defun 2soundex-key0 (string)
  (let ((key0 (- (char-code #\Z) (char-code #\A)))
	(key1 0)
	(key2 0)
	(key3 0))

    (loop with lastkey = nil
	  with theend = (length string)
	  for i from 0 below theend 
	  as ch = (aref string i)
	  as alpha = (alpha-char-p ch)
	  when alpha 
	    do 
	      (setq ch (char-upcase ch))
	      (let ((thiskey (aref soundex-key (- (char-code ch)(char-code #\A)))))
		(cond ((null lastkey)
		       (setq key0 ch)
		       (setq lastkey thiskey))
		      ((eql thiskey lastkey))
		      ((zerop thiskey)(setq lastkey thiskey))
		      ((eql key1 0) (setq lastkey thiskey) (setq key1 thiskey))
		      ((eql key2 0) (setq lastkey thiskey) (setq key2 thiskey))
		      (t (return (setq key3 thiskey))))))
    (values key0 key1 key2 key3)
    ))


(defun 2correlate-strings-internal0 (s1 s2 start-1 start-2 end-1 end-2 threshold transpose)
  (let* ((l1 (string-length s1))
	 (l2 (string-length s2))
	 (l1 (min (or end-1 l1) l1))
	 (l2 (min (or end-2 l2) l2))
	 (s1len (abs (- l1 start-1)))
	 (s2len (abs (- l2 start-2)))
	 (mismatch (abs (- s1len s2len)))
	 (cutoff (or threshold s1len)))

    (if (> mismatch cutoff)
	mismatch
	(loop for s1i from start-1 below l1
	      as s2i from start-2 below l2
	      as c1 = (aref s1 s1i)
	      as c2 = (aref s2 s2i)
	      unless (char-equal c1 c2)
		return
		  (if ( cutoff 0)
		      (max s1len s2len)
		      (min (+ (let ((mm (cond ((and transpose
						    ( start-1 1)
						    ( start-2 1)
						    (char-equal (aref s1 (1- s1i)) c2)
						    (char-equal (aref s2 (1- s2i)) c1))
					       0)
					      (t 1))))
				(+ mm
				   (correlate-strings-internal s1 s2
							       (1+ s1i)(1+ s2i)
							       l1 l2
							       (- cutoff mm)
							       transpose)
				   )))
3						;add or drop letter
0			   (+ 1 (correlate-strings-internal s1 s2
							    (1+ s1i) s2i
							    l1 l2
							    (- cutoff 1)
							    transpose))
			   (+ 1 (correlate-strings-internal s1 s2
							    s1i (1+ s2i) 
							    l1 l2
							    (- cutoff 1)
							    transpose)
			      )))
	      finally (return mismatch)))))

(defvar *soundex-match* t)
(defun 2correlate-strings
0       (s1 s2 &key (word-wise t)(any-word-order t)(transpose t) (soundex-match *soundex-match*)
	(start 0)(end )
	(start-1 start)(start-2 start)
	(end-1 end)(end-2 end)
	(word-penalty 5)
	(threshold))
  (if word-wise
      (let* ((s1 (if (consp s1) s1 (segment-into-words s1 start-1 end-1)))
	     (s2 (if (consp s2) s2 (segment-into-words s2 start-2 end-2)))
	     (max-len (max (loop for s in s1 sum (string-length s))
			   (loop for s in s2 sum (string-length s))))
	     (mismatch (* word-penalty (abs (- (length s1 )(length s2)))))
	     (cutoff (- (min (or threshold max-len)
			     max-len)
			mismatch)))

	(cond ((< cutoff 0) mismatch)
	      (any-word-order
3						;first remove any exact matches
0	       (let ((inter (intersection s1 s2 :test #'string-equal)))
		 (when inter (setq s1 (set-difference s1 inter :test #'string-equal)
				   s2 (set-difference s2 inter :test #'string-equal))))

	       (when (> (length s1)(length s2))(psetq s1 s2 s2 s1))
	       (loop for s in s1 
		     as best = nil
		     as best-v = nil
		     while (>= cutoff 0)
		     do
		 (loop for w in s2
		       as v = (correlate-strings s w
						 :word-wise nil
						 :soundex-match soundex-match
						 :transpose transpose
						 :threshold cutoff
						 )
		       when (or (null best-v)(< v best-v ))
			 do (setq best-v v best w))
		 (cond (best-v
			(setq s2 (delete best s2 :test #'string-equal :count 1))
			(incf mismatch best-v)
			(decf cutoff best-v)
			))
		 )
	       )

	      (t 
		 (loop for s in s1 as w in s2
		       as best-v = (correlate-strings s w
						     :word-wise nil
						     :soundex-match soundex-match
						     :transpose transpose)
		       while (>= cutoff 0)
		       do
		   (incf mismatch best-v)
		   (decf cutoff best-v)
		   )))
	mismatch
	)
      (cond ((and soundex-match
		  threshold
		  (multiple-value-bind (k0 k1 k2 k3)
		      (soundex-key s1)
		    (multiple-value-bind (a0 a1 a2 a3)
			(soundex-key s2)
		      (not (and (eql k0 a0)
				(eql k1 a1)
				(eql k2 a2)
				(eql k3 a3))))))
	     (1+ threshold))
	    (t (correlate-strings-internal s1 s2 start-1 start-2 end-1 end-2
					   threshold transpose))
      )))


(defun 2correct-spelling0 (word &optional common-misspellings)
  (if (consp word)
      (loop for i on word
	    do (setf (first i) (correct-spelling (first i) common-misspellings))
	       finally (return word))
      (loop for (good . bad) in common-misspellings
	    when (string-member word bad) return good
	    finally (return word))))


(defun 2print-brief-date0 (ut &optional (stream t))
  (cond ((null ut) 
	 (format stream 3"no value"0))
	((integerp ut)
	 (let ((time:*DATE-PRINTING-FORMAT* :mm-dd-yyyy))
	   (multiple-value-bind (SECS MINUTES HOURS DAY MONTH YEAR DAY-OF-THE-WEEK)
	       (decode-universal-time ut)
	     (format stream "~A ~D, ~D" (time:month-string month) day year)
	     (ignore day-of-the-week)
	     (unless (and (zerop hours)(zerop minutes)(zerop secs))
	       (format stream " ~D:~2,'0D" hours minutes))
	     (unless (zerop secs)
	       (format stream ":~2,'0D" secs))
	     )))
	(t (format stream "~A" ut))
	))

(format:defformat 2format:brief-date0 (:one-arg) (arg parameters)
  parameters
  (print-brief-date arg format:*format-output*))

(defun 2parse-game-time-internal 0(str must-have-year)
  (condition-case (err)
       (time:parse-universal-time str 0 nil nil 
						;require year
				  nil nil must-have-year)
     (time:parse-error
       (values nil  (with-output-to-string (str)
		      (send err :report str))))))

;call this one when you've got some junky thing from a game title
(defun 2parse-game-time0 (words &optional noise-words must-have-year)
  (let* ((words (remove-words words noise-words '("at")))
	 (not-number 0)
	 (number 0)
	 (year-first nil)
	 (more-than-31 0)
	 (less-than-31 0))

    ;change "nn,kk" into "nn kk"
    (loop for wp on words 
	  as (w) = wp
	  do
      (loop as first-char = (and (> (string-length w) 0) (aref w 0))
	    as match = nil
	    as comma = (and (member first-char *numbers*)
			    (incf number)
			    (or (string-search (setq match ",") w)
				(string-search (setq match ".") w)
				(string-search (setq match "-") w)
				(string-search (setq match "/") w)
				(string-search (setq match "&") w)

				(string-search (setq match "'st") w)
				(string-search (setq match "'nd") w)
				(string-search (setq match "'rd") w)
				(string-search (setq match "'th") w)

				(string-search (setq match "st") w)
				(string-search (setq match "nd") w)
				(string-search (setq match "rd") w)
				(string-search (setq match "th") w)
				))
	    while comma
	    do (loop for i from comma
		     repeat (string-length match)
		     do (setf (aref w i) #\space)))

      (let ((seg (segment-into-words w )))
	(setf (first wp) (first seg))
	(setf (cdr wp)
	      `(,@(cdr seg)
		,@(cdr wp))))
      )
						;add a day of 1 if none is apparent
     (loop as first-word = t then nil
	   for w on words
	  as n = (catch-error (read-from-string (first w) nil nil) nil)
	  do (cond ((not (integerp n))
		    (incf not-number)
		    (when (> not-number 1)
		      (setf (car w) nil)))
		   ((> n 31)
		    (incf more-than-31)
		    (setq year-first first-word)
		    (when (> more-than-31 1)
		      (setf (car w) nil)))
		   (t 
		    (incf less-than-31)
		    (when (and (> less-than-31 1)(< more-than-31 1))
		      (setf (car w) nil)))
		   )
	     )

    (setq words (remove nil words))

    (when (and (zerop not-number)
	       (zerop less-than-31)
	       (> more-than-31 0))
						;year only, supply Jan
      (push "Jan" words)
      (incf not-number)
      )

    (when (and ( not-number 1)(zerop less-than-31)( more-than-31 1))
      (incf less-than-31)
      (push "1" words))

    (unless (and (zerop less-than-31)(zerop more-than-31)(zerop number))
      (when (and year-first (> (length words) 3))
	(setf (cdddr words) nil))
      (let* ((complete (format nil "~A~{-~A~}" (first words) (cdr words))))
	(multiple-value-bind (time msg)
	    (parse-game-time-internal complete must-have-year)
	#||
	(if time
	    (multiple-value-bind (h min s d m y)
		(time:decode-universal-time time)
	      (ignore h min s d m)
	      (when (minusp y)
		(format t "~&parsed ~A as ~\\brief-date\\ " complete time)
		))
	    (format t "~&parsed ~A as ~A" complete msg)
	    )
	||#
	(values time msg)
	)))))

;call this one when you've a theoretically good string, ie
(defun 2parse-game-time-string0 (str)
  (parse-game-time-internal str t))


(defun2 parse-biography 0(str)
  (loop with birth-date and death-date and nationality and rank
	and all-lines = (segment-into-lines str)
	and nnames = (nationality-names)
	and first-line  = t
	for line in all-lines
	as words = (correct-spelling (segment-into-words (strip-parenthetic-remarks line))
				     *misspellings*)
	as len = (length words)
	do
    (cond (first-line 
	   (let* ((r (string-equal "dan" (nth (1- len) words)))
		  (n (when r (ignore-errors (read-from-string (nth (- len 2) words))))))
	     (when (numberp n) (setq rank n)))
	   (setq first-line nil))
	  (t (when (string-equal "Born" (first words))
	       (let* ((died (position "died" words :test #'string-equal))
		      (diedsub (when died (subseq words (1+ died) (+ died 4))))
		      (bornsub (if died 
				   (subseq words 1 (min 4 (1- died)))
				   (subseq words 1 4 )
				   )))
		 (setq birth-date (parse-game-time bornsub nil t))
		 (setq death-date (parse-game-time diedsub nil t))
		 ))
	     (loop for n in nnames 
		   when (string-member n words)
		     do (pushnew n nationality ))))
    finally (return
	      `(,@(when birth-date `(:birth-date ,birth-date))
		,@(when death-date `(:death-date ,death-date))
		,@(when (and nationality (null (cdr nationality)))
		    `(:nationality ,(first nationality)))
		,@(when rank `(:rank ,rank))))))

3;;
;; print pictures  0(3of a set0)3 with a margin spec.
;;
0(defun 2pretty-print-picture-internal0 (picture stream &optional lmargin)

  (let ((posmes (cond ((send stream ':operation-handled-p ':read-cursorpos)
		       '(:read-cursorpos :character))
		      ((send stream ':operation-handled-p ':position)
		       '(:position))))
	(cpos 0))

    (if posmes

	(if lmargin
	    (when (> (setq cpos (apply stream posmes)) lmargin)
	      (terpri stream))
	    (setq lmargin (setq cpos (apply stream posmes))))

	(unless lmargin (setq lmargin 0))
	(loop repeat (- lmargin cpos) do (princ " ")))

    (with-input-from-string (str picture)
      (loop as chr =  (read-char str nil nil)
	    while chr
	    do
	(case chr
	  (#\NEWLINE
	   (terpri stream)
	   (loop repeat lmargin do (write-char #\SPACE stream)))
	  (otherwise (write-char chr stream))))
      )
    ))

(defun 2print-list-of-pictures0 (stream lst &optional slashify
			       &key (intercolumn-spaces 1)
			       (interrow-spaces 1))
  (let ((lh (send stream :line-height))
	(cw (send stream :char-width)))
    (multiple-value-bind (xp yp)
	(send stream :read-cursorpos)
      (multiple-value-bind (xs ys)
	  (send stream :size-in-characters)
	(setq xs (* xs cw)
	      ys (* ys lh))
	(loop with xpos = xp and max-lines-printed = 0
	      for pic in lst
	      do
	  (send stream :set-cursorpos xpos yp)
	  
	  (multiple-value-bind (xc yc) (tv:bounding-box-chars pic slashify)
	    
	    (setq max-lines-printed (max max-lines-printed yc)
		  xc (* (+ intercolumn-spaces xc) cw)
		  xpos (+ xpos xc))
	    
	    (when (>= xpos xs)
	      (setq xpos (+ xp xc))
	      (send stream :set-cursorpos nil (+ yp (* lh max-lines-printed)))
	      (send stream :fresh-line)
	      (loop repeat interrow-spaces do (terpri))
	      (multiple-value-setq (nil yp) (send stream :read-cursorpos))
	      (send stream :set-cursorpos xp nil)
	      )
	    (tv:print-boxed-string stream pic slashify)
	    (multiple-value-bind (nil new-yp)
		(send stream :read-cursorpos)
	      (setq yp (- new-yp (* lh yc)))
	      )
	    ))))))


5;;
;; Some language extensions
;;

0si:
(defun 2consing-done0 ()
  (+ TOTAL-CONS-WORK-DONE *CONS-WORK-DONE* ))

(defun 2id-string0 ()
 (multiple-value-bind (maj min) (sct:get-system-version 'go)
			   (format nil "Go version ~A.~A on ~\\date\\ "
				   maj min (time:get-universal-time))))

(defmacro 2with-assertion0 (assertion form &rest format-args)
  (declare (special check-assertions))
  `(progn
    ,(when check-assertions
       `(when check-assertions
	  (assert ,assertion nil ,@(copy-list format-args))))
    ,form))
	 

(defmacro 2with-restart-options0 ( conditions . body)
  (let* ((outer-name (gensym))
	 (core `(return-from ,outer-name
		  (progn ,@body))))

    (loop for (condition . action) in conditions
	  do (setq core `(progn (catch-error-restart ,condition
				  ,core)
				(return-from ,outer-name
				  (progn ,@action)))))
    `(macrolet
       ((restart ()
	  `(return-from inner-loop nil)))
       (loop named ,outer-name
	     doing
	 (block inner-loop
	   ,core)))))


3;;
;;
;; Stuff to keep track of the color adjacent to a position.
;;
;;

0(defun2 decode-adjacent-to-internal0 (bits)
  (loop for bitmask first #o10 then (lsh bitmask -1)
	until (zerop bitmask)
	as cmask = (dpb bitmask (byte 4 4) bitmask)
	as emask = (logand bits cmask)
	collect
	(cond ((= emask 0) :empty)
	      ((= emask bitmask) :white)
	      ((= emask cmask) :border)
	      (t :black))))


(defun 2encode-adjacent-to-internal0 (color position)
  (cond ((consp color)
	 `(logior ,@(loop for i in color
			  collect (encode-adjacent-to-internal i position))))
	((consp position)
	 `(logior ,@(loop for i in position
			  collect
			    (encode-adjacent-to-internal i position))))
	(t (let ((bit (case position
			(:left #o10)
			(:top #o4)
			(:right #o2)
			(:bottom #o1)
			(otherwise (signal-error ("~A isn't encodable as an adjacent position"
				   position))))))
	     (case color
	       (:white bit)
	       (:black (dpb bit (byte 4 4) 0))
	       (:empty 0)
	       (:border (dpb bit (byte 4 4) bit))
	       (otherwise `(case color
		     (:white ,bit)
		     (:black ,(dpb bit (byte 4 4) 0))
		     (:empty 0)
		     (:border ,(dpb bit (byte 4 4) bit))
		     (otherwise (signal-error ("~A isn't encodable as an adjacent color" color)))
	       )))))))
      
(defmacro 2encode-adjacent-to0 (color position)
  (encode-adjacent-to-internal color position))

(defmacro 2loop-adjacent-colors
0	  ((encoded-colors &key color dx dy direction second-code second-color)
	   &body body)
  `(loop
    with mask = (encode-adjacent-to :border :left)
    repeat 4
    as encoded = ,encoded-colors then (lsh encoded 1)
    as masked = (logand mask encoded)
    ,@(when (or dx dy)
	`(for (,dx ,dy) in cardinal-directions))
    ,@(when direction
	`(for ,direction in cardinal-direction-names))
    ,@(when color
	`(as ,color = (cond ((= masked 0) :empty)
			    ((= masked (encode-adjacent-to :white :left)) :white)
			    ((= masked (encode-adjacent-to :black :left)) :black)
			    (t :border))))
    ,@(when second-code
	`(as second-encoded = ,second-code then (lsh second-encoded 1)
	     as second-masked = (logand mask second-encoded)
	     as ,second-color = (cond ((= second-masked 0)
				       :empty)
				      ((= second-masked (encode-adjacent-to :white :left))
				       :white)
				      ((= second-masked (encode-adjacent-to :black :left))
				       :black)
				      (t :border))))
    ,@body
    ))

2;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Integer encoding of move-ish things, X,Y plus some other information
0;; the variations on the basic representation should be mutually exclusive,
;; so any errors in passing around encoded variations will be caught
2;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

0(defconstant 2com-x-byte-spec0 (byte 5 5))
(defconstant 2com-y-byte-spec0 (byte 5 0))
(defconstant 2com-byte-spec0 (byte 5 10))
(defconstant 2com-move-number-byte-spec0 (byte 10 15))
(defconstant 2com-max-depth0 (byte 7 25))

;canonical representation doesn't include move number.  Used by canonical sequence stuff
(defconstant 2com-canonical-representation0 (byte 15 0))

(defmacro 2decode-move-number0 (val)
  `(let ((mn (ldb com-move-number-byte-spec ,val)))
     (if (= mn (ldb com-move-number-byte-spec -1)) nil mn)))


(defun 2decode-encoded-move0 (m &optional error-p)
  (let ((x (ldb com-x-byte-spec m))
	(y (ldb com-y-byte-spec m))
	(com (ldb com-byte-spec m))
	)
    (values (if ( x 20)
		x
		(selector x = (31 :pass)(30 :handicap)(29 :sequence)(28 :join)
			  (t (let ((err (format nil "Bogus X byte ~A" x)))
			       (if error-p (signal-error (err)))
			       err))))
	    (if (= y 31) nil (if (= y 30) :ko y))
	    (selector com =
	      (0 :funny)
	      (1 :black)
	      (2 :white)
	      (3 :empty)
	      (4 :border)
	      (5 'add-member)
	      (6 'remove-member)
	      (t (let ((err (format nil "Bogus com byte ~A" com)))
		   (if error-p (signal-error (err)))
		   err)))
	    (decode-move-number m))))
;;
;; This utility decodes an encoded list, so we can see what it refers to.
;;
(defun 2cs0 (cached-sequence)
  (if (not (consp
		cached-sequence))
      (cs1 cached-sequence)
      (loop for i in cached-sequence
	    collect
	      (cs1 i))))

(defun 2csa0 (cached-sequence-array)
  (loop for i being the array-elements of cached-sequence-array
	collect (cs1 i)))

(defun 2cs10 (i)
  (cond ((consp i) (cs i))
	((arrayp i)(csa i))
	((integerp i)
	 (multiple-value-bind (x y c mn)
	     (decode-encoded-move i)
	   `(,x ,y ,c ,@(when mn `(,mn)))))
	(t i)))


(defmacro 2encode-set-change0 (x y method)
  `(dpb (case ,method
	  (add-member 5)
	  (remove-member 6)
	  (otherwise (signal-error ("can't encode method ~A" ,method))))
	com-byte-spec
	(dpb -1 com-move-number-byte-spec
	     (dpb ,x com-x-byte-spec ,y))))

(defmacro 2decode-set-change0 (val)
  `(values
     (ldb com-x-byte-spec ,val)
     (ldb com-y-byte-spec ,val)
     (selector (ldb com-byte-spec ,val) =
	       (5 #'add-member)
	       (6 #'remove-member)
	       (t (signal-error ("Bogus encode change function ~A"
				 (ldb com-byte-spec ,val)))))
     (decode-move-number ,val)
     ))

(defmacro 2encode-color0 (color)
  `(case ,color
		(:black 1)(:white 2)(:empty 3)(:border 4)(:funny 0)
	    (otherwise (signal-error ("can't encode color ~A" ,color)))))

(defmacro 2decode-encoded-color 0(val)
  `(selector ,val =
     (1 :black)(2 :white)(3 :empty)(4 :border)(0 :funny)
     (t (signal-error ("Bogus color ~A" ,val)))))

(defmacro 2encode-move-spec0 (x y color &optional move-number)
  (if move-number
      `(dpb (or ,move-number -1)
	    com-move-number-byte-spec
	    (dpb (encode-color ,color)
		 com-byte-spec
		 (dpb (if (integerp ,x) ,x
			  (case ,x (:pass 31)(:handicap 30)(:sequence 29)(:join 28)))
		      com-x-byte-spec
		      (dpb (if (eq ,y nil) 31 (if (eq ,y :ko) 30 ,y))
			   com-y-byte-spec
			   0))))

      `(dpb (encode-color ,color)
	    com-byte-spec
	    (dpb (if (integerp ,x) ,x
		     (case ,x (:pass 31)(:handicap 30)(:sequence 29)(:join 28)))
		 com-x-byte-spec
		 (dpb (if (eq ,y nil) 31 (if (eq ,y :ko) 30 ,y))
		      com-y-byte-spec
		      (dpb -1 com-move-number-byte-spec 0)
		 ))))
  )

(defmacro 2encode-xy0 (x y) `(dpb ,x (byte 8 8) ,y))
(defmacro 2decode-xy0 (z) `(values (ldb (byte 8 8) ,z)(ldb (byte 8 0) ,z)))

(defmacro 2decode-move-spec0 (val &optional (move-number-too t))
  `(let ((xs (ldb com-x-byte-spec ,val))
	 (ys (ldb com-y-byte-spec ,val))
	 )
     (values (if ( xs 27) xs
		 (selector xs = (31 :pass)(30 :handicap)(29 :sequence)(28 :join)))
	     (if (= ys 31) nil (if (= ys 30) :ko ys))
	     (decode-encoded-color (ldb com-byte-spec ,val))
	     ,@(when move-number-too
		 `((decode-move-number ,val)))
	     )))


(defsubst 2encode-zone-affiliation0 (zonen subzonen)
  (sys:%logdpb zonen (byte 4 4) subzonen))
(defsubst 2decode-zone-affiliation0 (n)
  (values (ldb (byte 4 4) n) (ldb (byte 4 0) n)))

3;;
;; hack for where it is useful to encode x and y in a single number
;;
0(defmacro2 square-id0 (x y) `(+ (* ,x 100.) ,y))
(defmacro 2x-co0 (id) `(floor ,id 100.))
(defmacro 2y-co0 (id) `(rem ,id 100.))


(defun 2encode-move-geometry0 (move x-pos y-pos) 
  (lsh 1 (+ (case move
		( :first 0) (otherwise 1))
	    (case x-pos
		( :left 0) ( :center 2) (otherwise 4))
	    (case y-pos
		( :top 0) ( :center 6) (otherwise 12)))))

(defun 2decode-move-geometry0 (enc &optional how)
  (let* ((bp (- (integer-length enc) 1))
	 (move (if (evenp bp) :first :second))
	 (prim (floor bp 2)))
    (if (eq how :short-string)
	(values move
		(selector prim =
		  (0 #"NW") (1 #"N") (2 #"NE")
		  (3 #"W") (4 #"C") (5 #"E")
		  (6 #"SW") (7 #"S") (8 #"SE")
		  ))
	(values move
		(cond ((member prim '(0 3 6)) :left)
		      ((member prim '(1 4 7)) :center)
		      ((member prim '(2 5 8)) :right)
		      (t (signal-error ("~A is a bad direction primitive.  Expected 0-8"))))
		(cond ((member prim '(0 1 2)) :top)
		      ((member prim '(3 4 5)) :center)
		      ((member prim '(6 7 8)) :bottom)))
	)))


(defun 2decode-move-geometries0 (arg &optional (how :short-string))
  (let ((val (loop with res
		   until (zerop arg)
		   as this-pos = (logand arg (- arg))
		   do
	       (setq arg (logxor arg this-pos))
	       (multiple-value-bind (move direction ydir)
		   (decode-move-geometry this-pos how)
		 (when (null how) (setq direction (list move direction ydir)))
		 (unless (member direction res :test #'string-equal)
		   (push direction res)))
		   finally (return res))))
    (if (eq how :short-string)
	(sort  val #'string-lessp)
	val)))


(defmacro 2pack-border0 (border)
  `(case ,border
		(:friendly 0) (:empty 1) (:unfriendly 2)
	    (otherwise (signal-error ("Bad code ~A" ,border)))))
(defmacro 2unpack-border0 (border)
  `(selector ,border = (0 :friendly)(1 :empty )(2 :unfriendly)))

(defmacro 2unpack-classification0 (key)
  `(values
     (ldb (byte 4 0) ,key) ;row
     (ldb (byte 5 4) ,key) ;width 
     (ldb (byte 5 9) ,key) ;height
     (- (ldb (byte 4 22) ,key) 8);dx
     (- (ldb (byte 4 26) ,key) 8);dy
     (unpack-border (ldb (byte 2 20) ,key));left
     (unpack-border (ldb (byte 2 14) ,key));top
     (unpack-border (ldb (byte 2 18) ,key));right
     (unpack-border (ldb (byte 2 16) ,key));bottom
     ))

(defmacro 2pack-classification0 (row width height dx dy
			       left-border top-border right-border bottom-border)
  `(dpb (+ ,dy 8) (byte 4 26)
	(dpb (+ ,dx 8) (byte 4 22)
	     (dpb ,left-border (byte 2 20)
		  (dpb ,right-border (byte 2 18)
		       (dpb ,bottom-border (byte 2 16)
			    (dpb ,top-border (byte 2 14)
				 (dpb ,height (byte 5 9)
				      (dpb ,width (byte 5 4) ,row))))))))
  )

#||

(defun test-pack ()
  (loop for (dx dy) in '((0 0) (7 -7)) do
  (loop for l in '(:empty :unfriendly :friendly)
	as left = (pack-border l) do
  (loop for to in '(:empty :unfriendly :friendly)
	as top = (pack-border to)
	do
  (loop for r in '(:empty :unfriendly :friendly)
	as right = (pack-border r)
	do
  (loop for b in '(:empty :unfriendly :friendly)
	as bottom = (pack-border b)
	do
  (loop for width in '(1 19) do
  (loop for height in '(1 10) do
  (loop for row in '(1 10)
	as key = (pack-classification row width height dx dy
				      left top right bottom)
	do
    (multiple-value-bind (p-row p-width p-height p-dx p-dy p-left p-top p-right p-bottom)
	(unpack-classification key)
      (unless (and (eql row p-row)
		   (eql height p-height)
		   (eql width p-width)
		   (eql l p-left)
		   (eql to p-top)
		   (eql r p-right)
		   (eql b p-bottom)
		   (eql dx p-dx)
		   (eql dy p-dy))
	(signal-error ("unpack mismatch")))))))))))))

||#


#||
5Encoded fates of positions
0as stored in new-shape-data.

||#
(defconstant 2new-fate-byte-spec0 (byte 5 0))
(defconstant 2new-ord-byte-spec0 (byte 4 5))
(defconstant 2new-np-byte-spec0 (byte 3 9))
(defconstant 2new-libs-byte-spec0 (byte 4 12))
(defconstant 2new-libs2-byte-spec0 (byte 6 16))
(defconstant 2new-aux-byte-spec0 (byte 4 22))
(defconstant 2new-pv-byte-spec0 (byte 5 26))

(defun 2decode-ordinal-move 0(int)
  (let ((ord (ldb new-ord-byte-spec int)))
    (cond ((= ord 0) nil)
	  (( ord 13) ord)
	  ((= ord 14) :pass)
	  ((= ord 15) :outside)
	  (t (signal-error ("Nonsensical value ~D for ORD" ord))))))

(defun 2encode-ordinal-move0 (ord fate)
  (dpb (if (integerp ord) ord
	   (case ord
	     ((nil) 0)
	     (:pass 14)
	     (:outside 15)
	     (otherwise (signal-error ("Cannot encode ordinal number ~A" ord)))))
       new-ord-byte-spec
       fate))

(defun 2new-decode-fate0 (int)
  (declare (values (fate ord number-adjacent libs libs2 aux)))
  (if (not (integerp int))
      (values :unknown :pass 1 0 0 NIL NIL)
      (let ((fate (ldb new-fate-byte-spec int))
	    (aux (ldb new-aux-byte-spec int))
	    (ord (ldb new-ord-byte-spec int))
	    (np (ldb new-np-byte-spec int))
	    (libs (ldb new-libs-byte-spec int))
	    (max-libs (ldb new-libs2-byte-spec int))
	    (pv (ldb new-pv-byte-spec int))
	    )
	(values (selector fate =
		  (0 :alive) (1 :alive-in-ko) (2 :seki)
		  (3 :dead) (4 :dead-in-ko)
		  (5 :repetition) (6 :indeterminate) (7 :no-eyes)
		  (8 :alive-with-eye) (9 :dead-with-eye) (10 :unknown)
		  (11 :impossible)
		  (t (signal-error ( "Cannot decode ~d for fate" fate))))
		(cond ((= ord 0) nil)
		      (( ord 13) ord)
		      ((= ord 14) :pass)
		      ((= ord 15) :outside)
		      (t (signal-error ("Nonsensical value ~D for ORD" ord))))
		np
		libs max-libs
		(cond ((= aux 0) nil)
		      ((= aux 1) :ko)
		      ((= aux 2) :no-moves)
		      ((= aux 3) :outnumbered)
		      ((= aux 4) :numerical-superiority)
		      ((= aux 5) :benson)
		      (t (signal-error ("Nonsensical value ~D for AUX" aux))))
		(cond ((= pv 0) nil)
		      (t (1- pv)))
		))))

(defun 2new-encode-fate0 (fate ord np &optional libs max-libs aux pv)
  (dpb (if pv (1+ pv) 0)
       new-pv-byte-spec
       (dpb (or max-libs 0)
	    new-libs2-byte-spec
	    (dpb (or libs 0)
		 new-libs-byte-spec
		 (dpb np
		      new-np-byte-spec
		      (dpb (if (integerp ord) ord
			       (case ord
				 ((nil) 0)
				 (:pass 14)
				 (:outside 15)
				 (otherwise (signal-error ("Cannot encode ordinal number ~A" ord)))))
			   new-ord-byte-spec
			   (dpb
			     (case aux
			       ((nil) 0)
			       (:ko 1)
			       (:no-moves 2)
			       (:outnumbered 3)
			       (:numerical-superiority 4)
			       (:benson 5)
			       (otherwise (signal-error ("Cannot encode aux info ~a" aux))))
			     new-aux-byte-spec
			     (case fate
			       (:alive 0) (:alive-in-ko 1) (:seki 2)
			       (:dead 3) (:dead-in-ko 4)
			       (:repetition 5) (:indeterminate 6)
		(:no-eyes 7)
			       (:alive-with-eye 8)
			       (:dead-with-eye 9)
			       (:unknown 10)
			       (:impossible 11)
			       (otherwise (signal-error ("Cannot encode fate ~A" fate)))))))))))


(defun cf (a) (if (consp a) (mapcar #'cf a) (multiple-value-list (new-decode-fate a))))

(defun 2fate-for-total-liberties0 (encoded-fate total-libs)
  3;; given an encoded fate, which may be a list, determine which if
0  3;; and of them represents the fate of  groups with the specified number
0  3;; of total liberties.  If the group has more than the most we have
0  3;; specific data for, return the last one.
0  (cond ((consp encoded-fate)
	 (loop for fp on encoded-fate
	       as (fa) = fp
	       do
	   (when (or ( (ldb new-libs-byte-spec fa) total-libs (ldb new-libs2-byte-spec fa))
		     (unless (cdr fp) (> total-libs  (ldb new-libs2-byte-spec fa))))
	     (return (values fa fp))
	     )))
	((integerp encoded-fate)
	 (when (or ( (ldb new-libs-byte-spec encoded-fate)
		      total-libs
		      (ldb new-libs2-byte-spec encoded-fate))
		   (> total-libs (ldb new-libs2-byte-spec encoded-fate)))
	   encoded-fate))
	))

(defun 2merge-fates0 (old-fate new-fate)
  (multiple-value-bind (new change)
      (block change-fate 
	(when (null old-fate) (return-from change-fate (values new-fate t)))
	(multiple-value-bind (old-val pt)
	    (fate-for-total-liberties old-fate (ldb new-libs-byte-spec new-fate))
	  (when old-val
	    (multiple-value-bind (f1 ord1 np1 libs1 libs12 aux1 pv1) (new-decode-fate old-val)
	      (multiple-value-bind (f2 ord2 nil libs2 libs22 aux2 pv2) (new-decode-fate new-fate)

		(when (or (null f1)
			  (and (eq f1 :unknown)
			       (eql libs1 0)))
		  (return-from change-fate (values new-fate t)))

		(when (and (eql f1 f2)
			   (eql ord1 ord2)
			   (eql aux1 aux2)
						;don't check np
			   ( (or libs1 0) (or libs2 0) (or libs12 0))
			   ( (or libs1 0) (or libs22 0) (or libs12 0))
			   (eql pv1 pv2)
			   )
						3;no change
0		  (return-from change-fate (values old-fate nil)))

		(let ((new-list)
		      )

						3;some change
0		  (if (eql libs1 libs2)
		      (if ( libs22 libs12)
		      
						3;completely replaces old
0			  (push new-fate new-list
					)  
		      
						3;splice before old element
0			  (push new-fate new-list
					)
			  (push (new-encode-fate f1 ord1 np1 (1+ libs22) libs12 aux1 pv1)
				new-list
				)
			  (setq libs22 libs12))
						3; place after old element
0		      (if (and (eql libs2 (1+ libs12))
			       (eql ord1 ord2)
			       (eql aux1 aux2)
			       (eql f1 f2)
			       (eql pv1 pv2))
			  (push (new-encode-fate f1 ord1 np1 libs1 libs22 aux1 pv1) new-list
					)

			  (push (new-encode-fate f1 ord1 np1 libs1 (1- libs2) aux1 pv1)
				new-list
				)
			  (push new-fate new-list
					)))

		  (loop for new-elp on (cdr pt)
			as (new-el) = new-elp
			do
		    (multiple-value-setq (f1 ord1 np1 libs1 libs12 aux1)
		      (new-decode-fate new-el))
		    (when (> libs12 libs22)
		      (push (new-encode-fate f1 ord1 np1 (1+ libs22) libs12 aux1 pv1)
			    new-list
			    )
		      (loop for i in (cdr new-elp) do (push i new-list
								    ))
		      (loop-finish)
		      ))

		  (when new-list
		    (setq new-list (nreverse new-list))
		    (setq new-list
			  (nconc (loop with first-of-new = (car new-list)
				       for p on (when (consp old-fate) old-fate)
				       until (eq p pt)
				       as prev = (first p)
				       collect
					 (or (and (= (ldb new-libs2-byte-spec prev)
						     (1- (ldb new-libs-byte-spec first-of-new)))
						  (let ((nb1 (dpb (ldb new-libs2-byte-spec first-of-new)
								  new-libs2-byte-spec
								  prev))
							(nb2 (dpb (ldb new-libs-byte-spec prev)
								  new-libs-byte-spec
								  first-of-new)))
						    (when (eql nb1 nb2)
						      (setq new-list (cdr new-list))
						      nb1)))
					     prev))
				 new-list))
		    )

		  (let ((val (if (null (cdr new-list))
				 (car new-list)
				 new-list)))
		    (if (consp val)
			(loop for v1 in val
			      as v2 in (cdr val)
			      do
			  (multiple-value-bind (nil nil nil libs1 libs12)
			      (new-decode-fate v1)
			    (multiple-value-bind (nil nil nil libs2 libs22)
				(new-decode-fate v2)
			      (unless (and (> libs2 libs12)
					   ( libs12 libs1)
					   ( libs22 libs2))
				(signal-error ("Liberties confused"))))))
			(unless ( (ldb new-libs2-byte-spec val )
				   (ldb new-libs-byte-spec val))
			  (signal-error ("Liberties confused"))))			
		    (values val
			    t))
		  ))))
	  ))
    (when (null new) (signal-error ("Merge fates produced nil")))
    (values new change))
  )
      
(defun 2fate-for-outside-liberties0 (encoded-fate outside-liberties)
  3;; given an encoded fate, which may be a list, determine which if
0  3;; and of them represents the fate of the specified number of outside
0  3;; liberties.  This is a "exact only" match, so if the fate hasn't been
0  3;; calculated at all we return NIL
0  (cond ((consp encoded-fate)
	 (loop with ff = (first encoded-fate)
	       with *print-base* = (ldb new-libs-byte-spec ff)
	       for fa in encoded-fate
	       do
	   (when ( (- (ldb new-libs-byte-spec fa) *print-base*)
		    outside-liberties
		    (- (ldb new-libs2-byte-spec fa) *print-base*))
	     
	     (return (values fa *print-base*))
	     )
	       finally (return (values nil *print-base*))))
	((integerp encoded-fate)
	 (when (  outside-liberties
		  (- (ldb new-libs2-byte-spec encoded-fate)
		     (ldb new-libs-byte-spec encoded-fate)
		     ))
	   (values encoded-fate (ldb new-libs-byte-spec encoded-fate))
	   )
	 )))

(defun 2last-fate-for-outside-liberties0 (encoded-fate)
  3;; given an encoded fate, which may be a list, determine which if
0  3;; and of them represents the fate of the specified number of outside
0  3;; liberties.  This is a "exact only" match, so if the fate hasn't been
0  3;; calculated at all we return NIL
0  (cond ((consp encoded-fate)
	 (let* ((ff (first encoded-fate))
		(*print-base* (ldb new-libs-byte-spec ff))
		(fa (car (last encoded-fate))))
	   (values fa (- (ldb new-libs2-byte-spec fa) *print-base*))
	   ))
	((integerp encoded-fate)
	 (values encoded-fate (- (ldb new-libs2-byte-spec encoded-fate)
				 (ldb new-libs-byte-spec encoded-fate)
				 )))
	 ))

(defun 2fate-for-outside-liberties-if-different0 (encoded-fate outside-liberties)
  (multiple-value-bind (fa *print-base*)
      (fate-for-outside-liberties encoded-fate outside-liberties)
    (if (and fa
	     (or (eql *print-base* outside-liberties)
		 (eql (+ *print-base* outside-liberties)
		      (ldb new-libs-byte-spec fa))))
	fa
	nil)))

#||
;; a little test for encode/decode isomorphism
(defun test ()
  (loop for fate in '(:alive :alive-in-ko :dead :dead-in-ko :seki
			     :repetition :indeterminate :no-eyes
			     :alive-with-eye :dead-with-eye
			     :unknown :impossible)
	do	
    (loop for ord in '(nil :pass :outside 1 10) do
      (loop for aux in '(nil :ko :no-moves :outnumbered :numerical-superiority :benson)
	    do
	(loop for np in '(0 1 7) do
	  (loop for libs in '(0 1 15) do
	    (loop for max-libs in '(0 1 63) do
	      (loop for pv in '(nil 0 20)
		  as enc = (new-encode-fate fate ord np libs max-libs aux pv)
		  do
	      (multiple-value-bind (r-fate r-ord r-np r-libs r-max-libs r-aux r-pv)
		  (new-decode-fate enc)
		(unless (and (eql fate r-fate)
			     (eql r-ord ord)
			     (eql r-aux aux)
			     (eql r-np np)
			     (eql r-libs libs)
			     (eql r-pv pv)
			     (eql r-max-libs max-libs))
		  (signal-error ("no match"))))))))))))
||#


(defmacro 2xy-quadrant0 (x y &key (q0 0)(q1 1)(q2 2)(q3 3)(fbs 'full-board-size))
  3;divide the board into four triangular quadrants, centered on the corners and center
0  3;do stuff depending on which quadrant.   The basic classifications depend on this
0  3;to orient themselves with "down" pointing to the edge.
0  `(if (or (< ,x ,y)
	   (and (= ,x ,y) (or (< (+ ,x ,x) ,fbs)(< (+ ,y ,y) ,fbs))))

       (if (< (- ,fbs ,x) ,y)
		   
	   ,q0
		   
	   ,q1
	   )
       (if (< (- ,fbs ,x -1) ,y)
		   
	   ,q3
		   
	   ,q2)))

(defmacro 2with-deltas0 (quad core mod)
  (let ((b))
    `(progn
       ,(loop for (dx dy) in
		  (case quad
		    (0 '((-1 -1)(0 -1)(1 -1)(-1 0)(1 0)(-1 1)(0 1)(1 1)))
		    (1 '((1 -1)(1 0)(1 1)(0 -1)(0 1)(-1 -1)(-1 0)(-1 1)))
		    (2 '((1 1)(0 1)(-1 1)(1 0)(-1 0)(1 -1)(0 -1)(-1 -1)))
		    (3 '((-1 1)(-1 0)(-1 -1)(0 1)(0 -1)(1 1)(1 0)(1 -1))))
	      as i from 0 by mod
	      as form = `(,core ,dx ,dy)
	      do (if (null b)
		     (setq b form)
		     (setq b `(sys:%logdpb ,form (byte ,mod ,i) ,b)))
	      finally (return b)))))

(defmacro 2with-deltas--classify0 (dx dy)
  `(let ((color (color (aref ar (+ (* span (+ ,dx x)) ,dy y)))))
     (cond ((eq color :empty) 0)
	   ((eq color target-color) 1)
	   ((eq color :BORDER) 3)
	   (t 2))))


(defun 2-classify-position-for-board0 (x y target-color full-board-size positions-occupied)
3  ;;
0  3;; Classify the 3x3 neighborhood surrounding X,Y
0  3;;
0  (let* ((ar positions-occupied)
	 (span (si:array-row-span ar)))
    (declare (sys:array-register-1d ar))
      
    (multiple-value-bind (quad desc)
	(xy-quadrant
	  x y
	  :q0 (values 0 (with-deltas 0 with-deltas--classify 2))
	  :q1 (values 1 (with-deltas 1 with-deltas--classify 2))
	  :q2 (values 2 (with-deltas 2 with-deltas--classify 2))
	  :q3 (values 3 (with-deltas 3 with-deltas--classify 2)))
      (values desc quad)
      )))

(defmacro 2with-deltas-z-classify0 (dx dy)
  `(let* ((gr  (aref ar (+ (* span (+ ,dx x)) ,dy y)))
	  (cl (color gr)))
     (cond ((eq cl :empty) 0)
	   ((eq cl :BORDER) 7)
	   (t (let* ((saf (group-safety-estimate gr)))
		(cond ((eql cl target-color)
		       (cond ((< saf 0.19)
			      4)
			     ((< saf 0.4)
			      5)
			     (t 6)))
		      ((< saf 0.19) 1)
		      ((< saf 0.4)  2)
		      (t 3)))
	      ))))

(defun 2z-classify-position-for-board0 (x y target-color full-board-size positions-occupied)
3  ;;
0  3;; Classify the 3x3 neighborhood surrounding X,Y
0  3;;
0  (let* ((ar positions-occupied)
	 (span (si:array-row-span ar)))
    (declare (sys:array-register-1d ar))
      
    (multiple-value-bind (quad desc)
	(xy-quadrant
	  x y
	  :q0 (values 0 (with-deltas 0 with-deltas-z-classify 3))
	  :q1 (values 1 (with-deltas 1 with-deltas-z-classify 3))
	  :q2 (values 2 (with-deltas 2 with-deltas-z-classify 3))
	  :q3 (values 3 (with-deltas 3 with-deltas-z-classify 3)))
      (values desc quad)
      )))

(defmacro 2with-deltas-r-classify0 (dx dy)
  `(let* ((ax (+ ,dx x))
	  (ay (+ ,dy y))
	  (gr  (aref ar (+ (* span ax) ay)))
	  (cl (color gr)))
     (cond ((eq cl :empty) 
	    (let ((val (weighted-influence-classify self ax ay)))
	      (cond ((> val 0.02) 8)
		    ((> val -0.02) 9)
		    (t 7))))
	   ((eq cl :BORDER) 0)
	   (t (let* ((saf (group-safety-estimate gr)))
		(cond ((eql cl target-color)
		       (cond ((< saf 0.19)
			      4)
			     ((< saf 0.4)
			      5)
			     (t 6)))
		      ((< saf 0.19) 1)
		      ((< saf 0.4)  2)
		      (t 3)))
	      ))))

(defun 2r-classification-n-contacts 0(class &optional detailed)
  (loop with count = 0 
	with details
	repeat 8 
	for lclass = class then (lsh lclass -4)
	as item = (ldb (byte 4 0) lclass )
	do (cond ((eql item 0) (if detailed (push :border details)))
		 ((>= item 7) (if detailed (push :empty details)))
		 (t (push :colored details)
		    (incf count)))
	finally (return (values count details))))

(defun 2r-classify-position-for-board0 
       (self x y target-color full-board-size positions-occupied )
3  ;;
0  3;; Classify the 3x3 neighborhood surrounding X,Y
0  3;;
0  (let* ((ar positions-occupied)
	 (span (si:array-row-span ar)))
    (declare (sys:array-register-1d ar))
      
    (multiple-value-bind (quad desc)
	(xy-quadrant
	  x y
	  :q0 (values 0 (with-deltas 0 with-deltas-r-classify 4))
	  :q1 (values 1 (with-deltas 1 with-deltas-r-classify 4))
	  :q2 (values 2 (with-deltas 2 with-deltas-r-classify 4))
	  :q3 (values 3 (with-deltas 3 with-deltas-r-classify 4)))
      (values desc quad)
      )))

(define-presentation-type 2float-or-nil0 ()
   :abbreviation-for '(or float (eql nil)))

(define-presentation-type 2fixnum-or-nil0 ()
   :abbreviation-for '(or fixnum (eql nil)))

(define-presentation-type 2function-or-nil0 ()
   :abbreviation-for '(or function (eql nil)))

(define-presentation-type 2stone-coordinate0 ()
   :parser ((stream)  
	    (let ((val (read stream)))
	      (cond ((and (integerp val)
			  (<= 1 val 19))
		     val)
		    ((symbolp val)
		     (let* ((sval (string val))
			    (fc (aref sval 0))
			    (fv (parse-position-char fc nil)))
		       (values (and (eql (length sval) 1)
				    fv)
			       'letter-stone-coordinate)))))))

(define-presentation-type 2stone-coordinate-pair0 ()
      :parser ((stream)
	       (multiple-value-bind (xc xtype)
		   (accept 'stone-coordinate :prompt "X coord :" :stream stream)
		 (when xc
		   (multiple-value-bind (yc ytype)
		       (accept 'stone-coordinate :prompt "Y coord :" :stream stream)
		     (ignore ytype)
		     (when (and xc yc (eql xtype 'letter-stone-coordinate))
		       (setq yc (- 20 yc)))
		     (when (and xc yc)
		       (list xc yc)))))))

(define-presentation-type 2go-database-mode0 ()
   :abbreviation-for '(member :tandem nil :read-only :only))

(defmacro simple-time (var form)
  `(let ((now (get-internal-run-time))
	 (val ,form)
	 (later (get-internal-run-time)))	
     (incf ,var (- later now))
     val))

;
;verify that old and new return the same value, and gather
;some statistics about which is faster
;
(defvar *comparisons* nil)
(defmacro compare-versions ((name &key (test 'eql) use-only (nvalues 1)) first second)
  (ecase use-only
    (:first first)
    (:second second)
    ((nil)
      (let ((first-name (intern (format nil "~A-FIRST" name)))
	    (second-name (intern (format nil "~A-SECOND" name)))
	    (name-calls (intern (format nil "~A-CALLS" name)))
	    (first-value-names (loop for i from 1 to nvalues collect
				     (intern (format nil "~A-FIRST-VAL-~D" name i))))
	    (second-value-names (loop for i from 1 to nvalues collect
				     (intern (format nil "~A-SECOND-VAL-~D" name i))))
	    )
	(set second-name 0)
	(eval `(progn (defvar ,first-name 0)
		     (defvar ,second-name 0)
		     (defvar ,name-calls 0)))
	(pushnew (list name-calls first-name second-name) *comparisons* :test #'equal)
	`(let* ((time-now (get-internal-run-time))
		(odd-time (oddp time-now))
		,@first-value-names first-time ,@second-value-names second-time)

3	   ;randomize the order in which the alternatives are executed, so any
0	   3;penalty for page faults is spread fairly.  This contains forms
0	   3;"considered harmful", but there isn't any way to do it within
0	   3;structured orthodoxy.
0	   (tagbody (when odd-time (go part2))
		 part1
		    (multiple-value-setq ,first-value-names ,first)
		    (setq first-time (get-internal-run-time))
		    (when odd-time (go part3))
		 part2
		    (multiple-value-setq ,second-value-names ,second)
		    (setq second-time (get-internal-run-time))
		    (when odd-time (go part1))
		 part3
		    (cond (odd-time
			   (incf ,second-name (- second-time time-now))
			   (incf ,first-name (- first-time second-time)))
			  (t (incf ,second-name (- second-time first-time))
			     (incf ,first-name (- first-time time-now))))
		    )
	   (incf ,name-calls)
	   (if (and ,@(loop for i in first-value-names 
			    as j in second-value-names
			    collect `(,test ,i ,j) ))
	       (values ,@first-value-names)
	       (signal-error ("~D result differs, first =~A ; second = ~A"
			      ',name 
			      (list ,@first-value-names)
			      (list ,@second-value-names ))
		 ((:ignore :first-alternative) 
		  (values ,@first-value-names))
		 (:second-alternative 
		  (values ,@second-value-names))
		 )))))))

(defun show-comparisons ()
  (loop for (calls old new) in *comparisons*
	as ncalls = (eval calls)
	as fncalls =  (float (max 1 ncalls))
	do (format t "~&~A ~4f  first ~5f  second ~5f"
		   calls ncalls
		   (/ (eval old) fncalls)
		   (/ (eval new) fncalls))))

(defmacro trap ((condition msg &rest args) &body body)
  `(progn
     (when (and enable-traps ,condition)
       (cl:break "TRAP: ~a" (format nil ,msg ,@args)))
     ,@body)
     )

(defun sets-equal (a b &key (test #'equal-enough))
  (cond ((and (consp a)
	      (consp b))
	 (and (eql (length a )(length b))
	      (null (set-difference a b :test test))))
	((consp a) nil)
	((consp b) nil)
	(t (funcall test a b))
	))


(defvar %hash-compare% :first) ;nil :first or :second.  
     ; :First is new hash compare.  :second is old cons based

(eval-when (eval load compile)
(defvar zhash-seed 9112443))
(defvar zhash-white-seed 181448578)
(defvar zhash (si:random-create-array (* 19 19) 0 zhash-seed *go-static-area*))
(defvar zhash-white (si:random-create-array (* 19 19) 0 zhash-white-seed *go-static-area*))

(defun update-zhash (old-hash index old-color new-color)
  (when (>= index (* 19 19)) (setq index (mod index (* 19 19))))
  (let ((h old-hash))

    (cond ((eql old-color :empty))
	  ((eql old-color :black) (setq h (logxor h (aref zhash index))))
	  ((eql old-color :white) (setq h  (logxor h (aref zhash-white index))))
	  (t (signal-error ("should be black white or empty"))))

    (cond ((eql new-color :empty))
	  ((eql new-color :black) (setq h (logxor h (aref zhash index))))
	  ((eql new-color :white) (setq h  (logxor h (aref zhash-white index))))
	  (t (signal-error ("should be black white or empty"))))
    h))