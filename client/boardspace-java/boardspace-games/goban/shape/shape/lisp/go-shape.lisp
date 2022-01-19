D,#TD1PsT[Begin using 006 escapes](0 0 (NIL 0) (NIL NIL NIL) "CPTFONT")(1 0 (NIL 0) (NIL :BOLD-EXTENDED NIL) "CPTFONTB")(2 0 (NIL 0) (NIL :ITALIC NIL) "CPTFONTI")0;; -*- Mode: LISP; Package: GO; Base: 10; Fonts: CPTFONT,CPTFONTB,CPTFONTI; Syntax: Common-lisp -*-

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; The basic things to build a shape library
;;
;; we currently ennumerate all possible shapes of sizes
;; 1-7, and recognise all isomers of each basic shape.
;; There are 164 of them, considering all isomers as one shape
;;
;;
;; The whole point is to recognise basic shapes without actually canonicalizing their
;; orientation and position.  To accomplish this we use two heuristics,
;;  1) Center of mass
;;  2) Mass within a specified distance of the center of gravity.
;; This file builds a descrimination network for all the small shapes, and verifies
;; that all isomers of all the shapes are correctly recognised.
;;
;; All this is to be used primarily for life-and-death type problems
;;
;;
;;  sending a SET-OF-STONES a SHAPE-DESCRIPTION does a lookup
;;
;;
;; The principle information in the library of shapes is information
;; about the number of eyes that shape can make.  We store information
;; for the massive product of
;;       164 shapes (3 0 (NIL 0) (NIL NIL :SMALL) "TVFONT")(plus some auxiliary shapes)
0;;        x 9 different positions on the board
;;          x 2^n different patterns of "dropped in" stones of the opponent color
;;           x 2 (moving first or second)
;;            x ~3  0-2 outside liberties, plus more for some shapes
;;
;; Some interesting numbers;
;;
;;
;; 560523 The library contains solved positions
;;  62003 (11%) irrelevant, impossible or unreachable positions.
;; 498220 (89%) reachable positions
;; 375986 (67%) are static - either alive or dead, not affected by sente
;; 107528 (28%) sente (plus ko threats) determines outcome
;;  15605 (4%) not killable (absolutely alive)
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defvar *fate-property* :fate)
(defvar fate-properties '(:fate))
(defvar fate-liberties '(0 1 2))
(defvar obsolete-fate-properties
	'(:fates :one-liberty-fates :two-liberties-fates))
(defvar obsolete-fate-properties-and-liberties
	'((:fates 0) (:one-liberty-fates 1) (:two-liberties-fates 2)))


(defvar interesting-shape-properties `( :name :comment :fate-comment :dropped-shapes
				       :key-points ,@fate-properties))
(defvar recently-changed-shapes nil)


(defflavor shape-info ((canonical-number)
		       (which-isomer 'bad-isomer)
		       (ordinal-member)
		       )
	   (set-of-stones)
 :initable-instance-variables
 (:conc-name nil)
 :writable-instance-variables
 )

(defmethod (drop-in-number shape-info) ()
  (let ((parent (parent-shape self)))
    (if (eq self parent)
	0
	(loop for (n p s) in (get-prop parent :dropped-shapes)
	      when (eq s self) return (values n p s)))))

(defmethod (find-drop-in-number shape-info) (n &optional (error-p t))
  (if (zerop n) self
      (loop for (in p s) in (get-prop self :dropped-shapes)
	    when (eql in n)
	      return (values s p in)
	    finally (when error-p (signal-error ("No intron number ~A of ~A" n self))
			  ))))

(zl:def-menu-method menu-shape-description shape-info
  :menu-id "shape description"
  :sort-key shape-description
  :button :mouse-middle
  :context :view
  :test (progn nil))

(defwhopper-subst (add-member shape-info) (x y)
  (let ((val (continue-whopper x y)))
    (when val (setq ordinal-member nil))
    val))
(defwhopper-subst (remove-member shape-info) (x y)
  (let ((val (continue-whopper x y)))
    (when val (setq ordinal-member nil))
    val))


(defwhopper-subst (do-for-all shape-info) (fun)
  (if ordinal-member
      (loop for i from 0 below size
	    as v = (aref ordinal-member i)
	    do (funcall fun (ldb (byte 8 8) v)(ldb (byte 8 0) v)))
      (continue-whopper fun)))

(def-memo-method parent-shape shape-info members ()
  (or (get-prop self :parent-shape) self))

(defmethod (do-for-introns shape-info) (fun bits)
  (declare (sys:downward-funarg fun))
  (loop with target = (parent-shape self)
	until (zerop bits)
	as low = (logand bits (- bits))
	as bitn = (integer-length low)
	do (setq bits (logxor bits low))
	   (multiple-value-bind (x y)
	       (member-for-ordinal-number target bitn)
	     (funcall fun x y))))

(defun-in-flavor (construct-ordinal-member shape-info) ()
  (let* ((i 0)
	 (alen size)
	 (ar (make-array alen :element-type '(unsigned-byte 16))))
    (do-for-all self
		#'(lambda (x y)
		    (setf (aref ar i)
			  (dpb x (byte 8 8) y))
		    (incf i)))
    (setq ordinal-member ar)))

(defmethod (member-for-ordinal-number shape-info) (n &optional (error-p t))
  (let ((ar (or ordinal-member
		(construct-ordinal-member))))
    (cond ((or ( n 0) (> n (array-total-size ar)))
	   (when error-p
	     (signal-error ("~a does not have ~a members" self n))))
	  (t (let ((val (aref ar (1- n))))
	       (values (ldb (byte 8 8) val) (ldb (byte 8 0)  val))
	       )))))

(defmethod (get-which-isomer shape-info) (of)
  (funcall which-isomer of))

(defmethod (bad-isomer shape-info) (&rest ignore)
  (signal-error ("undefined isomer function")))

(defmethod (sys:print-self shape-info) (stream printlvl slash)
  slash printlvl
  (si:printing-random-object (self stream :no-pointer)
    (format stream #"shape #~A ~A"
	    (or canonical-number center)
	    (or (send self ':name) ""))
    (let ((perm (permutation-number self)))
      (when perm (format stream " -~D" perm)))
    (format stream " ~\\shape\\" self)
    ))

(def-prop-method :name shape-info)
(def-prop-method key-points shape-info)
(def-prop-method shape-font-char shape-info )

(defmethod (intron-array-size shape-info) ()
  (or (get-prop self :prescribed-intron-array-size)
      size))

(defwhopper (picture shape-info) (&optional (introns 0) &rest args)

  (let* ((drop (drop-in-number self)))
    3;;compensate for the fact that dropped shapes have their introns specified
0    3;;in terms of their parent shapes.
0    (when drop
      (loop with new-introns = 0
	    and bit = 1
	    until (and (zerop introns) (zerop drop))
	    do (unless (logtest 1 drop)
		 (setq new-introns (logior new-introns (if (logtest introns 1) bit 0)))
		 (setq bit (lsh bit 1)))
	       (setq introns (lsh introns -1)
		     drop (lsh drop -1))
	    finally (setq introns new-introns)
	      )))
  (lexpr-continue-whopper introns args))

(defmethod (lengths-for-row shape-info) (row-num)
  (loop with lens
	for is in (get-all-isomers self)
	as len = (length-row-n is row-num)
	unless (or (member len lens)
		   (zerop len))
	  do (push len lens)
	     finally (return (sort lens #'>))))

(def-memo-method add-to-shape-font shape-info ( :newxz-font) ()
  
  (let* ((sys:font shape-font-1)
	 (idx shape-font-index)
	 (start (* idx (zl:font-char-width sys:font)))
	 )
    (when (< start (decode-raster-array sys:font))
      
      (multiple-value-bind (w h)
	  (square-size self)
	
	(let ((mag (min 3
			(floor (zl:font-baseline sys:font) w)
			(floor (1- (zl:font-char-width sys:font)) h))))
	  
	  (when (> mag 0)
	    (do-for-all self #'(lambda (x y)
				 (blacken-in-font self
						  (- y (first-row self))
						  (- x (first-col self))
						  sys:font mag start)))
	    (incf (zl:font-fill-pointer sys:font))
	    (set-shape-font-char
	      self
	      (code-char (send shape-character-set :make-char shape-font-index)))))
	(incf shape-font-index)
	)
      )))

(defmethod (blacken-in-font shape-info) (x y sys:font mag start)

  (loop for row from (+ start (* mag y))
	repeat mag
	do
	(loop for col from (* mag x)
	      repeat mag
	      do
	      (setf (raster-aref sys:font row col) -1))))
#|| 
;used just once to translate to ordinal format
(defmethod (translate-1-fate shape-info) (from)
  (multiple-value-bind (fate x y np l1 l2 ) (decode-fate from)
    (when (and (fixp x)(fixp y))
      (setq x (ordinal-number-for-member self x y)
	    y nil))
    (encode-fate fate x np l1 l2 y)))

(defmethod (translate-fate shape-info) (fate)
  (cond ((nlistp fate) (translate-1-fate self fate))
	((null (cdr fate)) (translate-1-fate self (car fate)))
	(t (loop for f in fate collect (translate-1-fate self f)))))
||#

(defmethod (make-init-list shape-info) (&key additional-properties printable)
  `(,(PICTURE self)
    ,@(loop with pv
	    for i in (append additional-properties interesting-shape-properties)
	    when  (setq pv (getf properties i))
	    nconc
	    (PROGN 
	      (cond ((eq i :key-points)
		     (setq pv (write-key-points self pv)))
		    #|| 
		    once only code to translate x,y to ordinal format
		    ((eq i :fate)
		     (setq pv (loop with parent = (parent-shape self)
				    for (key . val) in pv
				    as len = (array-length val)
				    as newval = (make-array len)
				    do
				(loop for i from 0 below len do
				  (setf (aref newval i)
					(translate-fate parent (aref val i))))
				    collect (cons key newval ))))
		    ||#
		    ((eq i :dropped-shapes)
		     (setq pv (loop for (introns positions shape) in pv
				    collect `(,introns ,positions
					      ,(make-init-list shape :printable printable
							       :additional-properties additional-properties))))))
	      (list i pv)
	      ))))


(defmethod (remove-unknowns shape-info) (&optional (props fate-properties))
  (loop with ufate = (new-encode-fate :unknown :pass 1 0 0 nil)
	for prop in props do
    (loop for (nil . ar ) in (get-prop self prop) do
      (loop for idx below (array-total-size ar)
	    as v = (aref ar idx)
	    if (eq v :unknown) do (setf (aref ar idx) ufate)
	    else
	      if (consp v)
		do
		  (loop for i on v
			when (eq (car i) :unknown)
			  do (setf (car i) ufate))
			  ))))


(defmethod (subshape-p shape-info) (other-shape)
  (when (= (size other-shape) (1+ size))
    (with-group-resource (cop 'set-of-stones)
      (copy-set other-shape nil cop)
      (block is-it
	(do-for-all
	  cop
	  #'(lambda (x y)
	      (declare (sys:downward-function))
	      (remove-member cop x y)
	      (loop for s in (get-all-isomers self) 
		    when (equal-offset-members cop s)
		      do (return-from is-it t))
	      (add-member cop x y)))))))



;;
;; make new sets, adding stones all the way around
;;
(defmethod (extend-shape shape-info) (x y)
  (let ((cp (copy-set self nil)))
    (add-member cp x y)
    (multiple-value-bind (w h)
	(square-size cp)
2      ;;normalize the orientation so height is more than width
0      (when (> w h)
	(rotate cp)))
    (add-shape zhash-shape-database cp)
    ))

(defmethod (extend-shape-drop-in shape-info) (x y)
  (let ((cp (copy-set self nil)))
    (remove-member cp x y)
    (add-shape zhash-shape-database cp)
    ))




	   
(defmethod (make-normalizer shape-info)
	   (target norm
		   &optional
		   (perm (funcall which-isomer target))
		   (shape (shape-description target))
		   &aux x y dx dy)

  (multiple-value-setq (x y) (first-member self))
  ;; determine permutation number
  (set-vars norm perm 0 0 shape)
  ;; determine first member based on permutation.  The unpermuted first member is
  ;; the stone with minimum y and minumum x.  Permuted this moves to some other
  ;; position in the representation.  This selects the right finder to locate the
  ;; permuted first member wherever it is.
  (multiple-value-setq (dx dy)
    (funcall (cdr (assoc perm
			    '((0 . miny-minx)
			      (1 . minx-miny)
			      (2 . miny-maxx)
			      (3 . minx-maxy) 
			      (4 . maxy-minx)
			      (5 . maxx-miny)
			      (6 . maxy-maxx)
			      (7 . maxx-maxy))))
	     target )) 
  ;; now that we know the location of the first member, we permute it
  ;; to get the net translation the normalization process will need to
  ;; correct for.
  (multiple-value-setq (dx dy)
    (transform-to-standard-coordinates norm dx dy))
  (set-vars norm perm (- x dx) (- y dy) shape)
  norm)


;3this is here to avoid compiler warnings
0(def-memo-method shape-normalizer set-of-stones-mixin members ()
  (multiple-value-bind (class perm)
      (shape-description-and-permutation self)
    (when class
      (make-normalizer class self
		       (or (cdr (getf properties 'remembered-shape-normalizer))
			   (make-instance 'normalizer ':target self
					  :area *go-cons-area*
					  ))
		       perm
		       class)
	    )
    ))


(defmethod (verify-normalizers shape-info) (&aux n)
  (loop for i in (get-all-isomers self) do
	(shift-by i -3 -2)
	(or (equal-members self (setq n (shape-normalizer i)))
	    (signal-error ("normalization failed for ~A of ~A" n self)))
	(shift-by i 3 2)
	(do-for-all self #'(lambda (x y)
			     (declare (sys:downward-function))
			     (verify-normal-transformation n x y)))))

;;
;; detect duplicate shapes, eliminating rotation, reflection, and translation
;;
(defmethod (is-duplicate-shape shape-info) (other)
  (cond ((neq size
	      (size other)) nil)
	((equal-sets self other))
	(t 
	 (loop for i in (get-all-isomers self) when
	       (equal-offset-members i other) do (return t)))))


(zl:def-menu-method get-all-isomers shape-info
  :button :mouse-middle
  :context :view
  :menu-id "get all isomers")

(defmethod (fixup-isomers shape-info) (&optional lst)
  (when (or lst (get-prop self :all-isomers))
    (loop for iso in (or lst (get-all-isomers self))
	  do
      (send iso :set-name (send self :name))
      (setf (canonical-number iso) canonical-number)
      (cond ((not put-isomers-in-shape-font)
	     (set-shape-font-char iso (shape-font-char self)))
	    ((shape-font-char iso))
	    (t (add-to-shape-font iso))))))

	
(def-memo-method get-all-isomers shape-info members ()
 
  (let (isomers)
    (labels ((accumulate-isomer (new)
	       (loop for i in isomers
		     when (equal-offset-members new i)
		       do (return nil)
		     finally (let ((nn (copy-set new nil)))
			       (set-permutation-number nn (permutation-number new))
			       (setf (canonical-number nn) canonical-number)
			       (push nn isomers)))))

      (push self isomers)
      (set-permutation-number self 0)
      (loop with new = (copy-set self nil)
	    and perm = 0
	    repeat 4 do
	(set-permutation-number new perm)
	(accumulate-isomer new)
	(mirror new)
	(setq perm (logxor perm 1))
	(set-permutation-number new perm)
	(accumulate-isomer new)
	(y-flip new)
	(setq perm (logxor (cond ((zerop (logand perm 1))
				  4)
				 (t 2))
			   perm))
	))
    (put-prop self t :all-isomers)
    (fixup-isomers self isomers)
    isomers))

(defmethod (get-isomer shape-info) (n)
  (loop for isom in (get-all-isomers self)
	when (= (permutation-number isom) n) return isom))


(defmethod (add-larger-shapes shape-info) ()
  (do-for-all-adjacent self #'(lambda (x y)
				(declare (sys:downward-function))
				(extend-shape self x y)))
  )

(defmethod (add-drop-in-shapes shape-info) ()
  (do-for-all self #'(lambda (x y)
		       (declare (sys:downward-function))
		       (extend-shape-drop-in self x y)))
  )

(defmethod (verify-shape-description shape-info) ()
  (loop for of in (get-all-isomers self) do
	(verify-set-shape-description of self)))

(defmethod (verify-drop-in-shape-description shape-info) ()
  (loop for of in (get-all-isomers self) do
	(verify-set-drop-in-shape-description of self)))

(defmethod (drop-in-positions shape-info) (xpos ypos)
  (let ((drop-ins))
    (labels ((is-good-liberty (x y)
	       (and (is-adjacent self x y)  ;adjacent to us
		    (case xpos 	    ;and inside the board
		      (:center t)
		      (:right ( x last-col))
		      (:left ( x first-col))
		      (otherwise (signal-error ("bad x pos"))))
		    (case ypos	    ;inside in both X and Y
		      (:center t)
		      (:bottom ( y last-row))
		      (:top ( y first-row))
		      (otherwise (signal-error ("bad y pos"))))
		    (not (in-cavity x y))   ;not part of a cavity formed between the main shape and the edge
		      ))
	     (in-cavity (x y &optional (xpos xpos)(ypos ypos))
	       (case xpos
		 (:center
		  (case ypos
		    (:center nil)
		    (:top (and (not (connected-path-exists x y first-col first-row))
			       (not (connected-path-exists x y last-col first-row))
			       (connected-path-exists x y nil (1- first-row))))
		    (:bottom (and (not (connected-path-exists x y first-col last-row))
				  (not (connected-path-exists x y last-col last-row))
				  (connected-path-exists x y nil (1+ last-row))))))
		 (:left
		  (case ypos
		    (:center (and (not (connected-path-exists x y first-col first-row))
				  (not (connected-path-exists x y first-col last-row))
				  (connected-path-exists x y (1- first-col) nil)))
		    (:top (if (is-member self first-col first-row)
			      (or (in-cavity x y :left :center)
				  (in-cavity x y :center :top))
			      (connected-path-exists x y first-col first-row)))
		    (:bottom (if (is-member self first-col last-row)
				 (or (in-cavity x y :left :center)
				     (in-cavity x y :center :bottom))
				 (connected-path-exists x y first-col last-row)))))
		 (:right
		  (case ypos
		    (:center (and (not (connected-path-exists x y last-col first-row))
				  (not (connected-path-exists x y last-col last-row))
				  (connected-path-exists x y (1+ last-col) nil)))
		    (:top (if (is-member self last-col first-row)
			      (or (in-cavity x y :right :center)
				  (in-cavity x y :center :top))
			      (connected-path-exists x y last-col first-row)))
		    (:bottom (if (is-member self last-col last-row)
				 (or (in-cavity x y :right :center)
				     (in-cavity x y :center :bottom))
				 (connected-path-exists x y last-col last-row)))
		    ))))
	     (connected-path-exists (fx fy tx ty &optional checked)
	       (cond ((is-member self fx fy) nil)
		     ((and (or (null tx) (eql tx fx))
			   (or (null ty) (eql ty fy)))
		      t)
		     ((< fx first-col) nil)
		     ((< fy first-row) nil)
		     ((> fx last-col) nil)
		     ((> fy last-row) nil)
		     ((member (+ fx (* fy 100)) checked) nil)
		     (t (push (+ fx (* fy 100)) checked)
			(or (connected-path-exists (1+ fx) fy tx ty checked)
			    (connected-path-exists (1- fx) fy tx ty checked)
			    (connected-path-exists fx (1+ fy) tx ty checked)
			    (connected-path-exists fx (1- fy) tx ty checked)))))
	     )
      (do-for-all self
		  #'(lambda (x y)
		      (declare (sys:downward-function))
		      (unless (zerop (delta-groups-around self x y)) ;this might be a split
			(unless (or (is-good-liberty (1+ x) y)  ;a "good liberty" is one outside the shape
				    (is-good-liberty  x (1+ y)) ;from which one can reach any part of the shape
				    (is-good-liberty (1- x) y)  ;screw case is where the shape has a cavity
				    (is-good-liberty x (1- y)))
			  (push (list x y) drop-ins)))))
      drop-ins)))

#||

(4 0 (NIL 0) (NIL :BOLD NIL) "CPTFONTCB")This is just one of those occasional things ... In the course of solving
a position, I generated a legitimate drop-in shape that wasn't in the database.
This led to the discovery that there were some additional drop-ins, in shapes
where the cavity wasn't in the corner.

0(defmethod (bad-drop-in-positions shape-info) (xpos ypos)
  (let ((drop-ins))
    (labels ((is-good-liberty (x y)
	       (and (is-adjacent self x y)
		    (selectq xpos
		      (:center t)
		      (:right ( x last-col))
		      (:left ( x first-col))
		      (t (signal-error ("bad x pos"))))
		    (selectq ypos
		      (:center t)
		      (:bottom ( y last-row))
		      (:top ( y first-row))
		      (t (signal-error ("bad y pos"))))
		    (not (in-cavity x y))
		      ))
	     (in-cavity (x y &optional (xpos xpos)(ypos ypos))
	       (selectq xpos
		 (:center
		  (selectq ypos
		    (:center nil)
		    (:top (and (not (connected-path-exists x y first-col first-row))
			       (not (connected-path-exists x y last-col first-row))
			       (connected-path-exists x y nil (1- first-row))))
		    (:bottom (and (not (connected-path-exists x y first-col last-row))
				  (not (connected-path-exists x y last-col last-row))
				  (connected-path-exists x y nil (1+ last-row))))))
		 (:left
		  (selectq ypos
		    (:center (and (not (connected-path-exists x y first-col first-row))
				  (not (connected-path-exists x y first-col last-row))
				  (connected-path-exists x y (1- first-col) nil)))
		    (:top (if nil ;(is-member self first-col first-row)
			      (or (in-cavity x y :left :center)
				  (in-cavity x y :center :top))
			      (connected-path-exists x y first-col first-row)))
		    (:bottom (if nil ;(is-member self first-col last-row)
				 (or (in-cavity x y :left :center)
				     (in-cavity x y :center :bottom))
				 (connected-path-exists x y first-col last-row)))))
		 (:right
		  (selectq ypos
		    (:center (and (not (connected-path-exists x y last-col first-row))
				  (not (connected-path-exists x y last-col last-row))
				  (connected-path-exists x y (1+ last-col) nil)))
		    (:top (if nil ;(is-member self last-col first-row)
			      (or (in-cavity x y :right :center)
				  (in-cavity x y :center :top))
			      (connected-path-exists x y last-col first-row)))
		    (:bottom (if nil ;(is-member self last-col last-row)
				 (or (in-cavity x y :right :center)
				     (in-cavity x y :center :bottom))
				 (connected-path-exists x y last-col last-row)))
		    ))))
	     (connected-path-exists (fx fy tx ty &optional checked)
	       (cond ((is-member self fx fy) nil)
		     ((and (or (null tx) (eql tx fx))
			   (or (null ty) (eql ty fy)))
		      t)
		     ((< fx first-col) nil)
		     ((< fy first-row) nil)
		     ((> fx last-col) nil)
		     ((> fy last-row) nil)
		     ((memq (+ fx (* fy 100)) checked) nil)
		     (t (push (+ fx (* fy 100)) checked)
			(or (connected-path-exists (1+ fx) fy tx ty checked)
			    (connected-path-exists (1- fx) fy tx ty checked)
			    (connected-path-exists fx (1+ fy) tx ty checked)
			    (connected-path-exists fx (1- fy) tx ty checked)))))
	     )
      (do-for-all self
		  #'(lambda (x y)
		      (declare (sys:downward-function))
		      (unless (zerop (delta-groups-around self x y))
			(unless (or (is-good-liberty (1+ x) y)
				    (is-good-liberty  x (1+ y))
				    (is-good-liberty (1- x) y)
				    (is-good-liberty x (1- y)))
			  (push (list x y) drop-ins)))))
      drop-ins)))

(defun drop-errors ()
4  0(loop for (xp yp) in all-shape-positions
	nconc
	  (loop for s in (all-shapes zhash-shape-database)
		as good = (drop-in-positions s xp yp )
		as bad = (bad-drop-in-positions s xp yp)
		unless (equal good bad)
		  collect (list s xp yp good bad))))
||#
   
(defmethod (find-dropped-shape shape-info) (val)
  (cdr (assoc val (get-prop self :dropped-shapes) :test #'equal)))


4;;
;; Stuff for generating, maintaining, and using the shape library's FATE database.
;;

0(defmethod (calculate-one-fate shape-info)
	   (&key property x-pos y-pos outside-liberties introns
		 same-position same-liberties (moving '(:first :second))
		 skip-tight-corners forced)
  (declare (special save-trees))
  (let* ((*fate-property* property)
	 (first-fate (fate-of-shape self x-pos y-pos :first
				    property
				    introns))
	 (second-fate (fate-of-shape self x-pos y-pos :second
				     property
				     introns))
	 (original-first-fate first-fate)
	 (original-second-fate second-fate)
	 (l-first-fate (fate-for-outside-liberties first-fate outside-liberties))
	 (l-second-fate (fate-for-outside-liberties second-fate outside-liberties))
	 (decoded-first (new-decode-fate l-first-fate))
	 (decoded-second (new-decode-fate l-second-fate))
	 (first-fate-changed)
	 (second-fate-changed)
	 (fate-set)
	 (parent (parent-shape self))
	 (empty-pl (get-player-for-color *game* :black+empty)))
    (multiple-value-bind (fx fy)
	(first-member self)
      (unless (and l-first-fate l-second-fate
		   (neq decoded-first :unknown)
		   (neq decoded-second :unknown)
		   (not forced))
	(unwind-protect
	    (block evaluation
	      (multiple-value-bind (xofs yofs group captures)
		  (place-surrounded-in-position *game* self x-pos y-pos
						outside-liberties introns
						:same-position same-position
						:same-liberties same-liberties)
		(if (and (null captures)
			 (member group (active-groups (player group))))
		  
		    (let* ((empty-group (find-group-location empty-pl
							     (+ fx xofs) (+ fy yofs)))
			   (adjacent-ter (adjacent-territory-groups empty-group))
			   (len (length adjacent-ter))
			   (big-ter-libs (liberties-count group))
			   (ufate (new-encode-fate :unknown :pass
					       len
					       big-ter-libs big-ter-libs))
			   (big-en (loop with big-group and big-size
					 for i in (adjacent-enemy-groups group)
					 as size = (size i)
					 do (cond ((or (null big-size)
						       (> size big-size))
						   (setq big-size size
							 big-group i)))
					 finally (return big-group))))

		      (if (and skip-tight-corners (> len 1))
			  (return-from evaluation :skipped)
			  
			  (update-boards *game*)
			  (without-interrupts
			    3;this WITHOUT-INTERRUPTS is because we run the
0			    3;the save/compress operation concurrently0	       
			    (split-one-fate self property)
			    (when (and (member :first moving) first-fate)
			      (set-fate-of-shape self x-pos y-pos :first 
						 (merge-fates first-fate
							      ufate)
						 property
						 introns)
			      (setq fate-set t)
			      )
			    (when (and (member :second moving) second-fate)
			      (set-fate-of-shape self x-pos y-pos :second
						 (merge-fates second-fate
							      ufate)
						 property
						 introns)
			      (setq fate-set t))
			    )
			  (multiple-value-bind (first first-move second second-move)
			      (enumerated-group-status
				group
				:moving moving
				:his-considered-safe-area big-en
				:assume-enemies-killable t
				:assume-friends-killable t
				:print-stats save-trees
				:move-first-minimum nil
				:search-state
				`(:check-fate :always
				  :check-for-eye-in-stomach
				  default-check-for-eye-in-stomach
				  :try-passing-near-end 100)
				:search-parameters
				`(:absolute-depth-threshold 15
				  :periodically-update 10.0
				  ))
			    (let ((fx (first (first first-move)))
				  (fy (second (first first-move)))
				  (sx (first (first second-move)))
				  (sy (second (first second-move))))
			      (when (and (integerp fx) (integerp fy)
					 (not (is-member self (- fx xofs) (- fy yofs))))
				(setq fx :outside fy nil))
			      (when (and (integerp sx) (integerp sy)
					 (not (is-member self (- sx xofs) (- sy yofs))))
				(setq sx :outside sy nil))
			      
			      (when (member :first moving)
				(multiple-value-bind (ignore ignore ignore ignore ignore ignore
						      pv)
				    (new-decode-fate (fate-for-outside-liberties
						       original-first-fate outside-liberties))

				  (setq l-first-fate
					(new-encode-fate
					  first
					  (if (integerp fx)
					      (ordinal-number-for-member parent
									 (- fx xofs)
									 (- fy yofs))
					      fx)
					  len
					  big-ter-libs big-ter-libs
					  (if (integerp fx) nil fy)
					  pv
					  ))
				  (multiple-value-setq (first-fate first-fate-changed)
				    (merge-fates (or original-first-fate ufate) l-first-fate))))

			      (when (member :second moving)
				(multiple-value-bind (ignore ignore ignore ignore ignore ignore
						      pv)
				    (new-decode-fate (fate-for-outside-liberties
						       original-second-fate outside-liberties))

				  (setq l-second-fate
					(new-encode-fate
					  second
					  (if (integerp sx)
					      (ordinal-number-for-member parent
									 (- sx xofs)
									 (- sy yofs))
					      sx)
					  len
					  big-ter-libs big-ter-libs
					  (if (integerp sx) nil sy)
					  pv
					  ))
				  (multiple-value-setq (second-fate second-fate-changed)
				    (merge-fates (or original-second-fate ufate) l-second-fate))
				  (setq fate-set t)
				  ))))
		      ))

		    ;not a possible position
		    (when (member :first moving)
		      (setq l-first-fate
			    (new-encode-fate
			      :impossible
			      :outside 
			      0 0 0 nil))
		      (multiple-value-setq (first-fate first-fate-changed)
			(merge-fates original-first-fate l-first-fate))
		      )
		    (when (member :second moving)
		      (setq l-second-fate
			    (new-encode-fate
			      :impossible
			      :outside 
			      0 0 0 nil))
		      (multiple-value-setq (second-fate second-fate-changed)
			(merge-fates original-second-fate l-second-fate)))
		    (setq fate-set t)
		    )
		))

	  (when fate-set
	    (without-interrupts
	      3;this WITHOUT-INTERRUPTS is because we run the
0	      3;the save/compress operation concurrently
0	      (split-one-fate self property)
	      (when (or first-fate-changed second-fate-changed)
		(let ((par (parent-shape self)))
		  (unless (member par recently-changed-shapes)
		    (push par recently-changed-shapes))))
	      (when (member :first moving)
		(set-fate-of-shape self x-pos y-pos :second
				   first-fate
				   property
				   introns))
	      (when (member :second moving)
		(set-fate-of-shape self x-pos y-pos :second
				   second-fate
				   property
				   introns))))
	  (or first-fate-changed second-fate-changed)
	  )
	)
      )))


(defmethod (choose-intron shape-info) (&key live-window)
  (tv:choose
    (nreverse
      (loop with ias = (intron-array-size self)
	    with idxes = (aref sequences-sorted-by-bits ias)
	    for i from 1 below (expt 2 ias)
	    as idx = (aref idxes i)
	    collect
	      `(,(make-instance 'big-text-item :text (picture self idx :zero-char #\o))
		:value ,idx :documentation ,(format nil #"with introns = #o~O" idx)
		)))
    :live-window live-window
    :label #"Choose which picture"))

(defvar *new-game* t)
(zl:def-menu-method place-on-board shape-info
  :menu-id "place on board"
  :menu-documentation "place this shape in some quadrant of the board  R: specify intron too"
  :context :view
  :button :mouse-right
  (ignore ignore ignore  b window)
  (multiple-value-bind (choice nil button libs)
      (choose-position-and-liberties self :Live-window window)
    (when choice
      (let ((intron (or (when (or (eq b :mouse-right)
				  (eq button :mouse-right))
			  (choose-intron self :live-window window))
			0)))
	(when *new-game* (new-game *game*))
	(place-surrounded-in-position *game* self
				      (first choice) (second choice)
				      (or (first libs) 0)
				      intron)
	(update)))
    choice))

(zl:def-menu-method describe-fate shape-info
  :menu-id "describe fate"
  :menu-documentation 
  "Describe the saved results of captured-shape analysis L: principle  M: some intron   R: some position"
  :button :mouse-middle
  :context :view
  (ignore ignore ignore button window)

  (block everything
    (let* ((n (case button
		(:mouse-left t)
		(:mouse-middle (choose-intron self :live-window window))
		(:mouse-right t)))
	   (pos (cond
		  ((eq button :mouse-left) #"C")
		  ((eq button :mouse-right)
		   (or (choose-position self :Live-window window)
		       (return-from everything nil)))))
	   (drops (get-prop self :dropped-shapes))
	   (com (get-prop self :fate-comment)))

      (if (eq button :mouse-left)
	  (progn
	    (put-prop self nil :center-fate)
	    (compare-fates-one-position self nil 0 *fate-property* 0 :center-fate
					:center :center :center :center)
	    (describe-fate-property self :center-fate t nil 0)
	    (compare-1-position self *fate-property* 0 *fate-property* 0 :center-fate)
	    (describe-fate-property self :center-fate t nil 0))
	  (progn
	    (compare-1-shape self nil 0 *fate-property* 0 :liberties-fate)
	    (describe-fate-property self :liberties-fate n pos 0)))

      (loop for liberties in (cdr fate-liberties)
	    do
	(compare-1-shape self *fate-property* (1- liberties) *fate-property*
			 liberties :liberties-fate)
	(describe-fate-property self :liberties-fate n pos liberties))

      (when drops
	(format t "~&Additional information for drop-in shape~p~&"
		(length drops))
	(loop for (drop-in positions shape) in drops
 	      do (format t #"~&~a (#o~O) in position ~\\move-position\\"
			 shape
			 drop-in
			 positions)
		 ))
      (when com
	(format t "~&~A~%" com))))
  self)

(defmethod (describe-fate-property shape-info) (&optional
						 (property *fate-property*)
						 (introns 0)
						 (position)
						 (liberties 0)
						 (show-eqv t)
						 )

  (unless introns (setq introns 0))
  (labels ((report-one-intron-internal (spec val msg)
	     (multiple-value-bind  (fate ord nadj libs lib2 aux)
		 (new-decode-fate val) 
	       (ignore aux nadj)
	       (unless (and (neq property *fate-property*)
			    (member fate '(:impossible :unknown)))
		 (setq spec (sort spec #'string-lessp))
		 (format t
			 #"~a moving ~a at point ~a in position~p ~{~a ~} libs ~d-~d~%"
			 fate msg
			 ord
			 (length spec)
			 spec
			 libs lib2)))
	     )
	   (report-one-intron (spec result msg )
	     (let ((fval (aref result introns)))
	       (if (consp fval)
		   (loop for v in fval do (report-one-intron-internal spec v msg))
		   (report-one-intron-internal spec fval msg))))
	   (report-all-introns (spec result msg mode)
	     (loop with ias = (intron-array-size self)
		   and summ
		   with seq = (aref sequences-sorted-by-bits ias)
		   for idx from 1 below (expt 2 ias)
		   as i = (aref seq idx)
		   as fate = (new-decode-fate (aref result i))
		   as fl = (or (assoc fate summ :test #'equal)
			       (let ((new (cons fate nil )))
				 (push new summ)
				 new))
		   do (setf (cdr fl)
			    (cons i (cdr fl) ))
		   finally
		     (setq summ (sort summ
				      #'(lambda (a b)
					  (declare (sys:downward-function))
					  (> (length a)
					     (length b)))))
		     (loop for (fate . whose) in summ
			   unless (member fate '(:impossible :unknown))
			   do
		       (format t #"~&~%~A libs is ~A moving ~A in position~p ~{~a ~} for introns~%#o"
			       liberties fate msg (length spec) spec)
		       (print-list-of-pictures
			 *terminal-io*
			 (loop for pic in (reverse whose)
			       collect
				 (multiple-value-bind (nil ord)
				     (new-decode-fate (aref result pic))
				   (format nil "~o~&~\\picture\\"
					   pic
					   (picture self pic
						    :zero-char
						    #\center-dot
						    :point-char
						    (if (eq mode :first) #\O #\X)
						    :point (when (integerp ord) ord)
						    ))))
			 nil
			 :intercolumn-spaces 1
			 :interrow-spaces 0))))
	   (report-results (specs msg mode fates)
	     (loop for remaining-specs on (reverse specs)
		   as (spec) = remaining-specs
		   as more-fates on fates
		   as ((nil . result))= more-fates
		   when spec
		     do
		       (if (eql introns t)
			   (report-all-introns spec result msg mode)
			   
			   (multiple-value-bind (fate ord)
			       (new-decode-fate (aref result introns))
			     ord
			     (loop for future-specs on (cdr remaining-specs)
				   for (nil . next-results) in (cdr more-fates)
				   do
			       (multiple-value-bind (future-fate future-ord)
				   (new-decode-fate (aref next-results introns))

				 (when (and (eql fate future-fate)
					    (eql future-ord ord))
				   (nconc spec (first future-specs))
				   (setf (first future-specs) nil))))
			     (report-one-intron spec result msg ))
			   )
		   finally 
		     (unless (or (eql introns t)
				 (not show-eqv)
				 (member (new-decode-fate (aref result introns))
					 '(:impossible :unknown)))
		       (summarize-equivalent-outcomes
			 result introns
			 #"And likewise for introns #o"
			 self
			 ))))
	   )
    (loop with first-move and second-move
	  and some-first-move and some-second-move
	  and fates = (get-prop self property)
	  and moving and direction
	  for (positions) in fates
	  do
      (loop as remaining-positions
	       first positions 
	       then (logxor remaining-positions some-position)
	    while ( remaining-positions 0)
	    as some-position = (logand remaining-positions
				       (- remaining-positions))
	    do (multiple-value-setq (moving direction)
		 (decode-move-geometry some-position :short-string)
		 )
	    if (not (cond ((null position))
			  ((stringp position)
			   (equal position direction))
			  ((consp position)
			   (multiple-value-bind (nil xp yp)
			       (decode-move-geometry some-position)
			     (and (eq (first position) xp)
				  (eq (second position) yp))))))
	      do nil
	    else if (eq moving :first)
		   collect direction into first-m
	    else collect direction into second-m
	    finally
	      (push first-m first-move)
	      (when first-m (setq some-first-move t))
	      (push second-m second-move)
	      (when second-m (setq some-second-move t)))
	  finally
	    (unless (or (eql introns t)
			(not (or some-first-move some-second-move))
			(loop for (nil . result) in fates
			      as fate = (new-decode-fate (aref result introns))
			      always (member fate '(:impossible :unknown)))
			)
	      (format t #"~&~a (introns #o~o)  ~a ~a libs~%"
		      (picture self introns) introns self 
		      (if (eql t liberties) "all" liberties)))
		    
	    (when some-first-move
	      (report-results first-move #"first" :first fates))
	    (when some-second-move		
	      (report-results second-move #"second" :second fates))
	    )
    ))


(defun summarize-equivalent-outcomes (ar idx first-message shape
				      &aux (first-time t) pics)
  (labels ((report (from to)
	     (when (eql from idx) (incf from))
	     (when (eql to idx) (decf to))
	     (if shape
		 (loop for i from from to to
		       unless (= i idx)
			 do (push (format nil "~o~&~\\picture\\"
					  i (picture shape i))
				  pics))
		 (unless (> from to)
		   (if first-time
		       (format t "~&~A" first-message)
		       (format t ","))
		   (cond ((= from to)
			  (format t "~o" from))
			 ((= from (1- to))
			  (format t "~o,~o" from to))
			 (t (format t "~o-~o" from to)))
		   (setq first-time nil)))))
    (map-over-introns ar idx #'report shape)
    (when (and shape pics)
      (format t "~&~A" first-message)
      (print-list-of-pictures *terminal-io* (nreverse pics) nil
			      :intercolumn-spaces 2
			      :interrow-spaces 0)
      (setq first-time nil)
      )
    (unless first-time (format t "~&"))
    ))

(defun map-over-introns (ar idx fn shape )
  (loop with winner = (aref ar idx)
	and first-same = -1
	and last-same = -1
	doing
    (multiple-value-bind (winner-fate winner-pos)
	(new-decode-fate winner)
      (multiple-value-setq (first-same last-same)
	(get-outcome-and-range ar last-same winner-fate winner-pos shape )))
	until (null first-same)
	do
    (funcall fn first-same last-same)
    ))

(defun get-outcome-and-range (ar idx winner-fate winner-pos shape)
  (ignore shape)
  (loop with first-same and last-same
	for i from (max idx 0) below (array-total-size ar)
	as dat = (aref ar i)
	do
    (multiple-value-bind (my-fate my-pos)
	(new-decode-fate dat)
      (cond ((eql i idx))
	    ((and (eql my-fate winner-fate)
		  (eql my-pos winner-pos))
	     (cond ((null first-same)
		    (setq first-same i
			  last-same i))
		   (t (setq last-same i))))
	    (first-same
	     (return (values first-same last-same))
	     (setq first-same nil 
		   last-same nil))))
	finally
	  (when first-same (return (values first-same last-same))
		)))

(defmethod (fate-of-shape shape-info)
	   (x-position y-position move property introns)
  (unless *use-shape-library*
    (signal-error ("shouldn't get here unless *use-shape-library* is t")
      (:ignore)))
  (let ((idx (encode-move-geometry move x-position y-position))
	(fates  (get-prop self property)))
    (cond (fates 
	   (loop for (spec . arr) in fates
		 when (logtest spec idx)
		   return (values (aref arr introns) arr)))
	  (t (signal-error ("fate library appears to be missing")
	       (:ignore "Disable the shape library"
		(setq *use-shape-library* nil))
	       (:repair "Reload the shape library"
		(load shape-data-file)
		(fate-of-shape self x-position y-position move property introns)))
	     ))))

(defmethod (split-one-fate shape-info) (&optional property)
  3;; this splits the shared fate positions into segregated positions,
0  3;; which is necessary before adding new information, since it may not
0  3;; be shared among all positions
0  (map-props
    #'(lambda (&key property)
	(declare (sys:downward-function))
	(let ((val (get-prop self property)))
	  (without-interrupts3 ;because the compress process can run concurrently
0	    (loop for vv in val
		  as ( keys . arr ) = vv
		  do
	      (loop as kb = (logand keys (- keys))
		    until (eql keys kb)
		    do
		(let ((new-arr (make-array (array-total-size arr))))
		  (copy-array-contents arr new-arr)
		  (setf (first vv) (setq keys (logxor kb keys)))
		  (nconc val  `((,kb . ,new-arr)))))))
	  ))
    property))

(defun split-fates (&optional property)
  (do-for-shapes-and-drop-ins zhash-shape-database
			      #'(lambda (sh) (split-one-fate sh property))))
(defvar transactions nil)
(defmethod (set-fate-of-shape shape-info)
	   (x-position y-position move set-to-value property introns &optional merged-ok)

  (push (list x-position y-position move set-to-value property introns) transactions)

  (when (consp set-to-value)
    (loop with np
	  for i in set-to-value
	  do (multiple-value-bind (f ignore n) (new-decode-fate i)
	       (cond ((eq f :impossible))
		     ((null np)(setq np n))
		     (( np n) (error "mismatch"))))))
    
	  
  (let ((idx (encode-move-geometry move x-position y-position))
	(fates  (get-prop self property)))
    (unless
      (loop for spec in fates
		  as (key . vals) = spec
		  when (logtest idx key)
		    do
		      (if (or merged-ok (eql idx key)
			      (with-restart-options
				(((error "Do it anyway") t)
				 ((error "Skip it") (return-from set-fate-of-shape nil))
				 ((error "Split it" nil)))
				(signal-error ("Attempting to set fate of unsplit shape"))
				))
			  (progn (setf (aref vals introns) set-to-value)
				 (return t))
			  (setf (first spec) (logxor idx key))
			  (return nil)))
      (let ((vals (make-array (expt 2 (intron-array-size self)))))
	(setf (aref vals introns) set-to-value)
	(put-prop self (cons (cons idx vals) fates ) property)))))


(defmethod (share-fate-properties shape-info) (prop)
  (without-interrupts3 ;this without-interrupts is because we run the EXPAND/CREATE process
0    3                  ;concurrently
0    (loop with vals = (get-prop self prop)
	  and some = nil
	  for value on vals
	  as ((key . arr)) = value
	  when key
	    do
	      (loop for other-value on (cdr value)
		    as ((other-key . other-arr)) = other-value
		    when
		      (and other-key
			   (loop for i from 0 below (array-total-size arr)
				 always (equal (aref arr i) (aref other-arr i))))
		      do
			(setq key (logior key other-key))
			(setf (first (first value)) key)
			(setf (first other-value) nil)
			(setq some t))
	  finally
	    (put-prop self (delete nil vals) prop)
	    (return some)
	    )))

(defmethod (hash-cons-fate-properties shape-info) (prop)
  (without-interrupts3 ;this without-interrupts is because we run the EXPAND/CREATE process
0    3                  ;concurrently
0    (loop with vals = (get-prop self prop)
	  and some = nil
	  for value on vals
	  as ((key . arr)) = value
	  when key
	    do
	      (loop for i from 0 below (array-total-size arr)
		    as v = (aref arr i)
		    when (consp v)
		      do
			(setf (aref arr i)
			      (apply #'hash-list v)))
	  finally
	    (put-prop self (delete nil vals) prop)
	    (return some)
	    )))

(defmethod (compare-1-shape shape-info) (prop-1 libs-1 prop-2 libs-2 new-prop)
  (rem-prop self new-prop)
  (loop for (xp yp) in *all-shape-positions*
	do (compare-fates-one-position self prop-1 libs-1 prop-2 libs-2 new-prop
				       xp yp xp yp))
  (share-fate-properties self new-prop)
  (hash-cons-fate-properties self new-prop)
  )

(defmethod (compare-1-position shape-info) (prop-1 libs-1 prop-2 libs-2 new-prop)
  (rem-prop self new-prop)
  (loop with ((xp1 yp1)) = *all-shape-positions*
	for (xp2 yp2) in (cdr 3*0all-shape-positions3*0)
	do (compare-fates-one-position self prop-1 libs-1 prop-2 libs-2 new-prop
				       xp1 yp1 xp2 yp2))
  (share-fate-properties self new-prop)
  (hash-cons-fate-properties self new-prop)
  )

3;; This method removes redundant outcomes for the multiple-liberty
;; searches, so that the summaries will summarize only the differences
;; between the main case and the case with extra liberties.  
;; the result of this summary is for presentation purposes only - it
;; shouldn't be saved into a permanent data base!
;;
0(defmethod (compare-fates-one-position shape-info)
	   (fate-prop-1 libs1 fate-prop-2 libs2 new-prop xp-1 yp-1 xp-2 yp-2)
  (loop for move in '( :first :second)
	do
    (loop for introns from 0 below (expt 2 (intron-array-size self) )
	  as encoded-fate-2 = (fate-for-outside-liberties
				(fate-of-shape self xp-2 yp-2 move fate-prop-2 introns)
				libs2)
	  when encoded-fate-2
	    do
	      (let* ((fate-2 (new-decode-fate encoded-fate-2))
		     (fate-1 (when fate-prop-1
			       (new-decode-fate
				 (fate-for-outside-liberties
				   (fate-of-shape self xp-1 yp-1 move fate-prop-1 introns)
				   libs1)))))
		(when (and fate-2
			   (not (eql fate-1 fate-2)))
		  (set-fate-of-shape self xp-2 yp-2 move encoded-fate-2
				     new-prop introns)))
	      ))
  )


(defmethod (build-shapes zhash-shape-database) ()
  "do the whole shape database bit"
  (setf (zl:font-fill-pointer shape-font-1) 0)
  (zl:fillarray shape-font-1 '(0))
  (setq shape-font-index 0)
  (clrhash self)
  (loop for i from 0 below shape-database-generation do
3    ;; this builds shapes of sizes up to 07 (previously 6) 3.  We  could build larger shapes
    ;; but there are many more of them.  In additon, we'd need better discriminators
0    (build-shape-generation self i))

  #||
  3;; reactivate these lines if we ever revive the idea of including all
0  3;; the shapes with "drop ins" as well as "introns".  This never quite
0  3;; worked, and there are an awful lot of them!
0      (loop for i from 3 below shape-database-generation do
	3;; build the shapes with drop-ins of the same color, corresponding
0	3;; to simply split groups
0	(build-drop-in-generation self i))
   ||#
  ;(build-shape-descriminators self)
  ;(build-isomer-descriminators self)
  (find-drop-ins self)
  (do-for-shapes-and-drop-ins self #'fixup-isomers)
  ;(verify-primary-shapes self)
  )

(defmethod (find-drop-ins common-shape-database) ()
  3;; survey the whole gaumit of shapes, finding subshapes
0  3;; where an additional white stone splits the group
0  3;; but it remains diagonally connected.  In certain corners
0  3;; these shapes give the problem solver lots of problems
0  (tv:dolist-noting-progress (shape (all-shapes-in-canonical-order self)
				  "Finding drop-in shapes")
    (loop for (xpos ypos) in *all-shape-positions* 
	  as drops = (drop-in-positions shape xpos ypos)
	  as drop-mems = (loop for (x y) in drops
			       collect
				 `(,(ordinal-number-for-member shape x y) ,x ,y))
	  when drops
	    do
	      (labels ((add-dropped-shapes (bbase new-bits copy)
			 (if new-bits
			     (loop for bits in new-bits
				   as (bit x y) = bits
				   do
			       (remove-member copy x y)
			       (add-dropped-shapes (logior bbase (lsh 1 (1- bit)))
						   (remove bits new-bits :test #'equal)
						   copy)
			       (add-member copy x y)))
			 (unless (zerop bbase)
			   (let ((old (find-dropped-shape shape bbase)))
			     (unless old
			       (let ((copy (copy-set copy nil)))
				 (setf (canonical-number copy)
				       (format nil "~A-#o~O"
					       (canonical-number shape)
					       bbase))
				 (put-prop copy shape :parent-shape)
				 (put-prop copy (size shape)
					   :prescribed-intron-array-size)
				 (add-to-shape-font copy)
				 (setq old (list bbase 0 copy)))
			       (put-prop shape
					 (cons old
						       (get-prop shape :dropped-shapes)
						       )
					 :dropped-shapes)
			       (setq old (cdr old)))
			     (setf (first old)
				   (logior (first old)
					   (encode-move-geometry :first xpos ypos)))
			     ))))
		(add-dropped-shapes 0 drop-mems (copy-set shape nil))
		))
      ))
;;
;; starting with the current list of shapes, add one stone all the way around each
;; to yield the next larger family of shapes
;;
(defmethod (build-shape-generation common-shape-database)
	   ( size )
  (cond ((= size 0)
	 (clrhash self)
	 (let ((cg (make-instance 'shape-info
				  :area *go-static-area*
				  :full-board-size 19
				  )))
	   (re-init cg)
	   (add-member cg 10 10)
	   (add-shape self cg)))
	(t (loop for i in (all-shapes-in-canonical-order self)
		 when (= (size i) size)
		   do (add-larger-shapes i))))
  )

(defmethod (find-shape-with-name zhash-shape-database) (name)
  (let (shapes
	(name (string name)))
    (maphash #'(lambda (ignore dat)
		 (declare (sys:downward-function))
		 (when (equal name (string (send (first dat) ':name)))
		   (push dat shapes)))
	     self)
    (cond ((null (cdr shapes)) (car shapes))
	  (t shapes))))


(defmethod (verify-shape-descriptions common-shape-database) ()
  (tv:dolist-noting-progress (dat (all-shapes-in-canonical-order self)
				    "Verify shape descriminators")
    (verify-shape-description dat))
  self)


(defmethod (do-for-shapes zhash-shape-database) (func)
  (declare (sys:downward-funarg func))
  (maphash #'(lambda (ignore dat)
	       (declare (sys:downward-function))
	       (when (zerop (second dat)) (funcall func (first dat))))
	   self
	))

(defun shape-bigger (a b)
  (let ((sa (size a))
	(sb (size b)))
    (cond ((< sa sb))
	  ((= sa sb)
	   (> (center-shape-weight a 150)
	      (center-shape-weight b 150))
	   )
	  )))

(defmethod (all-shapes common-shape-database) ()
  (let (all-shapes)
    (do-for-shapes self #'(lambda (dat)
			    (push dat all-shapes)))
    (sort all-shapes #'shape-bigger)
    ))

(defmethod (all-shapes-in-canonical-order common-shape-database) ()
  (let (all-shapes)
    (do-for-shapes self #'(lambda (dat)
			    (declare (sys:downward-function))
			      (push dat all-shapes)))
    (sort all-shapes #'(lambda (a b)(< (canonical-number a)(canonical-number b))))
    ))

(defmethod (do-for-shapes-and-drop-ins common-shape-database) (fun)
  (declare (sys:downward-funarg fun))
  (loop for a in (all-shapes self) 
	do
    (funcall fun a)
    (loop for (nil nil sh) in (get-prop a :dropped-shapes)
	  do (funcall fun sh))))

(defmethod (all-shapes-and-drop-ins common-shape-database) ()
  (let (all-shapes)
    (do-for-shapes-and-drop-ins self #'(lambda (dat)
					 (declare (sys:downward-function))
					 (push dat all-shapes)))
    (sort all-shapes #'shape-bigger)
    ))

(defmethod (do-for-drop-ins common-shape-database) (fun)
  (declare (sys:downward-funarg fun))
  (loop for a in (all-shapes shape-database)
	do 
	   (loop for (nil nil sh) in (get-prop a :dropped-shapes)
		 do (funcall fun sh))))

(defmethod (all-drop-ins common-shape-database) ()
  (let (all-shapes)
    (do-for-drop-ins self #'(lambda (dat)
			      (declare (sys:downward-function))
			      (push dat all-shapes)))
    (sort all-shapes #'shape-bigger)
    ))



(defflavor normalizer (x-off y-off perm target shape-description)
	   (tv:essential-menu-methods-mixin)
  (:conc-name nil)
  (:readable-instance-variables target perm shape-description)
  (:writable-instance-variables target perm shape-description)
  :initable-instance-variables)

(defmethod (sys:print-self normalizer) (stream prin slash)
  prin slash
  (format stream #"#<normal ~A>" target))

(defmethod (ordinal-number-for-standard-member normalizer) (x y &optional (error-p t))
  (ordinal-number-for-member shape-description x y error-p)
  )

(defmethod (ordinal-number-for-member normalizer) (x y &optional (error-p t))
  (multiple-value-bind (tx ty)
      (transform-to-standard-coordinates self x y)
    (ordinal-number-for-member shape-description tx ty error-p)
    ))

(defmethod (member-for-ordinal-number normalizer) (n &optional (error-p t))
  (multiple-value-bind (x y)
      (member-for-ordinal-number target n error-p)
    (when (and x y)
      (transform-to-standard-coordinates self x y))))

(defmethod (board-member-for-standard-ordinal normalizer) (n &optional (error-p t))
  (multiple-value-bind (x y)
      (member-for-ordinal-number shape-description n error-p)
    (when (and x y)
      (transform-to-board-coordinates self x y))))

(defmethod (do-normalized normalizer) ( func x y )
  (declare (sys:downward-funarg func))
  (cond (( 0 (logand perm 1)) (psetq x y y x)))
  (cond (( 0 (logand perm 2)) (setq x (- x))))
  (cond (( 0 (logand perm 4)) (setq y (- y))))
  (funcall func target (+ x x-off) (+ y y-off)))

(defmethod (transform-to-standard-coordinates normalizer) ( x y )
  (cond (( 0 (logand perm 1)) (psetq x y y x)))
  (cond (( 0 (logand perm 2)) (setq x (- x))))
  (cond (( 0 (logand perm 4)) (setq y (- y))))
  (values (+ x x-off) (+ y y-off)))

(defmethod (normalized-edge-position normalizer) ()
  ;;
  ;; this tells us what overall board position the canonical shape
  ;; we correspond to is in.
  ;;
  (let ((fbs (full-board-size target)))
    (multiple-value-bind (ignore left top right bottom)
	(box-stats target)

      (when (logtest perm 1)
	(psetq left top
	       top left
	       right bottom
	       bottom right))
      (when (logtest perm 2)
	(psetq right (- fbs left -1)
	      left (- fbs right -1)))
      (when (logtest perm 4)
	(psetq bottom (- fbs top -1)
	      top (- fbs bottom -1)))
    (values (cond ((= left 1) :LEFT)
		  ((= right fbs) :RIGHT)
		  (t :CENTER))
	    (cond ((= top 1) :TOP)
		  ((= bottom fbs) :BOTTOM)
		  (t :CENTER))))))

(defmethod (normalized-board-position normalizer) ()
  ;;
  ;; this tells us what overall board position the canonical shape
  ;; we correspond to is in.
  ;;
  (multiple-value-bind (xpos ypos) (normalized-edge-position self)
    (board-position-from-edge-position xpos ypos)))

(defun board-position-from-edge-position (xpos ypos)
  (case xpos
    ((:left :right)
     (case ypos
       ((:top :bottom) :corner)
       (otherwise :side)))
    (otherwise (case ypos
	 ((:top :bottom) :side)
	 (otherwise :center)))))

(defmethod (introns-on-board normalizer) (&optional (drop-ins 0))
  (let* ((introns  0)
	 (sd shape-description)
	 (slot 1))
    (do-for-all
      sd
      #'(lambda (x y)
	  (declare (sys:downward-function))
	  (unless (logtest drop-ins slot)
	    (multiple-value-bind (nx ny)
		(transform-to-board-coordinates self x y)
	      (unless (is-member *empty* nx ny)
		(setq introns
		      (logior introns slot)))
	      ))
	  (setq slot (lsh slot 1))
	  ))
    introns))

(defmethod (transform-intron-to-standard normalizer) (int)
  (if (zerop perm) int
      (loop with sd = shape-description 
	    and mint = int
	    and res = 0
	    until (zerop mint)
	    as next-int = (logand int (- mint))
	    as ordinal-next-int = (integer-length next-int)
	    do
	(setq mint (logxor mint next-int))
	(multiple-value-bind (x y)
	    (member-for-ordinal-number self ordinal-next-int)
	  (setq res (logior res (lsh 1 (1- (ordinal-number-for-member sd x y))))))     
	    finally (return res))))

(defmethod (transform-ordinal-move-to-standard normalizer) (fate)
  (cond ((consp fate)
	 (loop for i in fate collect (transform-ordinal-move-to-standard self i)))
	((integerp fate)
	 (let ((ord (decode-ordinal-move fate)))
	   (if (integerp ord)
	       (encode-ordinal-move
		 (fast-low-bit
		   (transform-intron-to-standard self (lsh 1 (1- ord))))
		 fate)
	       fate)))
	(t fate)))

(defmethod (picture normalizer) (&optional (introns t))
  (when (eq introns t)
    (setq introns (introns-on-board self)))
  (picture shape-description introns))

(defmethod (dead-shape-p normalizer) () 
  (when *use-shape-library*
    (multiple-value-bind (xpos ypos)
	(normalized-edge-position self)
      (let ((fate (fate-of-shape shape-description xpos ypos :second *fate-property* 0)))
	(member (new-decode-fate (if (consp fate) (first fate) fate))
		'(:dead :dead-in-ko :alive-in-ko :dead-with-eye :seki))))))

(defmethod (fate-of-position normalizer)
	   (moving &optional (introns 0) (drop-ins 0))
  (when *use-shape-library*
    (when (eq introns t)
      (setq introns (introns-on-board self drop-ins))
      )

    (multiple-value-bind (xpos ypos)
	(normalized-edge-position self)
      (let* ((adj (if (zerop drop-ins)
		      (capturing-groups target)
		      (adjacent-territory-or-drop-in-groups target)))
	     (len (length adj))
	     (desc1 shape-description)
	     (desc (when desc1
		     (if (and drop-ins ( drop-ins 0))
			 (cadr (find-dropped-shape desc1 drop-ins))
			 desc1)))
	     )
	(unless desc
	  (signal-error ("No shape description for ~A and drop-in ~A" (or desc1 self) drop-ins)))

	(multiple-value-bind (big-group big-liberties)
	    (loop with (first . others) = adj
		  with first-size = (size first)
		  for i in others
		  as new-size = (size i)
		  when (> new-size first-size)
		    do (setq first i
			     first-size new-size)
		  finally (return (values first (liberties-count first))))
	  (let* ((encoded-fate (fate-of-shape desc xpos ypos moving *fate-property* introns)))
	    (if (consp encoded-fate)
	      
		(loop for fa on encoded-fate
		      do
		  (multiple-value-bind (fate ord number-adjacent libs libs2 aux)
		      (new-decode-fate (car fa))
		    (when (and (eql number-adjacent len)
			       (or ( libs big-liberties libs2)
				   (unless (cdr fa) (> big-liberties libs2))))
		      (when (integerp ord)
			(multiple-value-setq (ord aux)
			  (board-member-for-standard-ordinal self ord))) 
		      (return (values fate ord aux len big-group))
		      )))
	      
		(multiple-value-bind (fate ord number-adjacent libs libs2 aux pv)
		    (new-decode-fate encoded-fate)
		  (when (and (eql number-adjacent len)
			     (or ( libs big-liberties libs2)
				 (> big-liberties libs2)))
		    (when (integerp ord)
		      (multiple-value-setq (ord aux)
			(board-member-for-standard-ordinal self ord)))
		    (values fate ord aux pv)
		    ))
		)))))))

(defmethod (fate-of-secure-position normalizer)
	   (moving n-insecure &optional (introns 0) (drop-ins 0))
  (when *use-shape-library*
    (when (eq introns t)
      (setq introns (introns-on-board self drop-ins))
      )

    (multiple-value-bind (xpos ypos)
	(normalized-edge-position self)
      (let* ((len (1+ n-insecure))
	     (desc1 shape-description)
	     (desc (when desc1
		     (if (and drop-ins ( drop-ins 0))
			 (cadr (find-dropped-shape desc1 drop-ins))
			 desc1)))
	     )
	(unless desc
	  (signal-error ("No shape description for ~A and drop-in ~A" (or desc1 self) drop-ins)))

	(let* ((encoded-fate (fate-of-shape desc xpos ypos moving *fate-property* introns)))
	  (when (consp encoded-fate)
	    (setq encoded-fate (car (last encoded-fate))))
	  (multiple-value-bind (fate ord number-adjacent nil nil aux pv)
	      (new-decode-fate encoded-fate)
	    (when ( len number-adjacent)
	      (when (integerp ord)
		(multiple-value-setq (ord aux)
		  (board-member-for-standard-ordinal self ord)))
	      (values fate ord aux pv)
	      )))))))

(defmethod (transform-to-board-coordinates normalizer) (x y &aux x1 y1)
  (setq x1 (- x x-off) y1 (- y y-off))
  (cond (( 0 (logand perm 2)) (setq x1 (- x1))))
  (cond (( 0 (logand perm 4)) (setq y1 (- y1))))
  (cond (( 0 (logand perm 1)) (psetq x1 y1 y1 x1)))
  (values x1 y1))

(defmethod (set-vars normalizer) (p x y shape)
  (setq perm p x-off x y-off y shape-description shape)) 

(defmethod (is-member normalizer) (x y)
  (multiple-value-bind  (x1 y1)
      (transform-to-board-coordinates self x y)
    (is-member target x1 y1)))

(zl:def-menu-method mouse-fate normalizer
  :button :mouse-middle
  :context :view
  :menu-id "fate"
  :menu-documentation "Find the fate of this shape in the fate database"
  (&aux secure)
  (multiple-value-bind (fate x y)
      (fate-of-position self :first t 0)
    (if fate
	(setq secure t)
	(setq secure nil)
	(multiple-value-setq (fate x y)(fate-of-secure-position self :first 0 t 0)))
    (format t #"~&Moving First, fate is ~A~A moving at ~\\xc\\~\\yc\\"
	    (if secure "" "3probably0 ") fate x y))
  (multiple-value-bind (fate x y )  (fate-of-position self :second t 0)
    (if fate
	(setq secure t)
	(setq secure nil)
	(multiple-value-setq (fate x y)(fate-of-secure-position self :second 0 t 0)))
    (format t #"~&Moving Second, fate is ~A~A moving at ~\\xc\\~\\yc\\"
	    (if secure "" "3probably ") 0fate x y))
  self)

(zl:def-menu-method list-of-members normalizer
  :button :mouse-middle
  :context :view
  :menu-id "list of members"
  ()
  (let (ml)
    (do-for-all target
	  #'(lambda (x y)
	      (declare (sys:downward-function))
	      (multiple-value-bind (x1 y1)
		  (transform-to-standard-coordinates self x y)
		(push (list x1 y1) ml))))
    (values (reverse ml) self)))


(defmethod (verify-normal-transformation normalizer) (x y &aux x1 y1 x2 y2)
  (multiple-value-setq (x1 y1) (transform-to-standard-coordinates self x y))
  (multiple-value-setq (x2 y2) (transform-to-board-coordinates self x1 y1))
  (or (and (= x2 x) (= y2 y))
      (signal-error ("Normalize and transform-to-board-coordinates don't match ~A ~A ~A" self x y))))

4;;
;; Stuff to support writing files containing shape data.
;;

0(defun print-new-shapes-to-file ( base-file4 0&key periodically base-extension)
  (let* ((base-path (pathname base-file))
	 (wild-path (send base-path :new-pathname :type "0*" :version :newest))
	 (old-files (sort (mapcar #'car (cdr (fs:directory-list wild-path :fast)))
			  #'(lambda (a b)
			      (declare (sys:downward-function))
			      (let ((a (catch-error (read-from-string (send a :type))))
				    (b (catch-error (read-from-string (send b :type)))))
				(if (and (integerp a) (integerp  b))
				    (> a b)
				    (if (integerp a) t (if (integerp b) nil t)))))))
	 (first-file-number (when old-files
			      (catch-error (read-from-string
					     (send (first old-files) :type)))))
	 (next-number (or base-extension
			  (if (integerp first-file-number) (1+ first-file-number) 1))))
    (loop  with new-shapes do
      (unwind-protect
	  (progn (without-interrupts
		   (setq new-shapes recently-changed-shapes
			 recently-changed-shapes nil))
		 (when new-shapes
		   (catch-error
		     (progn
		       (print-shapes-to-file (send base-path :new-pathname
						   :type (format nil "0~4,48d" next-number))
					     :shapes new-shapes)
		       (incf next-number)
		       (setq new-shapes nil))))
		 )
	(when new-shapes
	  (without-interrupts
	    (loop for i in recently-changed-shapes
		  unless (member i new-shapes)
		    do (push i new-shapes)
		       finally (setq recently-changed-shapes new-shapes)))))
      (if periodically
	  (sleep (/ (truncate (* 60 60 60 (if (numberp periodically) periodically 3))) 60))
	  (loop-finish)))))


(defun print-shapes-to-file (&optional (f shape-data-file) 
			     &key (shapes (all-shapes zhash-shape-database))
			     (in-package "zl-user")
			     (format :bin))
  (let ((file-name
	  (fs:merge-pathname-defaults f nil (if (eq format :bin) :bin :lisp)))
	(forms (loop for i in shapes
		     collect
		       `(zl-user:modify-shape ,@(make-init-list i :printable t))))
	)
    (ecase format
      (:bin
	(si:dump-forms-to-file
	  file-name
	  forms
	  `(:package ,(find-package in-package) :base 10)))
      ((:text :compressed-text)
       ;; format compressed text doesn't quite work becuase the stream can't handle
       ;; fonts directly.
       (let (file)
	 (scl:with-standard-io-environment 
	   (let ((package (pkg-find-package in-package)))
	     (with-open-file-for-me (stream file-name
					    :direction :output
					    :characters t
					    :compress (eq format :compressed-text))
	       (setq file (send stream :truename))
	       (format stream ";;; -*- Package: ~A; Base: ~A; Syntax: Common-Lisp; Mode: LISP -*-~%~%" package *print-base*)
	       (loop for form in forms do (terpri stream) (prin1 form stream))
	       )))
	 file))
      )))
  


4;;
;; Stuff to support reading shapes back in.  The major hair is
;; because we don't guarantee the permutation structure of the
;; shape database to be stable, so we have to consider the possibility
;; that the incoming data need to be re-permuted
;;
0(defun save-fate-data (&key
		       (old-prop *fate-property*)
		       (new-prop :f)
		       (delete nil)
		       (copy nil))
  (do-for-shapes-and-drop-ins
    zhash-shape-database
    #'(lambda (shape)
	(declare (sys:downward-function))
	(unless (eq delete :only)
	  (let ((old (get-prop shape old-prop)))
	    (when copy
	      (setq old (copy-tree old *go-cons-area*)))
	    (put-prop shape old  new-prop))
	  (when delete (put-prop shape nil old-prop)))))
  (when delete (setq recently-changed-shapes nil)))

(defun map-props (fun props)
  (declare (sys:downward-funarg fun))
  (unless props (setq props fate-properties))
  (if (consp props)
      (loop for prop in props do (funcall fun :property prop))
      (funcall fun :property props)))


(defun find-shape (pic)
  (cond ((typep pic 'shape-info) pic)
	((symbolp pic)
	 (cond ((find-shape-with-name zhash-shape-database pic))
	       (t (format t #"can't find ~S" pic)
		  (values))))
	((integerp pic)
	 (nth (1- pic) (all-shapes zhash-shape-database)))
	(t (locate-shape-from-picture pic))))

(defun scan-picture (picture &optional new-set)
  (loop with maxx = 1 and maxy = 1
	and x = 1 and y = 1 and color
	and other-new
	and pic = (string picture)
	with lim = (1- (length pic))
	for idx from 0 to lim
	as chr = (aref pic idx)
	do 

    (selector chr char-equal
      ((#\_ #\-) 
       (setq y 0 x 0))
      (#\|  
       (setq x 1)
       (when (and (< idx lim) (char-equal (aref pic (1+ idx)) #\SPACE))
	 (incf idx))
       )
      ((#\o #\x #\center-dot #\. #\O #\X)
       (let ((new-color (selector chr char-equal
			  ((#\o #\O) :white)
			  ((#\x #\X) :black)
			  (t color))))
	 (unless (eq new-color color)
	   (when new-set 
	     (unless (null color)
	       (rotatef new-set other-new)
	       (unless new-set (setq new-set (make-instance 'set-of-stones
						    :area *go-cons-area*
						    :full-board-size 
						    (full-board-size other-new)
						    ))
		       (re-init new-set))))
	   (setq color new-color))
	 )
       (when (and (< idx lim) (not (char-equal (aref pic (1+ idx)) #\NEWLINE)))
	 (incf idx))
       (setq maxy (max y maxy))
       (setq maxx (max x maxx))
       (when new-set 
	 (add-member new-set x y))
       (incf x))
      (#\SPACE
       (when (and (< idx lim) (char-equal (aref pic (1+ idx)) #\SPACE))
	 (incf idx))
       (incf x))
      (#\NEWLINE
       (incf y)
       (setq x 1))
      (t  (signal-error (#"bad char ~A in pic" chr))))
    finally
      (cond (new-set
	     (when (eql color :white) (rotatef new-set other-new))
	     (return (values new-set other-new)))
	    (t (return (values (max maxx maxy)(min maxx maxy)))))
    ))

(defun construct-set-from-picture
       (picture &key zone-class return-board-position pathname full-board-size)
  (let* ((real-fbs (scan-picture picture))
	 (fbs (or (cond (full-board-size (max full-board-size real-fbs))
			(return-board-position (max 19 real-fbs))
			(t real-fbs))))
	 (new (make-instance 'set-of-stones
			     :area *go-cons-area*
			     :full-board-size fbs)))
    (re-init new)
    (multiple-value-bind (new other-new) 
	(scan-picture picture new)
      (when return-board-position
	(unless other-new
	  (setq other-new (make-instance 'set-of-stones :full-board-size fbs))
	  (re-init other-new)))
      (cond ((and new other-new)
	     (find-position-from-sets other-new
				      new
				      :create-p t
				      :full-board-size fbs
				      :pathname pathname
				      :zone-class zone-class))
	    (t new))
      )))



(defun locate-shape-from-picture (pic &optional error-p )
  (let* ((set (construct-set-from-picture pic))
	 (desc (when set (shape-description set))))
    (if desc
	(values desc set)
	(when error-p (signal-error ("Shape ~%~A~%not in library"))))))


(defun keep-files-loaded (files &key (periodically t))
  3;; Keep looking for new files, which presumably are being generated
0  3;; on another machine.
0  (fs:with-automatic-login-to-sys-host
    (loop with loaded-files = nil
	  doing
      (loop for file in files
	    as old-files =
	       (catch-error
		 (sort (mapcar #'car (cdr (fs:directory-list file :fast)))
		       #'(lambda (a b)
			   (declare (sys:downward-function))
			   (let ((a (catch-error (read-from-string (send a :type))))
				 (b (catch-error (read-from-string (send b :type)))))
				(if (and (integerp a) (integerp  b))
				    (< a b)
				    (if (integerp b) t (if (integerp a) nil t)))))))
	    do (loop for old-file in old-files
		     unless (member old-file loaded-files)
		       do (catch-error
			    (progn (load old-file)
				   (push old-file loaded-files)))))
      (if periodically
	  (sleep (/ (truncate (* 60 60 60 (if (numberp periodically) periodically 3)))
		    60))
	  (loop-finish))
      )))

(defun load-new-shape-data
       (&optional (base-file "go:go;test-intron-data-purple")
	&key (save nil))
  3;;
0  3;; load a batch of new files
0  3;;
0  (when save (save-fate-data))
  (let* ((base-path (pathname base-file))
	 (wild-path (send base-path :new-pathname :type :wild :version :newest))
	 (old-files (sort (mapcar #'car (cdr (fs:directory-list wild-path :fast)))
			  #'(lambda (a b)
			      (declare (sys:downward-function))
			      (let ((a (catch-error (read-from-string (send a :type))))
				    (b (catch-error (read-from-string (send b :type)))))
				(if (and (integerp a) (integerp  b))
				    (< a b)
				    (if (integerp b) t (if (integerp a) nil t))))))))
    (loop for file in old-files do (load file))))


(defmacro zl-user:modify-shape ( pic &rest args )
  `(multiple-value-bind (desc set)
       (locate-shape-from-picture ,pic)
       (modify-properties desc (shape-normalizer set)
	     ,@(loop for i in args collect `(quote ,i)))))

(defvar *troubled-shapes* nil)
(defun report-shape-trouble (dr sh self p pos)
  (push (list dr self sh p pos) *troubled-shapes*)
  (format t "~&dropped shape ~A (~A) for ~A has positions ~\\move-position\\ ~
				but new has positions ~\\move-position\\"
	  dr sh self p pos)
  nil)
(defmethod (modify-properties shape-info) (normal &rest props &aux val)
  (setq val self)
  (loop with p = props
	and pv and pn and needs-summary
	while p do
	(setq pn (car p))
	(setq pv (cadr p))
	(setq p (cddr p))

        ;; convert user to keyword
	(and (eq (symbol-package pn) (symbol-package 'zl-user:foo))
	     (setq pn (intern pn (symbol-package ':foo))))

	(cond ((null pv) (remf properties pn))
	      ((eq pn ':key-points)
	       (input-key-points self normal
		     (if (integerp pv) (list pv) pv)))
	      ((eq pn ':dropped-shapes)
	       (loop for var in pv
		     with perm = (perm normal)
		     as (dr pos props) = var
		     as xformed-pos = (transform-moves pos perm)
		     as xformed-dr = (transform-intron-to-standard normal dr)
		     as sh = (find-dropped-shape
				      self
				      xformed-dr)
		     as (p s) = sh
		     do
		 (cond ((null s)
			(signal-error ("can't find dropped shape ~A for ~A"
				dr self)))
		       ((and ( p xformed-pos)
			     (with-restart-options (((error "Use the existing positions")
						     nil)
						    ((error "Skip the whole thing")
						     t)
						    ((error "Coerce the positions to agree")
						     (setf (first sh) pos)
						     nil))
						   (report-shape-trouble xformed-dr sh self p
									 xformed-pos))))
		       (t (apply #'modify-properties
			    s
			    normal
			    (cdr props))))
		 (setf (second var) xformed-pos)
		 (setf (first var) xformed-dr)
		 ))
	      ((member pn fate-properties)
	       (setf (getf properties pn)
			(read-combined-fate-property self normal pn pv)))
	      ((member pn obsolete-fate-properties)
	       (setf (getf properties pn)
			(if (or (arrayp (cdr (car pv)))
				(consp (cadr (car pv))))

			    (read-intron-fate-property self normal pn pv)

			    (read-primary-fate-property self normal pv)))
	       (setq needs-summary t))
	      (t (setf (getf properties pn) pv)
		 (and (eq pn ':name) (setq val pv))
		 ))
	finally 
	  (when needs-summary
	    (summarize-discreet-fates
	      self
	      obsolete-fate-properties
	      (first fate-properties)))
	  )
  (setq recently-changed-shapes (delete self recently-changed-shapes :test #'equal))
  (fixup-isomers self)
  val)

(defmethod (summarize-discreet-fates shape-info) (properties output-property)
  ;;
  ;; this converts an old style "3 properties" to a new style "one property" fate
  (split-one-fate self properties)
  (loop for (position . basic-val) in (get-prop self (first properties))
	as alen = (array-total-size basic-val)
	as result = (make-array alen)
	do
    (copy-array-contents basic-val result)
    (loop for new-prop in (cdr properties)
	  as new-val = (loop for (pos . val) in (get-prop self new-prop)
			     when (eql pos position)
			       return val)
	  do
      (loop for idx from 0 below alen
	    as basic-fate-list = (aref result idx)
	    as basic-fate = (if (consp basic-fate-list)
				(car (last basic-fate-list))
				basic-fate-list)
	    as new-fate = (aref new-val idx)
	    do
	(multiple-value-bind (fate ord np libs nil aux)
	    (new-decode-fate basic-fate)
	  (multiple-value-bind (fate2 ord2 np2 libs2 max-libs2 aux2)
	      (new-decode-fate new-fate)
	    (if (and (eql fate fate2)
		     (eql aux aux2)
		     (eql ord ord2)
		     (eql np np2))
		(progn (setq basic-fate (new-encode-fate fate ord np libs (or max-libs2 libs2) aux))
		       (if (consp basic-fate-list)
			   (setf (car (last basic-fate-list))
				 basic-fate)
			   (setf (aref result idx) basic-fate)))
		(setq basic-fate (new-encode-fate fate ord np libs (1- libs2) aux))
		(setq new-fate (new-encode-fate fate2 ord2 np2 libs2 (or max-libs2 libs2) aux2))
		(if (consp basic-fate-list)
		    (let ((ll (last basic-fate-list)))
		      (setf (car ll) basic-fate)
		      (setf (cdr ll) (list new-fate)))
		    (setf (aref result idx) (list basic-fate new-fate))))))))
    collect `(,position . ,result) into val	;
	finally (put-prop self val output-property)
		(share-fate-properties self output-property)))

(defmethod (read-primary-fate-property shape-info) (normal pv)
  ;;old style
  (loop for (val fate on nm lib) in pv
	collect
	  `(,(transform-moves val (perm normal))
	    ,@(let ((arr (make-array
			   (expt 2
				 (intron-array-size self))
			   :area *go-static-area*
			   )))
		(setf (aref arr 0) 
		      (new-encode-fate fate on nm lib))
		arr)))
  )

(defmethod (read-intron-fate-property shape-info) (normal pname pv)
3  ;;intron style, still with discreet properties
0  (loop with xtra-libs = (loop for (n l) in obsolete-fate-properties-and-liberties
			       when (eq n pname) return l)
	and perm = (perm normal)
	and fate-prop-name = (first obsolete-fate-properties)
	with fate-prop-val = (get-prop self fate-prop-name)
	for (val . fate-sum) in pv
	as transformed-val = (transform-moves val perm)
	as ar = nil		;the array we are loading
	as fate-prop-ar = nil	;the corresponding array for :fates
	do
    (labels  ((set-intron (spec val)
		(setq spec (transform-intron-to-standard normal spec))
		(when (eq val fate-prop-name)
		  (multiple-value-bind (fate ord np nl nil nil aux)
		      (new-decode-fate (aref fate-prop-ar spec))
		    (setq val (new-encode-fate fate ord np (when nl (+ nl xtra-libs)) nil aux))))
		(if (aref ar spec)
		    (signal-error ("Intron slot ~A already filled"
			    spec))
		    (setf (aref ar spec) val)))
	      )
      
      (if (arrayp fate-sum)
	  (if (zerop perm)
	      (setq ar fate-sum)
	      (signal-error ("nonzero permutation reading binary fate file")))
	  
	  (setq ar (make-array (expt 2 (intron-array-size self))
			       :area *go-static-area*
			       ))
	  (setq fate-prop-ar
		(loop for (pv . pa) in fate-prop-val
		      when (eql transformed-val (logand transformed-val pv))
			return pa))
	  
	  (loop for (specs fate on nm lib) in fate-sum
		do
	    
	    (if (eq fate fate-prop-name)
		
		(loop for spec in specs
		      do
		  (if (integerp spec)
		      (set-intron spec fate)
		      (loop with (from . to) = spec
			    for idx from from to to
			    do
			(set-intron idx fate))))
		
		(loop with val = (new-encode-fate fate on (or nm 1) lib)
		      for spec in specs
		      do
		  (if (integerp spec)
		      (set-intron spec val)
		      (loop with (from . to) = spec
			    for idx from from to to
			    do
			(set-intron idx val))))
		))))
	collect `(,transformed-val . ,ar)
      ))

(defvar *previous-losing-shape* nil)
(defvar *losing-shapes* nil)
(defmethod (read-combined-fate-property shape-info) (normal ignore pv)
  ;;new style
  (loop with perm = (perm normal)
	for (val . ar) in pv
	as transformed-val = (transform-moves val perm)
	unless (zerop perm)
	  do
	    (unless (assoc self *losing-shapes*)
	      (push (list self perm) *losing-shapes*))
	    (unless (eq self *previous-losing-shape*)
	      (format t "~&losing on ~A" self)
	      (setq *previous-losing-shape* self))
	    (si:with-stack-array (temp (array-total-size ar))
	      (copy-array-contents ar temp)
	      (si:fill-array ar nil nil)
	      (loop for idx from 0 below (array-total-size ar)
		    as encoded-fate = (aref temp idx)
		    do
		(let ((new-idx (transform-intron-to-standard normal idx)))
		  (if (aref ar new-idx)
		      (signal-error ("Intron slot ~A already filled"
			      new-idx))
		      (setf (aref ar new-idx)
			    (transform-ordinal-move-to-standard normal encoded-fate))))))
	collect
	  `(,transformed-val . ,ar)
	  ))

(defun modify-fate (&key
		     moving
		     (fate nil fate-specified-p)
		     (move-at nil)
		     (print-message t)
		     shape
		     (property *fate-property*) (position :c) (introns 0)
		     (liberties 0)
		     (pv :unchanged)
		     (extend-liberties nil)
		     )
  3;;
0  3;; A top level function for tweaking shapes
0  3;;
0  (unless moving (signal-error ("You must specify the :MOVING as first or second")))
  (when (not (consp
		moving))
    (setq moving (if (eq moving t) '(:first :second) (list moving))))
  (map-shape-spec
    #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	(declare (sys:downward-function))
	set
	(loop with ord and aux
	      for move in moving
	      as coded-fate = (fate-of-shape shape x-pos y-pos move property introns)
	      do
	  (multiple-value-bind (liberty-coded-fate base-liberties)
	      (fate-for-outside-liberties coded-fate outside-liberties)
	    (unless liberty-coded-fate
	      (when (not extend-liberties)
		(signal-error
		  ("Modify-fate is extending the liberty range, but :EXTEND-LIBERTIES NIL")
		  ((:ignore :repair))
		  (t (return-from modify-fate nil))))
	      (loop for ol from (1- outside-liberties) downto base-liberties
		    until liberty-coded-fate
		    doing
		(multiple-value-setq (liberty-coded-fate base-liberties)
		  (fate-for-outside-liberties coded-fate ol))
		    finally (loop until ( ol (- base-liberties 2))
				  doing
			      (incf ol)
			      (modify-fate :moving moving
					   :position position
					   :property property
					   :shape shape
					   :liberties ol))
			    (setq coded-fate (fate-of-shape shape x-pos y-pos move property introns))
			    ))

	    (multiple-value-bind (old-fate old-ord old-np nil nil old-aux old-pv)
		(new-decode-fate liberty-coded-fate)
	      
	      (unless fate-specified-p (setq fate old-fate))
	      
	      (if move-at 
		  (cond ((integerp move-at)
			 (setq ord move-at aux nil))
			((consp move-at)
			 (setq ord (first move-at)
			       aux (second move-at)))
			(t (setq ord move-at
				 aux nil)))
		  (setq ord old-ord
			aux old-aux))
	      (when (eq pv :unchanged)
		(setq pv old-pv))
	      
	      (let* ((liberties (+ liberties base-liberties))
		     (new-coded-fate (new-encode-fate fate ord old-np
						      liberties liberties aux pv)))
		(multiple-value-bind (new-fate change-p)
		    (merge-fates coded-fate new-coded-fate)
		  (when change-p
		    (let ((par (parent-shape shape)))
		      (unless (member par recently-changed-shapes)
			(push par recently-changed-shapes))
		      (split-one-fate shape property)
		      (set-fate-of-shape shape x-pos y-pos move
					 new-fate
					 property introns)))
		  (when print-message
		    (format t "~&~\\picture\\ ~A (#o~O) ~A,~A ~A~%~
			              ~A  "
			    (picture shape introns)
			    shape introns x-pos y-pos move
			    property)
		    (print-fate shape liberty-coded-fate t :print-np t :print-libs t)
		    (princ " -> " t)
		    (if change-p
			(print-fate shape new-coded-fate t :print-np t :print-libs t)
			(princ 3"NO CHANGE"0))
		    (print (cf (fate-of-shape shape x-pos y-pos move property introns)))
		    )
		  ))))))
    :shape shape :property property :introns introns :position position :liberties liberties
    :drop-ins nil
    ))

4;;
;; Some useful iterators
;;


0(defun map-libs (fun p&l)
  (declare (sys:downward-funarg fun))
  (unless p&l (setq p&l fate-liberties))
  (if (consp p&l)
      (if (consp (car p&l))
	  (loop for libs in p&l do
	    (funcall fun :outside-liberties libs))
	  (loop for s in p&l do (map-libs fun s)))
      (if (integerp p&l)
	  (funcall fun :outside-liberties p&l) 
	  (signal-error ("Don't understand ~A as a prop spec" p&l)))))

(defun map-positions (fun pos)
  (declare (sys:downward-funarg fun))
  (unless pos (setq pos *all-shape-positions*))
  (if (consp pos)
      (if (consp (car pos))
	  (loop for (xp yp) in pos do (funcall fun :x-pos xp :y-pos yp))
	  (loop for i in pos do (map-positions fun i)))
      (multiple-value-bind (xp yp) (direction-to-position pos)
	(unless (and xp yp) (signal-error ("Can't interpret ~A as a direction" pos)))
	(funcall fun :x-pos xp :y-pos yp))))

(defun map-shapes (fun sh &optional (progress-note "map shapes"))
  (declare (sys:downward-funarg fun))
  (cond ((consp sh)
	 (tv:noting-progress (progress-note)
	     (loop with ns = (length sh)
		   for s in sh
		   as idx from 0
		   do
	       (tv:note-progress idx ns)
	       (map-shapes fun s))))
	((null sh)
	 (map-shapes fun (all-shapes zhash-shape-database) progress-note))
	(t (multiple-value-bind (news set)
	       (find-shape sh)
	     (if (consp news)
		 (map-shapes fun news progress-note)
		 (if news
		     (funcall fun :shape news :set set)
		     (signal-error ("can't coerce ~A into a shape" sh))))))))

(defun map-shapes-and-drop-ins (fun sh pos &optional (drop-ins t) (progress-note "map shapes"))
  (declare (sys:downward-funarg fun))
  (cond ((consp sh)
	 (tv:noting-progress (progress-note)
	   (loop with ns = (length sh)
		 for s in sh
		 as idx from 0
		 do (tv:note-progress idx ns)
		 (map-shapes-and-drop-ins fun s pos drop-ins))))
	((null sh)
	 (map-shapes-and-drop-ins fun (all-shapes zhash-shape-database) pos
				  drop-ins progress-note))
	(t (multiple-value-bind (news set)
	       (find-shape sh)
	     (if (consp news)
		 (map-shapes-and-drop-ins fun news pos drop-ins progress-note)
		 (if news
		     (loop with i-am-a-drop-in
			   for (nil positions sh) in
			       (cond ((loop with parent = (parent-shape news)
					    for spec in (when (neq parent news)
							  (get-prop parent :dropped-shapes))
					    when (eq sh (third spec))
					      return
						(progn (setq i-am-a-drop-in t)
						       (list spec))))
				     (drop-ins (get-prop news :dropped-shapes)))
			   as revised-pos = (let ((val))
					      (map-positions
						#'(lambda (&key x-pos y-pos)
						    (declare (sys:downward-function))
						    (when (logtest
							    (encode-move-geometry :first x-pos y-pos)
							    positions)
						      (push (list x-pos y-pos) val)))
						pos)
					      (nreverse val))
			   when (or revised-pos (null pos))
			     do (funcall fun :shape sh :position revised-pos :set set)
			   finally
			     (unless i-am-a-drop-in
			       (funcall fun :shape news :position pos :set set)))
		     (signal-error ("can't coerce ~A into a shape" sh))))))))

(defun map-introns (fun spec)
  (declare (sys:downward-funarg fun))
  (if (or (arrayp spec)
	  (instancep spec))
      (loop with al = (if (arrayp spec)
			  (array-total-size spec)
			  (expt 2 (intron-array-size spec)))
	    with ref-array = (aref sequences-sorted-by-bits  (1- (integer-length al)))
	    for ref-idx below al
	    do (funcall fun :introns (aref ref-array ref-idx))
	       )
      (if (consp spec)
	  (if (consp (cdr spec))
	      (loop for i in spec do (map-introns fun i))
	      (loop for i from (car spec) to (cdr spec) do (map-introns fun i)))
	  (if (integerp spec)
	      (funcall fun :introns spec)
	      (signal-error ("Can't coerce ~A to an intron spec" spec))))))

(defun map-shape-spec
       (function &key shape property position introns liberties
	(drop-ins t)
	(progress-note "map shape spec")
	)

  (declare (sys:downward-funarg function))
  (unless shape (setq shape (all-shapes zhash-shape-database)))
  (unless property (setq property fate-properties))
  (unless position (setq position *all-shape-positions*))
  (unless introns (setq introns t))
  (unless liberties (setq liberties fate-liberties))
  (map-shapes-and-drop-ins
    #'(lambda (&key shape position set)
	(declare (sys:downward-function))
	(catch 'exit-shape
	  (map-props
	    #'(lambda (&key property)
		(declare (sys:downward-function))
		(catch 'exit-props
		  (map-positions 
		    #'(lambda (&key x-pos y-pos)
			(declare (sys:downward-function))
			(catch 'exit-position
			  (map-libs
			    #'(lambda (&key outside-liberties)
				(declare (sys:downward-function))				
				(catch 'exit-libs
				  (map-introns
				    #'(lambda (&key introns)
					(declare (sys:downward-function))
					(funcall function
						 :shape shape
						 :x-pos x-pos
						 :y-pos y-pos
						 :set set
						 :outside-liberties outside-liberties
						 :property property
						 :introns introns))
				    (if (eq introns t) shape introns))))
			    liberties)))
		    position)))
		property)))
    shape
    position
    drop-ins
    progress-note
    ))

3;;
;; KEY-POINTS is an older idea, from before the shape database
;; was as extensive as it is now.  It's really obsolete and no
;; longer used by the program.
;;
0(defmethod (input-key-points shape-info) (normal arg)
  (set-key-points self
		  (read-key-points self normal arg)))
 
;;
;; convert external to internal form of key points
;;
(defmethod (read-key-points shape-info) (normal mlst)
  (cond ((not (consp
		mlst))
	 (cond ((integerp mlst)
		(multiple-value-list (member-for-ordinal-number normal mlst))
		)
	       (t mlst)))
	(t (cons
	     (read-key-points self normal (car mlst))
	     (read-key-points self normal (cdr mlst))
	     ))))

;;
;; convert internal to external form of key points list
;;
(defmethod (write-key-points shape-info) (mlst)
  (cond ((not (consp
		mlst)) mlst)
	((integerp (car mlst))
	 (apply #'ordinal-number-for-member self mlst))
	(t (cons
	     (write-key-points self (car mlst))
	     (write-key-points self (cdr mlst))
	     ))))



(zl:def-menu-method show-key-points shape-info
  :button :mouse-middle
  :context :view
  :menu-id "show key points"
  :menu-documentation "show the key points list of this shape"
  :test (when (key-points zl-user:item) t)
  (&rest ignore)
  (values (write-key-points self (key-points self)) self))



(defmethod (build-simple-shape-from-introns shape-info)
	   (introns new-shape)
  (re-init new-shape)
  (let* ((first-bit (logand introns (- introns)))
	 (intr (logxor introns first-bit)))

    (multiple-value-bind (x y)
	(member-for-ordinal-number self (integer-length first-bit))
      (add-member new-shape x y)
      (loop as some = nil
	    doing
	(do-for-introns
	  self
	  #'(lambda (x y)
	      (when (is-adjacent new-shape x y)
		(add-member new-shape x y)
		(setq some t)
		(setq intr (logxor intr (lsh 1 (1- (ordinal-number-for-member self x y))))))
	      )
	  intr)
	until (null some)))
      intr))

(defmethod (drop-in-shape-description shape-info) (other x-pos y-pos perm)
  (loop with encoded-pos = (encode-move-geometry :first x-pos y-pos)
	for (introns geo sh) in (get-prop self :dropped-shapes)
	when (and (logtest geo encoded-pos)
		  (loop for i in (get-all-isomers sh)
			    thereis (equal-offset-members other i)))
	  return (values sh perm introns)))


(defmethod (successor-shape-moving-first shape-info)
	   (ordinal-move
	     &key
	     (introns 0)
	     (outside-liberties 0)
	     (x-pos :center)
	     (y-pos :center))

  (cond ((eq ordinal-move :outside)
3	 ;we won't really move outside to decrease our liberties
0	 (values self introns outside-liberties))
	((eq ordinal-move :pass)
	 (values self introns outside-liberties))
	(t
	 (when (logtest (lsh 1 (1- ordinal-move)) introns)
	   (signal-error ("~\\picture\\ move #o~o is already occupied by introns #o~o"
		   (picture self introns
			    :point ordinal-move
			    :point-char #\*)
		   ordinal-move introns)))
	 (let ((new-shape)
	       (new-introns 0))
	   (with-group-resources ((new 'set-of-stones))
	     (copy-set self nil new)
	     (let ((parent (parent-shape self)))
	       (multiple-value-bind (x y)
		   (member-for-ordinal-number parent ordinal-move)
		 (remove-member new x y)
		 (multiple-value-setq (new-shape new-introns)
		   (track-new-shape (shape-normalizer self) new introns 0 x-pos y-pos))
		 )))
	   (values new-shape new-introns outside-liberties)
	 ))))

(defmethod (track-new-shape normalizer)
	   (new introns drop-ins &optional x-pos y-pos)

  (unless (and x-pos y-pos)
    (multiple-value-setq (x-pos y-pos) (normalized-edge-position self)))

  (let (drop-in non-drop-in new-shape new-permutation new-drop-ins)

    (unless (zerop drop-ins)
      (do-for-introns
	shape-description
	#'(lambda (x y)
	    (multiple-value-bind (x y) (transform-to-board-coordinates self x y)
	      (if (drop-in-position-p new x y)
		  (push (list x y) drop-in)
		  (push (list x y) non-drop-in))))
	drop-ins)
      (loop for (x y) in non-drop-in do (remove-member new x y))
      (unless drop-in
	(setq drop-ins 0)))


    (multiple-value-setq (new-shape new-permutation new-drop-ins)
      (if (simply-connected-p new)
	  (shape-description-and-permutation new)
	  (drop-in-shape-description shape-description new x-pos y-pos perm)))

    (cond ((null new-shape)
	   nil)
	  (t
	   (unless new-drop-ins (setq new-drop-ins 0))
	   (let* ((parent (parent-shape new-shape))
		  (norm (make-normalizer new-shape
					 new
					 (make-instance 'normalizer
							:target new)
					 new-permutation
					 parent
					 ))
		  new-introns
		  )
	     (when drop-in
	       (multiple-value-setq (x-pos y-pos) (normalized-edge-position norm))
	       (loop for (x y) in drop-in do (remove-member new x y) )
	       (multiple-value-setq (new-shape new-permutation new-drop-ins)
		 (drop-in-shape-description (shape-description norm)
					    new x-pos y-pos (perm norm)))
	       (loop for (x y) in drop-in do (add-member new x y))
	       (setq norm (make-normalizer new-shape
					   new
					   (make-instance 'normalizer
							  :target new)
					   new-permutation
					   parent
					   ))
	       )
	     (setq new-introns (translate-introns self norm introns))

	     (cond ((eq new-shape parent)
		    (when (= (aref *number-of-ones* new-introns)
			     (size new-shape))
		      (setq new-introns 0)))
		   (t 
		    ;determine which, if any, introns are captured
		    (with-group-resource
		      (intron-set 'set-of-stones
				  (set-full-board-size intron-set
						       (full-board-size new-shape)))
		      (loop with intr = new-introns
			    and final-intr = 0
			    as old-intr = intr
			    until (zerop intr)
			    do
			(setq intr (build-simple-shape-from-introns
				     parent
				     intr intron-set))
			(when (block cap
				(do-for-all-adjacent
				  intron-set
				  #'(lambda (x y)
				      (when (is-member new-shape x y)
					(return-from cap t)))))
			  (setq final-intr
				(logior final-intr (- old-intr intr))))
			    finally (setq new-introns final-intr)
				    ))
		    ))
	     (values new-shape new-introns new-drop-ins norm)
	     )))))


(defmethod (translate-introns normalizer) (normalizer introns)
  (let ((sum 0))
    (do-for-introns
      shape-description
      #'(lambda (x y)
	  (multiple-value-bind (x y)
	      (transform-to-board-coordinates self x y)
	    (setq sum (logior sum
			    (lsh 1 (1- (ordinal-number-for-member 
					 normalizer x y)))))
	  ))
      introns)
    sum))

(defmethod (successor-shape-moving-second shape-info)
	   (ordinal-move introns outside-liberties)


  (cond ((eq ordinal-move :outside)
	 (values self introns (max 0 (1- outside-liberties))))
	((eq ordinal-move :pass)
	 (values self introns outside-liberties))
	(t
	 (let ((bit (lsh 1 (1- ordinal-move))))
	   (when (logtest bit introns)
	     (signal-error ("~\\picture\\ move #o~o is already occupied by introns #o~o"
		     (picture self introns
			      :point ordinal-move
			      :point-char #\*)
		     ordinal-move introns)))
	   (values self (logior bit introns) outside-liberties)
	   ))))



(defmethod (ko-threat-p shape-info)
	   (&key (x-pos :center)
		 (y-pos :center)
		 (introns 0)
		 (outside-liberties 0)
		 (property *fate-property*)
		 (move :first))
  3; I don't think this is completely accurate, but
0  3;it seems to work OK when the shapes queried are
0  3;dead shapes to begin with.

0  (let* ((fate (fate-of-shape self x-pos y-pos move property introns))
	 (l-fate (fate-for-outside-liberties fate outside-liberties))
	)
    (multiple-value-bind (first-fate ord) (new-decode-fate l-fate)
    (multiple-value-bind (new-shape new-introns new-libs)
	(if (eq move :first)
	    (successor-shape-moving-first self ord
					  :x-pos x-pos
					  :y-pos y-pos
					  :introns introns
					  :outside-liberties outside-liberties)
	    (successor-shape-moving-second self ord introns outside-liberties))

      (let* ((next (when new-shape
		     (fate-of-shape new-shape x-pos y-pos move property new-introns)))
	     (l-next-fate (fate-for-outside-liberties next new-libs))
	     (next-fate (new-decode-fate l-next-fate))
	     (val (eq (case next-fate
			((:impossible :dead-with-eye) :dead)
			((:unknown :alive-with-eye) :alive)
			(otherwise next-fate))
		      (case first-fate
			(:dead-with-eye :dead)
			(:alive-with-eye :alive)
			(otherwise first-fate)))))
	(not val)))
    )))


3#||


4Building and maintaining the shape database

3the basic shapes are generated automatically, but the information
0about3 those shapes is much to voluminous to generate on the fly.
The following bunch of things were used to generate the original
data and massage it into its current form.

4How to build the shape database

3 First, run CALCULATE-FATES-WITH-INTRONS.  This takes about three weeks.
 While you are waiting, you'll want to periodically write out the intermediate
 results.

 Merge together all the results, then start auditing.  The top level 
 auditing function is SHAPE-ABNORMALITIES, but there are numberous gems
 here that look for abnormal or undesirable conditions.

 When all the results are correct, run SET-TERMINAL-POSITIONS to determine which
 of the shapes are terminal.  Then run SET-NON-TERMINAL-POSITIONS to build the
 rest of the game tree.

0(defun copy-fate-property-contents (&key old-prop new-prop shape)
  (map-shape-spec
    #'(lambda (&key shape set x-pos y-pos property introns outside-liberties)
	(ignore set outside-liberties)
	(loop for moving in '(:first :second) do
	  (multiple-value-bind (nil new-array)
	      (fate-of-shape shape x-pos y-pos moving property introns)
	    (multiple-value-bind (nil old-array)
		(fate-of-shape shape x-pos y-pos moving old-prop introns)
	      (unless (and old-array new-array)
		(signal-error ("didn't get both")))
	      (copy-array-contents old-array new-array)))))
    :liberties 0
    :introns 0
    :shape shape
    :property new-prop))



(defun copy-fate-data (old-prop new-prop)
  (do-for-shapes-and-drop-ins
    zhash-shape-database
    #'(lambda (shape)
	(declare (sys:downward-function))
	(put-prop shape
		  (loop for (prop . val) in (get-prop shape old-prop)
			as new-arr = (make-array (array-length val)
						 :area *go-static-area*)
			do (copy-array-contents val new-arr)
			collect `(,prop . ,new-arr))
		  new-prop)))
  )

(defun clear-fate-data (&optional (props fate-properties))
  (setq recently-changed-shapes nil)
  (do-for-shapes-and-drop-ins
    zhash-shape-database
    #'(lambda (shape)
	(declare (sys:downward-function))
	(loop for prop in props do
	  (put-prop shape nil prop)))))


(defun compare-fates-properties (old-prop new-prop)

  (loop for shape in (all-shapes zhash-shape-database)
	as old-val = (get-prop shape old-prop)
	as new-val = (get-prop shape new-prop)
	do
    (loop for (old-spec . old-arr) in old-val
	  as (new-spec . new-arr) = (assoc old-spec new-val)
	  as old-len = (array-length old-arr)
	  as new-len = (array-length new-arr)
	  do
      (unless (and (eql old-len new-len)
		   (loop for i from 0 below old-len
			 always (eql (aref old-arr i) (aref new-arr i))))
	(signal-error ("failed"))
	))))

(defun compare-fate-properties (&key old-prop new-prop qualitative introns position shape
				repair
				)
  (let ((coerce-shape repair)
 	(last-shape))
  (map-shape-spec 
    #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	(declare (sys:downward-function))
	set outside-liberties
	(unless (eql shape last-shape)
	  (setq coerce-shape repair))
	(setq last-shape shape)
	(loop with f1q and f2q
	      as coerce-position = nil
	      with coerce-one = (or coerce-shape coerce-position)
	      for moving in '(:first :second) 
	      as f1 = (fate-of-shape shape x-pos y-pos moving property introns)
	      as f2 = (fate-of-shape shape x-pos y-pos moving new-prop introns)
	      unless (or (equal f1 f2)
			 (loop for i from 0
			       as f1l = (fate-for-outside-liberties f1 i)
			       as f2l = (fate-for-outside-liberties f2 i)
			       do
			   
			   (setq f1q (new-decode-fate f1l)
				 f2q (new-decode-fate f2l))
			       when (or (and (eq f1q :unknown)(eq f2q :unknown))
					(null f1l)(null f2l))
				 return t
			       when (and (not (or (and (memq f1q '(:impossible :unknown))
						       (memq f2q '(:impossible :unknown)))
						  (and qualitative (eq f1q f2q))))
					 (not (eql f1l f2l)))
				 return nil))
		do
		  (format t "~&~%~\\picture\\ ~A (#o~O) ~A ~A,~A~&~A ->~&~A~%"
			  (picture shape introns) shape introns moving x-pos y-pos 
			  (cf f1)
			  (cf f2))
		  (when repair
		    (place-surrounded-in-position game shape x-pos y-pos 0 introns)
		    (update)
		    (with-restart-options
		      (((error "continue") (setq coerce-one nil))
		       ((error "undo the change")
			(setq coerce-one t))
		       ((error "undo the changes for the rest of this position")
			(setq coerce-position t
			      coerce-one t))
		       ((error "undo the changes for the rest of this shape")
			(setq coerce-shape t
			      coerce-one t))
		       ((error "recalculate")
			(calculate-one-fate shape
					    :property new-prop
					    :x-pos x-pos
					    :y-pos y-pos
					    :moving (list moving)
					    :outside-liberties 0
					    :introns introns
					    :forced t))
		       )
		    (ferror "they're diffrent")))
		  (when coerce-one
		    (set-fate-of-shape shape x-pos y-pos moving f1 new-prop introns))
		  ))
    :shape shape :property old-prop :introns introns :position position
    :liberties 0
    )))





(defun calculate-fates-with-introns (&key (priority -2) forced (drop-ins t)
				     shape property position introns)
  


;  Estimated time to compute, in days
;
;    (// (* 2 ;seconds per position
;       9 ;places on board
;       3 ;outside liberties 0,1,2
;       (loop for i in (all-shapes zhash-shape-database) sum (expt 2 (intron-array-size i)))
;       )
;    (* 24 60 60.0))
;
  (let ((old-priority (send tv:current-process :priority))
	last-shape last-liberties last-x-pos last-y-pos)
    (unwind-protect
	(progn
	  (when priority (send tv:current-process :set-priority priority))
	  
	  (map-shape-spec
	    #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
		(declare (sys:downward-function))
		set
		(let ((was-calculated
			(calculate-one-fate shape
					    :x-pos x-pos
					    :y-pos y-pos
					    :property property
					    :outside-liberties outside-liberties
					    :introns introns
					    :same-position (and (eql shape last-shape)
								(eql x-pos last-x-pos)
								(eql y-pos last-y-pos))
					    :forced forced
					    :same-liberties (eql outside-liberties
								 last-liberties))))
		  (if was-calculated
		      (setq last-liberties outside-liberties
			    last-x-pos x-pos
			    last-y-pos y-pos
			    last-shape shape)
		      (setq last-liberties nil
			    last-x-pos nil
			    last-y-pos nil
			    last-shape nil))
		))
	    :shape shape :property property :introns introns :position position
	    :drop-ins drop-ins
	    )
	  (send tv:current-process :set-priority old-priority)
	  )
      (send tv:current-process :set-priority old-priority)
      )))

3;;
;; This function looks for positions where extending the data beyond
;; the standard two liberties may affect the result.  It's not perfect.
;;
0(defun may-benefit-more-liberties (&key try-it verbose quiet
				     shape property position introns liberties)
  
  (map-shape-spec
    #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	(declare (sys:downward-function))
	set
	(when (= outside-liberties 0)
	  (throw 'exit-libs nil))
	(loop for retried from 0 to 1
	      named retry
		do
	  (loop with no-benefit and some-benefit and must-try and some-improves
		for move in '(:first :second)
		as encoded-fate =(fate-of-shape shape x-pos y-pos move property introns)
		as prev-fate = (fate-for-outside-liberties
				 encoded-fate
				 (1- outside-liberties))
		as this-fate = (fate-for-outside-liberties
				 encoded-fate
				 outside-liberties)
		as next-fate = (fate-for-outside-liberties
				 encoded-fate
				 (1+ outside-liberties))
		as decoded-this-fate = (new-decode-fate this-fate)
		as decoded-prev-fate = (new-decode-fate prev-fate)
		as decoded-next-fate = (new-decode-fate next-fate)
		when (and (not (memq decoded-next-fate '(:unknown :impossible)))
			  (not (memq decoded-prev-fate '(:unknown :impossible)))
			  (neq decoded-next-fate decoded-prev-fate)
			  (eq decoded-this-fate decoded-prev-fate)
			  )
		  do (push `(,move
			     ,decoded-prev-fate
			     ,decoded-this-fate
			     ,decoded-next-fate)
			   some-improves )
		when (not (memq decoded-this-fate '(:unknown :impossible)))
		  do
		    (let* ()
		      (cond ((null next-fate)
			     (unless (and (neq decoded-this-fate :dead-with-eye)
					  (neq decoded-this-fate :seki)
					  (eq decoded-this-fate decoded-prev-fate))
			       (setq must-try t)))
			    ((eq decoded-next-fate
				 decoded-this-fate)
			     (setq no-benefit t))
			    (t (push `(,move ,decoded-this-fate ,decoded-next-fate)
				     some-benefit))))
		finally
		  (when (or some-benefit
			    (and verbose must-try)
			    some-improves
			    #+maclisp no-benefit)
		    (cond (must-try
			   (when verbose
			     (format t "~&~%~\\picture\\ ~A (#o~O) ~A,~A ~&"
				     (picture shape introns) shape introns x-pos y-pos )
			     (format t "May benefit from ~A to ~A"
				     outside-liberties
				     (1+ outside-liberties))))
			  (some-benefit
			   (unless (and quiet (= retried 0))
			     (format t "~&~%~\\picture\\ ~A (#o~O) ~A,~A ~&"
				     (picture shape introns) shape introns x-pos y-pos )
			     (format t "benefits from ~A to ~A ~:{ ~A from ~A  to ~A ~}"
				     outside-liberties
				     (1+ outside-liberties)
				     some-benefit)))
			  (some-improves
			   (unless (and quiet (= retried 0))
			     (format t "~&~%~\\picture\\ ~A (#o~O) ~A,~A ~&"
				     (picture shape introns) shape introns x-pos y-pos )
			     (format t 
				     "IMPROVES from ~A to ~A but not from ~A to ~A~&     ~:{ ~A ~A -> ~a -> ~a~}"
				     (1- outside-liberties) (1+ outside-liberties)
				     (1- outside-liberties) outside-liberties
				     some-improves))
			   ))
		    )
		  (if (and must-try try-it (= retried 0))

		    (calculate-one-fate shape :property property
					:x-pos x-pos
					:y-pos y-pos
					:forced t
					:outside-liberties (1+ outside-liberties)
					:introns introns)

		    (return-from retry nil))
		  )))
    :shape shape :property property :introns introns :position position
    :liberties liberties
    )
  )

3;;
;; This function is intended to repair damage or extend the data beyond
;; two liberties.  Basicly, :DEAD-WITH-EYE and :SEKI must become :DEAD
;; at some point (with enough outside liberties).  This function looks 
;; for positions that terminate with an unviable fate, prune it back,
;; and extend from there.
;;
0(defun last-liberties-fate (&key fixit (fate '(:dead-with-eye :seki)) (quiet t)
			    shape property position introns)
  (when (nlistp fate)
    (setq fate (list fate)))
  (map-shape-spec
    #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	(declare (sys:downward-function))
	set outside-liberties 
	(loop with something-to-fix and min-fate = 100
	      for move in '(:first :second)
	      as encoded-fate = (fate-of-shape shape x-pos y-pos move property introns)
	      do
	  (multiple-value-bind (final-fate-spec outside-libs)	      
	      (last-fate-for-outside-liberties encoded-fate )
	    (multiple-value-bind (final-fate ord np nl1 nl2 aux)
		(new-decode-fate final-fate-spec)
	      (when (memq final-fate fate)
		(unless quiet
		  (format
		    t
		    "~&~%~\\picture\\ ~A (#o~O) ~A,~A~%~
    			finally ~A with ~d libs, moving ~A~&"
		    (picture shape introns) shape introns x-pos y-pos
		    final-fate outside-libs move))
		(when (and fixit (> nl2 nl1))
		  (let* ((base (- nl2 outside-libs))
			 (u-fate (new-encode-fate :unknown ord np
					      (max (+ base 3) (1+ nl1))
					      (max (+ base 3) nl2)
					      aux))
			 (new-fate (merge-fates encoded-fate u-fate)))
		    (when (and (listp new-fate)
			       (eq (new-decode-fate (car (last new-fate))) :unknown))
		      (setq new-fate (butlast new-fate)))
		    (when (and (listp new-fate)
			       (null (cdr new-fate)))
		      (setq new-fate (car new-fate)))
		    (set-fate-of-shape shape x-pos y-pos move new-fate property introns)
		    (setq something-to-fix t)
		    (setq min-fate (min min-fate (max 2 (- nl1 base))))
		    )))
	      ))
	  finally
	    (when something-to-fix
	      (may-benefit-more-liberties :try-it t
					  :verbose nil
					  :quiet quiet
					  :shape shape
					  :property property
					  :position (list (list x-pos y-pos))
					  :introns introns
					  :liberties (loop for i from min-fate to
							       (max 3 (+ min-fate 1))
							   collect i)
					  )
	      )))
    :shape shape
    :property property
    :introns introns
    :position position
    :liberties 0
  ))


(defun too-much-seki (&key fixit (fate '(:dead-with-eye :seki)) (quiet t)
			    shape property position introns)
  (when (nlistp fate)
    (setq fate (list fate)))
  (map-shape-spec
    #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	(declare (sys:downward-function))
	set outside-liberties 
	(loop with something-to-fix 
	      for move in '(:first :second)
	      as encoded-fate = (fate-of-shape shape x-pos y-pos move property introns)
	      when (and (listp encoded-fate)
			(loop for i in encoded-fate
			      always (eq (new-decode-fate i) :seki)))
	      do (when fixit (setq something-to-fix t))
		 (when (or fixit (not quiet))
		   (format
		     t
		     "~&~%~\\picture\\ ~A (#o~O) ~A,~A moving ~A~%~
    			is ~A"
		     (picture shape introns) shape introns x-pos y-pos move
		     (cf encoded-fate)))
		 (set-fate-of-shape shape x-pos y-pos move
				    (multiple-value-bind (nil nil np nl1)
					(new-decode-fate (first encoded-fate))
				      (new-encode-fate :unknown :outside 
						   np nl1 nl1))
				    property introns)
	  finally
	    (when something-to-fix
	      (loop for liberties from 0 to 2
		    as first-time = nil then t
		    do
		(calculate-one-fate shape
				    :property property
				    :forced t
				    :x-pos x-pos
				    :y-pos y-pos
				    :outside-liberties liberties
				    :introns introns
				    :same-position first-time))
	      (may-benefit-more-liberties :try-it t
					  :verbose nil
					  :quiet quiet
					  :shape shape
					  :property property
					  :position (list (list x-pos y-pos))
					  :introns introns
					  :liberties '(2 3 4)
					  )
	      (let ((new-fate-1 (fate-of-shape shape x-pos y-pos
					       :first property introns))
		    (new-fate-2  (fate-of-shape shape x-pos y-pos :second
						property introns)))
		(when quiet
		  (format
		    t
		    "~&~%~\\picture\\ ~A (#o~O) ~A,~A~%"
		    (picture shape introns) shape introns x-pos y-pos
		    ))
		(format t "~&Moving first ~A" (cf new-fate-1))
		(format t "~&Moving second ~A" (cf new-fate-2))
		)
	      )))
    :shape shape
    :property property
    :introns introns
    :position position
    :liberties 0
  ))

(defun shape-result-abnormalities (old-prop &key (all t) 
				   repair qualitative 
				   (dead-with-eye-is-same qualitative)
				   shape property position introns
				   (progress-note "result abnormalities"))
  (map-shape-spec
    #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	(declare (sys:downward-function))
	set 
	(loop for move in '(:first :second) do
	    (loop with retry = nil
		  as rep first repair then (when repair t)
		  as encoded-fate-1 = (fate-for-outside-liberties 
					(fate-of-shape shape x-pos y-pos
						       move old-prop introns)
					outside-liberties)
		  as encoded-fate-2 = (fate-for-outside-liberties
					(fate-of-shape shape x-pos y-pos
						       move property introns)
					outside-liberties)
		  do
	      (multiple-value-bind (fate1 ord1 np1 libs1 nil aux1 pv1)
		  (new-decode-fate encoded-fate-1)
		libs1
		(multiple-value-bind (fate2 ord2 np2 libs2 nil aux2 pv2)
		    (new-decode-fate encoded-fate-2)
		  (labels ((report-error (msg)
			     (format t "~&~\\picture\\ ~A  (#o~O) ~a libs  ~A,~A ~A~%~
			              ~A  ~a ~a -> ~a ~a~&"
				     (picture shape introns)
				     shape introns outside-liberties
				     x-pos y-pos move
				     msg old-prop
				     (print-fate shape encoded-fate-1 nil)
				     property
				     (print-fate shape encoded-fate-2 nil)
				     )
			     (unless all (throw 'shape t))))
		    
		    (when (and (not (memq fate2 '(:unknown :impossible)))
			       (not (memq fate1 '(:unknown :impossible)))
			       )
		      
		      (if (and (= np1 np2)
			       (cond ((eql fate1 fate2))
				     ((and dead-with-eye-is-same
					   (or (and 
						 (memq fate1 '(:dead :dead-with-eye))
						 (memq fate2 '(:dead :dead-with-eye)))
					       (and (memq fate1 '(:alive :alive-with-eye))
						    (memq fate2 '(:alive :alive-with-eye))))))
				     )
			       (or qualitative
				   (and (eql ord1 ord2)
					(eql pv1 pv2)
					(eql aux1 aux2))))
			  
			  (when retry
			    (format t "~&repair succeeded - fate is now ~A~%" fate2)
			    (setq retry nil))
			  
			  (block report
			    (when (neq rep :pause)
			      (report-error
				(format nil "~A~A~A"
					(if (= np1 np2) ""
					    "Number of adjacent groups changed")
					(if (cond ((eql fate1 fate2))
						  ((and dead-with-eye-is-same
							(or (and 
							      (memq fate1 '(:dead :dead-with-eye))
							      (memq fate2 '(:dead :dead-with-eye)))
							    (and (memq fate1 '(:alive :alive-with-eye))
								 (memq fate2 '(:alive :alive-with-eye)))))))
					    ""
					    " Score changed ")
					(if (and (not qualitative)
						 (or (not (eql ord1 ord2))
						     (not (eql aux1 aux2))))
					    " Best move changed " "")
					))))
			  
			  (when repair
			    (if (and retry (neq retry :again))
				
				(block restart
				  (setq retry nil)
				  (catch-error-restart (error "continue with next intron")
				    (catch-error-restart (error "Retry")
				      (catch-error-restart (error "Coerce it to agree")
					(signal-error ("Repair failed for ~A" shape))
					(return-from restart nil))
				      (set-fate-of-shape
					shape x-pos y-pos move
					(new-encode-fate fate1 ord2 np2 libs2 nil aux2 pv2)
					property introns))
				    (setq retry :again)))
				
				(if (eq rep :pause)
				    (place-surrounded-in-position game shape
								  x-pos y-pos
								  outside-liberties
								  introns)
				    (calculate-one-fate shape
							:property property
							:forced t
							:x-pos x-pos
							:y-pos y-pos
							:outside-liberties outside-liberties
							:introns introns))
				(setq retry t)))
			  )))
		  ))
		  while retry )
			       
	    )
	  )
    :shape shape :property property :introns introns :position position
    :progress-note progress-note
    ))

(defun shape-liberties-abnormalities (&key (all t) repair quiet
				   shape property position introns
				   (progress-note "liberties abnormalities")
				   )
  
  (map-shape-spec
    #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	(declare (sys:downward-function))
	set outside-liberties
	(loop for move in '(:first :second) do
	  (let ((fate (fate-of-shape shape x-pos y-pos move property introns)))


	    3;remove lists which have become redundant
0	    (when (and (listp fate) (null (cdr fate)))
	      (setq fate (car fate))
	      (set-fate-of-shape
		shape x-pos y-pos move
		fate
		property introns t))


	    (when (listp fate)
	      (loop named base
		    with base-liberties = (ldb new-libs-byte-spec (first fate))
		    for idx from 0
		    do
		(loop with retry = nil
		      as encoded-fate = (nth idx fate)
		      as next-encoded-fate = (nth idx (cdr fate))
		      when (null next-encoded-fate) do (return-from base nil)
		      do
		  (multiple-value-bind (fate1 ord1 np1 libs1 libs12 aux1 pv1)
		      (new-decode-fate encoded-fate)
		    (multiple-value-bind (fate2 ord2 np2 libs2 libs22 aux2 pv2)
			(new-decode-fate next-encoded-fate)
		      (labels ((report-error (msg)
				 (unless quiet
				   (format t "~&~\\picture\\ ~A (#o~O) ~A,~A ~A~%~
			              ~A  ~a libs ~a  -> ~a libs ~a~&"
					   (picture shape introns)
					   shape introns x-pos y-pos move
					   msg
					   (- libs12 base-liberties) fate1
					   (- libs2 base-liberties) fate2
					   ))
				 (unless all (throw 'exit-shape t))))
			
			(when (and (not (memq fate2 '(:unknown :impossible)))
				   (not (memq fate1 '(:unknown :impossible))))
			  
			  (when (and (eq :seki fate1)
				     (eq :seki fate2)
				     (eq move :first)
				     (eq :pass ord1)
				     (neq :pass ord2))
			    (report-error "Seki need not move outside")
			    (setq ord2 :pass
				  aux2 nil)
			    (setq fate (copylist fate))
			    (let ((ff (nthcdr (1+ idx) fate)))
			      (setf (car ff)
				    (new-encode-fate fate1 ord1 np2 libs2 libs22 aux2 pv2))
			      (set-fate-of-shape shape x-pos y-pos move
						 fate property introns t)
			      ))

			  (when (and (eq move :first) (eq fate2 fate1)
				     (eq ord1 :pass) (neq ord2 :pass))
			    (report-error "Continue passing")
			    (setq ord2 :pass
				  aux2 nil)
			    (setq fate (copylist fate))
			    (let ((ff (nthcdr (1+ idx) fate)))
			      (setf (car ff)
				    (new-encode-fate fate1
						     ord1 np2 libs2 libs22 aux2 pv2))
			      (set-fate-of-shape shape x-pos y-pos move
						 fate property introns t)
			      ))
			  (when (and (eq move :second)
				     (neq fate1 :seki)
				     (eq ord1 :pass)
				     (neq aux1 :benson))
			    (setq ord1 ord2
				  aux1 aux2)
			    (setq fate (copylist fate))
			    (let ((ff (nthcdr idx fate)))
			      (setf (car ff)
				    (new-encode-fate fate1
						     ord1 np1 libs1 libs12 aux1 pv1))
			      (set-fate-of-shape shape x-pos y-pos move
						 fate property introns t)
			      )
			    (report-error "moving second shouldn't pass")
			    )

			  (when (and (eq move :second) (eq ord2 :pass)
				     (neq ord1 :pass))
			    (report-error "Shouldn't pass second")
			    (setq ord2 ord1
				  aux2 nil)
			    (setq fate (copylist fate))
			    ;(print (cf fate))
			    (let ((ff (nthcdr (1+ idx) fate)))
			      (setf (car ff)
				    (new-encode-fate fate1
						     ord1 np2 libs2 libs22 aux2 pv2))
			      ;(when (neq ord2 :outside) (dbg))
			      (set-fate-of-shape shape x-pos y-pos move
						 fate property introns t)
			      ))

			  (when (and (eq fate1 fate2)
				     (eql ord1 ord2)
				     (eql aux1 aux2)
				     (eql np1 np2)
				     (eql pv1 pv2)
				     (eql libs2 (1+ libs12)))
			    (let ((ff (nthcdr idx fate)))
			      (unless quiet
				(format t "~&compacting ~A (#o~O) ~a,~a ~A+~A"
					shape introns x-pos y-pos
					(multiple-value-list (new-decode-fate encoded-fate))
					(multiple-value-list (new-decode-fate next-encoded-fate))))
			      (setf (car ff)
				    (new-encode-fate fate1 ord1 np1 libs1 libs22 aux1 pv1))
			      (setf (cdr ff) (cddr ff))))
				       
			  (if (and (= np1 np2)
				   (> libs2 libs12)
				   ( libs12 libs1)
				   ( libs22 libs2)
				   (or (eql fate1 fate2)
				       (>= (outcome-to-score fate2)
					   (outcome-to-score fate1))))

			      (when retry
				(unless quiet
				  (format t "~&repair succeeded - fate is now ~A~%" fate2))
				(setq retry nil))
			      
			      (report-error
				(format nil "~A~A~A"
					(if (= np1 np2) ""
					    "Number of adjacent groups changed")
					(if (or (eql fate1 fate2)
						(>= (outcome-to-score fate2)
						    (outcome-to-score fate1))) ""
					    "Score got worse")
					(if (and (> libs2 libs12)
						 ( libs12 libs1)
						 ( libs22 libs2)) ""
					    "Liberties ordering is confused")
					))


			      (when retry
				(setq retry nil)
				(with-restart-options
				  (((error "continue with next-intron")
				    (setq retry :exit))
				   ((error "retry"))
				   ((error "Coerce second to agree with first")
				    (setq fate
					  (merge-fates
					    fate
					    (new-encode-fate fate1 ord2 np2 libs2 libs22
							     aux2 pv2)))
				    (set-fate-of-shape
				      shape x-pos y-pos move fate property introns t)
				    (setq retry t)
				    )
				   ((error "Coerce first to agree with second")
				    (setq fate
					  (merge-fates
					    fate
					    (new-encode-fate fate2 ord1 np1 libs1 libs12
							     aux1 pv1)))
				    (set-fate-of-shape
				      shape x-pos y-pos move fate property introns t)
				    (setq retry t)				    
				    ))
				  (signal-error ("Repair failed for ~A" shape))))

			      (when (and repair (null retry))
				(setq retry t)
				(if (and (eq repair :seki)
					 (eq fate1 :seki)
					 (eq fate2 :dead))

				    (multiple-value-bind (new-fate change-p)
					(merge-fates
					  fate
					  (new-encode-fate fate1 ord2 np2 libs2 libs22 aux2))
				      (if change-p
					  (set-fate-of-shape
					    shape x-pos y-pos move
					    new-fate
					    property introns t)
					  (signal-error ("didn't change!"))))

				    (calculate-one-fate shape
							:property property
							:forced t
							:x-pos x-pos
							:y-pos y-pos
							:outside-liberties
							(- libs2 base-liberties)
							:introns introns))
				(setq fate (fate-of-shape
					     shape
					     x-pos y-pos move property introns))
				(setq retry t)
				)
			      )))))
		    while (eq retry t)
		    )
		  ))
	    )))
    :shape shape :property property :introns introns :position position
    :liberties 0
    :progress-note progress-note
    ))

(defun flush-seki (&key  shape property position introns (moves '(:first :second)) quiet)
  (let ((ufate (new-encode-fate :unknown :pass 0 )))
    (map-shape-spec
      #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	  (declare (sys:downward-function))
	  set outside-liberties
	  (loop for move in moves do
	    (block fa
	      (let ((fate (fate-of-shape shape x-pos y-pos move property introns)))
		(when (cond ((listp fate)
			     (loop for i in fate
				   thereis (eq (new-decode-fate i) :seki)))
			    (t (eq (new-decode-fate fate) :seki)))
		  (unless quiet
		    (format t "~&~%Flush seki for ~A ~A,~A ~A #o~o~&~A"
			    shape x-pos y-pos move introns
			    (cf fate)))
		  (set-fate-of-shape shape x-pos y-pos move
				     ufate
				     property introns)
		  (return-from fa nil))
		))))
      :shape shape :property property :introns introns :position position
      :liberties 0
      )))

3;;
;; Set the terminal position data for the whole data base
;;
0(defun map-liberty-range (fun &key  shape x-pos y-pos set property introns)
  (let* ((fate-1 (fate-of-shape shape x-pos y-pos :first property introns))
	 (fate-2 (fate-of-shape shape x-pos y-pos :second property introns))
	 (first-fate-1 (ldb new-libs-byte-spec
			    (if (listp fate-1) (car fate-1) fate-1)))
	 (last-fate-1 (ldb new-libs2-byte-spec
			   (if (listp fate-1) (car (last fate-1)) fate-1)))
	 (first-fate-2 (ldb new-libs-byte-spec
			    (if (listp fate-2) (car fate-2) fate-2)))
	 (last-fate-2 (ldb new-libs2-byte-spec
			   (if (listp fate-2) (car (last fate-2)) fate-2))))
    (loop for i from 0 to (max (- last-fate-1 first-fate-1)
			       (- last-fate-2 first-fate-2))
	  do
      (funcall fun
	       :outside-liberties i
	       :real-liberties (+ first-fate-1 i)
	       :set set
	       :shape shape :x-pos x-pos :y-pos y-pos
	       :l-first-fate (fate-for-outside-liberties fate-1 i)
	       :l-second-fate (fate-for-outside-liberties fate-2 i)
	       :introns introns
	       :property property))))

(defun set-terminal-positions (&key (force-pass :ask) shape)
  3;determine which positions are terminal.
0  3;in addition, set the current move to :PASS if there isn't
0  3;even a Ko threat
0  (let ((unknown 0)
	(total 0)
	(ko-threats 0)
	(not-killable 0)
	(terminal 0))
  (map-shape-spec
    #'(lambda (&key shape set x-pos y-pos outside-liberties property introns)
	(ignore outside-liberties)
	(map-liberty-range
	  #'(lambda (&key shape set x-pos y-pos outside-liberties property introns
		     real-liberties l-first-fate l-second-fate )
	      (ignore set outside-liberties)
	      (let ((first-fate (fate-of-shape shape x-pos y-pos :first property introns))
		    (second-fate (fate-of-shape shape x-pos y-pos :second property introns)))
	      (multiple-value-bind (fate-1 ord-1 np-1 nil nil  aux-1 )
		  (new-decode-fate l-first-fate)
		(multiple-value-bind (fate-2 ord-2 np-2 nil nil aux-2 )
		    (new-decode-fate l-second-fate)
		  (let ((f1 (selectq fate-1 (:dead-with-eye :dead)(:alive-with-eye :alive)
				     ((:impossible :unknown) (incf unknown) fate-1)
				     (t fate-1)))
			(f2 (selectq fate-2 (:dead-with-eye :dead)(:alive-with-eye :alive)
				     (t fate-2))))
		    (incf total)
		    (when (and (eql f1 f2)
			       (neq f1 :impossible)
			       (neq f1 :unknown))
		      (when (eq ord-2 :pass)
			(incf not-killable))
		      (unless (or (eql ord-1 :pass)
				  (not (memq fate-1 '(:dead :dead-with-eye))))
			(when (selectq
				(ko-threat-p shape
					     :move :first
					     :x-pos x-pos
					     :y-pos y-pos
					     :introns introns
					     :property property
					     :outside-liberties outside-liberties)
				((t) (incf ko-threats) nil)
				(nil t)
				(t (cond ((eq force-pass nil) (incf ko-threats) nil)
					 ((eq force-pass :ask)
					  (format t "~&~%~\\picture\\ ~A (#o~o ~A,~A +~A) ~A moves at ~A"
						  (picture shape introns) shape introns 
						  x-pos y-pos outside-liberties
						  fate-1
						  ord-1 
						  )
					  (if (fquery nil
						      "Make it pass instead? ")
					      t
					      (incf ko-threats)
					      nil))
					 (t t)
					 )))
			    (format t "~&~%~\\picture\\ ~A (#o~o ~A,~A +~A) ~A moves at ~A"
				    (picture shape introns) shape introns 
				    x-pos y-pos outside-liberties
				    fate-1
				    ord-1 
				    )
			  (setq ord-1 :pass
				aux-1 nil)
			  ))
		      (unless (or (eql ord-2 :pass)
				  (not (memq fate-2 '(:alive :alive-with-eye))))
			(when (selectq
				(ko-threat-p shape
					     :x-pos x-pos
					     :y-pos y-pos
					     :introns introns
					     :property property
					     :move :second
					     :outside-liberties outside-liberties)
				((t) (incf ko-threats) nil)
				(nil t)
				(t (cond ((eq force-pass nil) (incf ko-threats) nil)
					 ((eq force-pass :ask)
					  (format t "~&~%~\\picture\\ ~A (#o~o ~A,~A +~A) ~A moves at ~A"
						  (picture shape introns) shape introns 
						  x-pos y-pos outside-liberties
						  fate-1
						  ord-1 
						  )
					  (if (fquery nil
						      "Make it pass instead? ")
					      t
					      (incf ko-threats)
					      nil))
					 (t t)
					 )))
			    (format t "~&~%~\\picture\\ ~A (#o~o ~A,~A +~A) ~A moves at ~A"
				    (picture shape introns) shape introns 
				    x-pos y-pos outside-liberties
				    fate-1
				    ord-1 
				    )
			  (setq ord-2 :pass
				aux-2 nil)
			  ))
		      (incf terminal)
		      (let ((new-fate-1
			      (merge-fates first-fate
					   (new-encode-fate
					     fate-1 ord-1 np-1
					     real-liberties real-liberties
					     aux-1 0)))
			    (new-fate-2
			      (merge-fates second-fate
					   (new-encode-fate
					     fate-2 ord-2 np-2
					     real-liberties real-liberties
					     aux-2 0))))
			(when (or (null new-fate-1)(null new-fate-2))
			  (signal-error ("merge-fate returned nil")))
			(set-fate-of-shape
			  shape x-pos y-pos :first
			  new-fate-1
			  property
			  introns)
			(set-fate-of-shape
			  shape x-pos y-pos :second
			  new-fate-2
			  property
			  introns)))
		    )))))
	  :shape shape
	  :set set
	  :x-pos x-pos
	  :y-pos y-pos
	  :property property
	  :introns introns
	  ))
    :shape shape
    :liberties 0
    )
  (format t "~&~A Total~&~A (~3f%) irrelevant~&~A (~3f%) terminal~&~A (~3f%) ko threats~
             ~&~A (~3f%)not killable"
	  total
	  unknown (/ unknown (float total))
	  terminal (/ terminal (float total))
	  ko-threats (/ ko-threats (float terminal))
	  not-killable (/ not-killable (float terminal))
	  )
  )
  )

(defun set-depth-for-shape (&key shape x-pos y-pos introns outside-liberties property move)
3  ;set the depth by reacursive descent to a 0, which should have been set
0  3;by SET-TERMINAL-POSITIONS
0  (if shape
      (let* ((fate (fate-of-shape shape x-pos y-pos move property introns))
	     (l-fate (fate-for-outside-liberties fate outside-liberties)))

	(multiple-value-bind (fate-1 ord-1 np-1 l-1 l-2 aux-1 pv-1)
	    (new-decode-fate l-fate)

	  (if (memq fate-1 '(:impossible :unknown))

	      0

	      (if (fixp pv-1)
		  pv-1
		  (multiple-value-bind (new-shape new-intr new-libs)
		      (if (eq move :first)
			  (successor-shape-moving-first
			    shape ord-1
			    :introns introns
			    :outside-liberties outside-liberties
			    :x-pos x-pos
			    :y-pos y-pos)
			  (successor-shape-moving-second
			    shape ord-1 introns outside-liberties))
		    (let ((depth (set-depth-for-shape
				   :shape new-shape
				   :x-pos x-pos
				   :y-pos y-pos
				   :introns new-intr
				   :outside-liberties new-libs
				   :property property
				   :move (if (eq move :first) :second :first))))
		      (when depth
			(let* ((new-fate (new-encode-fate fate-1 ord-1
							  np-1 l-1 l-2 aux-1 (1+ depth)))
			       (merged (merge-fates fate new-fate)))
 			  (set-fate-of-shape shape x-pos y-pos move merged property introns)
			  )
			(1+ depth)
			)))))))
      0))

(defun set-non-terminal-positions (&key shape)
  (let ((unknown 0)
	(total 0)
	(depths (make-array 25 :initial-value 0))
	)
  (map-shape-spec
    #'(lambda (&key shape set x-pos y-pos outside-liberties property introns)
	(ignore outside-liberties set)
	(loop for move in '(:first :second)
	      do
	  (let ((fa (fate-of-shape shape x-pos y-pos move property introns)))
	    (loop with first-f = (ldb new-libs-byte-spec (if (listp fa) (first fa) fa))
		  for f in (if (listp fa) fa (list fa))
		  as val = (set-depth-for-shape
			     :shape shape
			     :x-pos x-pos
			     :y-pos y-pos
			     :introns introns
			     :outside-liberties (- (ldb new-libs-byte-spec f) first-f)
			     :property property
			     :move move)
		  do
	      (if (null val)
		  (incf unknown)
		  (incf (aref depths val))
		  )))))
    :shape shape
    :liberties 0
    )
  `((:total ,total)
    (:unknown ,unknown)
    ,@(loop for i from 0 below 25
	    as v = (aref depths i)
	    when ( v 0)
	    collect `(,i ,v )
	      ))
  ))

(defun largest-drop-in-shape (game)
  3;;
  ;; seatch for the larrgest drop in shape for center, side, and corner
0  3;; This takes a while, and will never change, so we don't run it wevery time
0  3;; Go is loaded up!
  ;;
0  (let ((max-corner-size 0)
	max-corner-shape
	(max-side-size 0)
	max-side-shape
	(max-center-size 0)
	max-center-shape
	)
    (map-shape-spec 
      #'(lambda (&key shape set x-pos y-pos outside-liberties property introns )
	  (declare (sys:downward-function))
	  (ignore outside-liberties property introns set)
	  (multiple-value-bind (ignore ignore main-group cap)
	      (place-surrounded-in-position game shape x-pos y-pos 0 0)
	    (when cap (signal-error ("bogus drop in")))
	    (loop for gr in (active-groups (get-player-for-color game :white))
		  unless (eq gr main-group)
		    do
		      (let ((pos (board-position gr)))
			(selectq pos
			  (:corner
			   (when (> (size gr) max-corner-size)
			     (setq max-corner-size (size gr)
				   max-corner-shape shape)
			     (update)
			     (format t "~&~A: ~A ~A" pos shape max-corner-size))
			   )
			  (:side
			   (when (> (size gr) max-side-size)
			     (setq max-side-size (size gr)
				   max-side-shape shape)
			     (update)
			     (format t "~&~A: ~A ~A" pos shape max-side-size))
			   )
			  (:center
			   (when (> (size gr) max-center-size)
			     (setq max-center-size (size gr)
				   max-center-shape shape)
			     (update)
			     (format t "~&~A: ~A ~A" pos shape max-center-size))
			   )
			  (t (signal-error ("~A: unknown board position" pos)))))
		      )))
      :liberties 0
      :introns 0
      :shape (all-drop-ins zhash-shape-database))
    (values (list max-corner-size max-corner-shape)
	    (list max-side-size max-side-shape)
	    (list max-center-size max-center-shape))))


(defun largest-number-adjacent (&key shape (property *fate-property*) (position) (introns)
				(liberties 2)
				)
  3;;
0  3;; Find the largest number of adjacent groups.  The normal numbers
0  3;; are 1 and 2.  We don't run this every time because it will never change.
0  3;;
0  (let ((na (make-array 10 :initial-value 0)))
    (map-shape-spec
      #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	  (declare (sys:downward-function))
	  set property
	  (let* ((fate (fate-for-outside-liberties
			 (fate-of-shape shape x-pos y-pos :first *fate-property* introns)
			 outside-liberties)))
	    (multiple-value-bind (nil nil  n)
		(new-decode-fate fate)
	      (incf (aref na n)))))
      :shape shape :property property
      :introns introns
      :position position
      :liberties liberties
      :drop-ins t)
    (listarray na)
    )
  )


4;;
;; Experimantal stuff to explicitly generate move trees,independant of the
;; problem solver
;;
0(defvar all-outcomes (make-hash-table :locking nil :test #'equal :area *go-static-area*))

(defflavor outcome  (key
		     (predecessors)
		     (first-successor)
		     (second-successor)
		     (tag 0))
	   (property-list-mixin tv:essential-menu-methods-mixin)
  :readable-instance-variables
  (:initable-instance-variables key))

(defmethod (sys:print-self outcome ) (stream ignore ignore)
  (si:printing-random-object (self stream :no-pointer :typep)
    (multiple-value-bind (ff sf) (basic-outcomes self)
      (format stream "~A -> ~A~A~A" key
	      ff (if (eq sf ff) "" " or ") (if (eq sf ff) "" sf)))))

(defmethod (basic-outcomes outcome) ()
  (if (listp key)
      (destructuring-bind (&optional shape xp yp intr lib &rest ignore)
	  key
	(let* ((first-fate (fate-of-shape shape xp yp :first *fate-property* intr))
	       (second-fate (fate-of-shape shape xp yp :second *fate-property* intr)))
	  (multiple-value-bind (first-outcome first-move )
	      (new-decode-fate (or (fate-for-outside-liberties first-fate lib)
				   (last-fate-for-outside-liberties first-fate)
				   ))
	    (multiple-value-bind (second-outcome second-move)
		(new-decode-fate (or (fate-for-outside-liberties second-fate lib)
				     (last-fate-for-outside-liberties first-fate)))
	      (values first-outcome second-outcome first-move second-move)))))
      (values key key key key)))

(defmethod (stable-outcome-p outcome) ()
  (if (nlistp key) t
      (multiple-value-bind (first-fate second-fate)
	  (basic-outcomes self)
	(when (eq first-fate :alive-with-eye) (setq first-fate :alive))
	(when (eq second-fate :dead-with-eye) (setq second-fate :dead))
	(eq first-fate second-fate)
	)))

(defmethod (successors outcome) ()
  (if (and first-successor second-successor)
      (values first-successor second-successor)
      (generate-successors self)))

(defmethod (predecessors outcome) ()
  predecessors)

(defmethod (add-predecessor outcome) (pred)
  (unless (memq pred predecessors)
    (push pred predecessors)))

(defmethod (add-first-successor outcome) (succ)
  (setq first-successor succ))

(defmethod (add-second-successor outcome) (succ)
  (setq second-successor succ))

(defmethod (generate-successors outcome) ()
  (when (listp key)
    (multiple-value-bind (first-outcome second-outcome first-move second-move)
	(basic-outcomes self)

      (labels ((add-new (new-key first-or-second)
		 (let ((old-out (gethash new-key all-outcomes)))
		   (unless old-out
		     (setq old-out (make-instance
				     (typep self)
				     :area *go-cons-area*
				     :full-board-size (full-board-size self)
				     :key new-key))
		     (re-init old-out)
		     (puthash new-key old-out all-outcomes))
		   (add-predecessor old-out self)
		   (selectq first-or-second
		     (:first (add-first-successor self old-out))
		     (:second (add-second-successor self old-out))
		     (t (signal-error ("first-or-second must be :first or :second")))
		     ))))

	(destructuring-bind (&optional shape xp yp intr libs&rest ignore) key
	  (cond ((fixp first-move)
		 (multiple-value-bind (new-shape new-introns new-libs)
		     (successor-shape-moving-first shape first-move
						   :introns intr
						   :outside-liberties libs
						   :x-pos xp
						   :y-pos yp)
		   (if (null new-shape)
		       (add-new (if (= (size shape) 1) :dead :alive) :first)
		       (add-new (list new-shape xp yp new-introns new-libs) :first))
		   ))
		((eq first-move :outside)
		 (signal-error ("first move shouldn't be :OUTSIDE")))
		((eq first-move :pass)
		 (add-new :pass :first))
		(t (signal-error ("unknown first move ~A -> ~A" first-outcome first-move))))

	  (cond ((fixp second-move)
		 (multiple-value-bind (new-shape new-introns new-libs)
		     (successor-shape-moving-second shape second-move intr libs)

		   (let* ((new-fate (fate-of-shape
				      shape xp yp :first *fate-property* new-introns))
			  (new-libs-fate (fate-for-outside-liberties
					   new-fate new-libs))
			  (decoded-new-fate (new-decode-fate new-libs-fate)))
		     (if (eq decoded-new-fate :impossible)
			 (add-new :dead :second)
			 (add-new (list new-shape xp yp new-introns new-libs) :second))
		     )))
		((eq second-move :outside)
		 (if (> libs 0)
		     (add-new (list shape xp yp intr (1- libs)) :second)
		     (add-new :pass :second)))
		((eq second-move :pass)
		 (add-new :pass :second))
		(t (signal-error ("unknown first move ~A" second-outcome second-move))))
	  )
	)
      (values first-successor second-successor)
      )))

(defmethod (generate-tree outcome) ()
  (if (listp key)
      (multiple-value-bind (first second) (generate-successors self)
	(list (generate-tree first)
	      (generate-tree second)))
      self))

(defmethod (make-outcome shape-info) (xp yp intr outside)
  (let* ((key (list self xp yp intr outside))
	 (old (gethash key all-outcomes)))
    (unless old
      (setq old (make-instance 'outcome :area *go-cons-area* :key key))
      (puthash key old all-outcomes))
    old))


3;;
;; Experiment to look for better compression through increased sharing.
;;
0(defvar *miss* 0)
(defvar *hit* 0)
(defun hash-1-fate (item)
  (loop with ar = (cdr item)
	with len = (array-length ar)
	for i from 0 below len
	as val = (aref ar i)
	when (listp i) do (setf (aref ar i) (lexpr-funcall #'hash-list val))
	finally
	  (let ((sa  (intern-small-array ar)))
	    (if (eq sa ar) (incf *miss*) (incf *hit*))
	    (return (hash-cons (car item) sa)))))

(defun hash-fate (lst)
  (loop with val
	for i in (reverse lst)
	do (setq val (hash-cons (hash-1-fate i) val))
	finally (return val)))

(defun hash-all-fates (&optional (prop *fate-property*))
  (loop for s in (all-shapes-and-drop-ins zhash-shape-database)
	do (put-prop s (hash-fate (get-prop s prop)) prop)))


(defun shape-move-abnormalities (&key (all t) repair
				   shape property position introns
				   (progress-note "move abnormalities"))
  
  (map-shape-spec
    #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	(declare (sys:downward-function))
	set
	(loop with retry = nil
	      as f1 = (fate-of-shape shape x-pos y-pos :first property introns)
	      as f2 = (fate-of-shape shape x-pos y-pos :second property introns)
	      as f1l = (fate-for-outside-liberties f1 outside-liberties)
	      as f2l = (fate-for-outside-liberties f2 outside-liberties)
	      do
	  (multiple-value-bind (fate1 ) (new-decode-fate f1l)
		(multiple-value-bind (fate2) (new-decode-fate f2l)
		  (labels ((report-error (msg)
			     (format t "~&~\\picture\\ ~A (#o~O) ~A,~A~%~
			              ~A  ~a ~a  -> ~a~&"
				     (picture shape introns) shape introns x-pos y-pos
				     msg property  fate2 fate1
				     )
			     (unless all (throw 'exit-shape t))))

		    (when (and (eq fate1 :dead-with-eye)
			       (eq fate2 :dead))
		      (with-restart-options
			(((error "Continue") (setq retry nil))
			 ((error "Coerce it")
			  (multiple-value-bind (nil nil np l1 l2 aux pv)
			      (new-decode-fate f1)
			    (let ((new (merge-fates f2 (new-encode-fate
							 :dead-with-eye
							 :outside
							 np l1 l2 aux pv))))
			      (set-fate-of-shape shape
						 x-pos y-pos :second new property introns
						 t)
			      (setq fate2 fate1)
			      )
			    )))
			(report-error "Dead-with-eye becomes Dead moving second")
			(signal-error ("what now?"))
			))

		    (when (and (not (memq fate2 '(:unknown :impossible)))
			       (not (memq fate1 '(:unknown :impossible))))

		      (if (or (eql fate1 fate2)
			      (>= (outcome-to-score fate1)
				  (outcome-to-score fate2))
			      )

			  (when retry
			    (format t "~&repair succeeded - fate is now ~A~%" fate2)
			    (setq retry nil))
			  
			  (report-error "Score got worse moving first")			  

			  (when repair
			    (if retry
				(progn (setq retry nil)
				       (break "Repair failed"))
				(calculate-one-fate shape
						    :property property
						    :forced t
						    :x-pos x-pos
						    :y-pos y-pos
						    :outside-liberties outside-liberties
						    :introns introns)
				(setq retry t)))
			  )
		      ))))
		while retry)
	      )
    :shape shape :property property :introns introns :position position
    :progress-note progress-note
    ))

(defun shape-outcome-abnormalities (&key (all t) repair
				   shape property position introns
				   (progress-note "outcome abnormalities"))
  (ignore repair)
  (map-shape-spec
    #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	(declare (sys:downward-function))
	set
	(loop with retry = nil
	      as f1 = (fate-of-shape shape x-pos y-pos :first property introns)
	      as f1l = (fate-for-outside-liberties f1 outside-liberties)
	      as fate1 = (new-decode-fate f1l)
	      when (memq fate1 '(:unknown :impossible nil)) return nil
	      as score1 = (outcome-to-score fate1)
	      as test-bit = 1 then (lsh test-bit 1)
	      as real-intron = (bit-test test-bit introns)
	      as test-intron = (cond (real-intron (logxor test-bit introns))
				     ((> test-bit introns) introns)
				     (t nil))
	      as test-libs = (if real-intron outside-liberties (1+ outside-liberties))
	      as f2 = (when test-intron
			(fate-of-shape shape x-pos y-pos :second property test-intron))
	      as f2l = (when f2
			 (fate-for-outside-liberties f2 test-libs))
	      as fate2 = (when f2l (new-decode-fate f2l))
	      when fate2
	      do
		(labels ((report-error ()
			   (format t "~&~A,~A ~S~
					~&~\\picture\\ ~A (#o~O) ~d libs ~S moving second~
					~&~%~\\picture\\ ~A (#o~O) ~d libs ~S moving first"
				   x-pos y-pos property
				   (picture shape test-intron) shape test-intron test-libs fate2 
				   (picture shape introns) shape introns outside-liberties fate1
				   )
			   (unless all (throw 'exit-shape t))))

		  (when (> (outcome-to-score fate2) score1)
		      (with-restart-options
			(((error "Continue") (setq retry nil))
			 ((error "Coerce moving second to agree with moving first")
			  (multiple-value-bind (nil nil np l1 nil aux pv)
			      (new-decode-fate f2l)
			    (let ((new (merge-fates f2 (new-encode-fate
							 fate1
							 (if (> test-bit introns)
							     :outside
							     (haulong test-bit)
							     )
							 np l1 l1 aux pv))))
			      (set-fate-of-shape shape x-pos y-pos :second new property test-intron
						 t)
			      (setq fate2 fate1)
			      )
			    ))

			 ((error "Coerce moving first to agree with moving second")
			  (multiple-value-bind (nil nil np l2 nil aux pv)
			      (new-decode-fate f1l)
			    (let ((new (merge-fates f1 (new-encode-fate
							 fate2
							 (if (> test-bit introns)
							     :outside
							     (haulong test-bit)
							     )
							 np l2 l2 aux pv))))
			      (set-fate-of-shape shape x-pos y-pos :first new property introns
						 t)
			      (setq fate2 fate1)
			      )
			    )))
			(report-error)
			(signal-error ("what now?"))
			))
	      )
		until (> test-bit introns)
		))
    :shape shape :property property :introns introns :position position
    :progress-note progress-note
    ))

(defun shape-fate-abnormalities (&key (all t) repair
				 (abnormal-fates '(:repetition :indeterminate))
				 shape property position introns
				 (progress-note "fate abnormalities"))
  
  (map-shape-spec
    #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	(declare (sys:downward-function))
	(ignore outside-liberties)
	set
	(loop for move in '(:first :second)
	      do
	  (loop named list-loop
		for n from 0
	      do
	  (loop with retry = nil and change = nil
		as fate = (fate-of-shape shape x-pos y-pos move property introns)
		when (null fate) do (return-from list-loop nil)
		as fate-el = (if (listp fate)
				 (nth n fate)
				 (if (= n 0) fate nil))
		as base-liberties = (ldb new-libs-byte-spec
					 (if (listp fate)
					     (first fate)
					     fate))
		when (null fate-el) do (return-from list-loop nil)
		do
	    (multiple-value-bind (fate1 ord1 np1 l1 l2 aux1 pv1 )
		(new-decode-fate fate-el)
	      (labels ((report-error (msg)
			 (format t "~&~\\picture\\ ~A (#o~O +~D-~D) ~A,~A ~A~%~
			              ~A  ~a ~a@~a~&"
				 (picture shape introns) shape introns
				 (- l1 base-liberties) (- l2 base-liberties)
				 x-pos y-pos move
				 msg property fate1 ord1
				 )
			 (when repair (signal-error ("~A" (cf fate))))
			 (unless all (throw 'exit-shape t))))

		(with-restart-options
		  (((error "Continue") (setq retry nil))
		   ((error "Re evaluate") (setq retry t))
		   )
		  (setq retry nil)
		  (cond ((memq fate1 '(:unknown :impossible)))
			((memq fate1 abnormal-fates)
			 (report-error "Abnormal fate")
			 )
			((memq fate1 '(:dead-with-eye :dead))
			 (when (and (eq ord1 :pass) (eq move :second))
			   (report-error "Passing when we should be killing")
			   ))
			((memq fate1 '(:alive :alive-with-eye))
			 (when (eq ord1 :outside)
			   (with-restart-options
			     (((error "Make it pass")
			       (setq ord1 :pass change t)))
			     (if (eq move :first)
				 
				 (progn
				   ;(report-error "Outside-moves can't help make us alive")
				   (setq ord1 :pass change t))
				   

				 (unless (ko-threat-p shape
						      :x-pos x-pos
						      :y-pos y-pos
						      :introns introns
						      :outside-liberties
						      (- l1 base-liberties)
						      :property property
						      :move :second)
				   (setq ord1 :pass change t)
				   ;(report-error "Moving outside won't help us kill")
				   )))))
			))
		(when change
		  (let* ((new-fate (new-encode-fate fate1 ord1 np1 l1 l2 aux1 pv1))
			 (merged (merge-fates fate new-fate)))
		    ;(dbg:when-mode-lock (dbg))
		    (set-fate-of-shape shape x-pos y-pos move merged property introns t)
		    (setq fate merged)))

		(when (and repair retry)

		  (calculate-one-fate shape
				      :property property
				      :forced t
				      :x-pos x-pos
				      :y-pos y-pos
				      :outside-liberties
				      (- l1 base-liberties)
				      :introns introns))
		))
		while retry))))
    :shape shape :property property :introns introns :position position
    :progress-note progress-note
    ))

(defun shape-position-abnormalities (&key ;repair
				     (property *fate-property*)
				     (progress-note "position abnormalities")
				     shape)
  (let ((bad-shapes))
    (map-shapes-and-drop-ins
      #'(lambda (&key shape position set)
	  (declare (sys:downward-function))
	  (ignore set)
	  (loop with used = 0 
		and prop = (get-prop shape property)
		and all-prop = 0
		for move in '(:first :second) do
	    (loop for (xp yp) in position
		  as nfound = 0
		  as pos = (encode-move-geometry move xp yp)
		  do (loop for (move-pos . val) in prop
			   when (and (bit-test pos move-pos) val)
			     do (incf nfound))
		     (setq used (logior used pos))
		     (selector nfound eql
		       (1)
		       (0 
			 (unless (assoc shape bad-shapes) (push (list shape move xp yp) bad-shapes))
			 (format t "~&Missing fate for ~A moving ~A @ ~A,~A"
				 shape move xp yp))
		       (t (signal-error ("Multiple fate for ~A moving ~A @ ~A,~A"
				  shape move xp yp))))
		     )
		finally (loop for (move-pos . val) in prop
			      do (setq all-prop (logior all-prop move-pos)))
			(setq all-prop (logand all-prop (lognot used)))
			(when ( all-prop 0)
			  (with-restart-options
			    (((error "delete it")
			      (loop for spec in prop
				    when (bit-test (first spec) all-prop)
				      do (setf (first spec) (logand (first spec) (lognot all-prop))))
			      (setq prop (remove-if #'(lambda (a)(zerop (first a))) prop))
			      (put-prop shape prop property)
			      )
			     ((error "continue")))

			    (signal-error ("Shape ~A has extra info for positions ~A"
				    shape
				    (decode-move-geometries all-prop nil)))))
			))
      shape *all-shape-positions* t progress-note
      )
    bad-shapes))

(defun verify-na (fate should-be-np)
  (cond ((listp fate)
	 (let ((new (verify-na (first fate) should-be-np))
	       (rest (verify-na (cdr fate) should-be-np)))
	   (if (and (eql new (first fate))
		    (eq rest (cdr fate)))
	       fate
	       (cons new rest ))))
	((null fate) nil)
	(t
	 (multiple-value-bind (f ord np l1 l2 aux pv )
	     (new-decode-fate fate)
	   (cond ((memq f '(nil :unknown :impossible)) fate)
		 ((eql np should-be-np) fate)
		 (t (new-encode-fate f ord should-be-np l1 l2 aux pv)))))))

(defun collect-nas (fate)
  (if (listp fate)
      (cons
	(collect-nas (first fate))
	(collect-nas (cdr fate))
	)
      (multiple-value-bind (nil nil f) (new-decode-fate fate)
	f)))

(defvar *shape-na-data* nil)

(defun restore-shape-na-data (forms)
  (unless *shape-na-data*
    (setq *shape-na-data* (make-hash-table :locking nil :test #'equal :size (length forms))))
  (loop for (data pic intr . spec) in forms
	as shape = (or (find-shape pic) (signal-error ("shape ~A not found" pic)))
	as intron = (find-drop-in-number shape intr)
	do (puthash (cons intron spec ) data *shape-na-data*)))

(defun load-shape-na-data ()
  (load "go:go;shape-na-data.bin")
  )

(defun dump-shape-na-data ()
  (let ((forms))
    (maphash #'(lambda (key dat)
		 (let* ((sh  (first key))
			(in (drop-in-number sh)))
		   (push `(,dat ,(picture (parent-shape sh) in) ,in
			   ,@(cdr key)) forms)))
	     *shape-na-data*)
    (si:dump-forms-to-file "go:go;shape-na-data.bin" `((restore-shape-na-data ',forms)))))

(defun shape-na-abnormalities (&key repair (property *fate-property*) shape
			       (progress-note "na abnomalities"))
  (ignore repair)
  (unless *shape-na-data*
    (load-shape-na-data))
  (let ((bad-shapes)
	new-data)
    (map-shapes-and-drop-ins
      #'(lambda (&key shape position set)
	  (declare (sys:downward-function))
	  (ignore set)
	  (map-positions
	    #'(lambda (&key x-pos y-pos)
		(let ((true-na (or (with-stack-list (key shape x-pos y-pos)
				     (gethash key *shape-na-data*))
				   (multiple-value-bind (nil nil some)
				       (place-surrounded-in-position game shape x-pos y-pos )
				     (loop with adj
					   for gr in (adjacent-territory-groups some)
					   do (loop for g in (adjacent-territory-groups gr)
						    unless (memq g adj)
						      do (push g adj))
					   finally
					     (puthash (list shape x-pos y-pos)
						      (length adj)
						      *shape-na-data*)
					     (setq new-data t)
					     (return (length adj))
					     )))))
		  (loop with prop = (get-prop shape property)
			for move in '(:first :second) 
			as pos = (encode-move-geometry move x-pos y-pos)
			as item = (loop for i in prop 
					as (move-pos . val) = i
					when (and (bit-test pos move-pos) val)
					  return i)
			as (positions . results) = item
			when item
			  do
			    (loop for idx from 0 below (array-length results)
				  with continue
				  as val = (aref results idx)
				  as new-val = (verify-na val true-na)
				  unless (eql val new-val)
				    do
				      (labels ((repair-1 ()
						 (let ((new (make-array (array-length results))))
						   (copy-array-contents results new)
						   (setq results new))
						 (cond ((not (eql (first item) pos))
							(setf (first item) (logxor pos (first item)))
							(setq item (cons pos results ))
							(setq prop (cons item prop ))
							(put-prop shape prop property)
							)
						       (t (setf (cdr item) results)))
						 (setf (aref results idx) new-val)
						 (setq continue t)))
				      (with-restart-options
					(((error "Split it and continue fixing")
					  (repair-1))
					 ((error "Skip the rest")
					  (loop-finish))
					 )
					(if continue
					    (setf (aref results idx) new-val)
					    (if repair
						(progn 
						  (format t "Shape ~A moving ~A @ ~A,~A #~D~&  is: ~D sb ~D"
							       shape move x-pos y-pos idx
							       (collect-nas val)
							       (collect-nas new-val))
						  (repair-1))
						(update)
						(signal-error ("Shape ~A moving ~A @ ~A,~A #~D~&  is: ~D sb ~D"
							       shape move x-pos y-pos idx
							       (collect-nas val)
							       (collect-nas new-val)))))
					)))
			    )))
	    position))
      shape *all-shape-positions* t progress-note
      )
    (when new-data
      (dump-shape-na-data))
    bad-shapes))


(defun shape-dead-with-eye-abnormalities
       (&key repair (property *fate-property*) count shape (progress-note "dead-with-eye abnormalities"))
  ;look for places where "dead" probably should be "dead with eye"
  (let ((grand-total 0))
    (map-shapes-and-drop-ins
      #'(lambda (&key shape position set)
	  (declare (sys:downward-function))
	  (ignore set position)
	  (loop named shape
		with coerce-shape = repair
		with prop = (get-prop shape property)
		with total = 0
		for (positions . results) in prop
		do
	    (loop named position
		  with coerce-position
		  as some = nil
		  for idx from 0 below (array-length results)
		  as minl1 = 999
		  as val = (aref results idx)
		  as lval = (cond ((listp val) val)
				  ((null val) val)
				  (t (list val)))
		  do
	      (loop for enc-val in lval
		    as coerce-one = nil
		    do 
		(multiple-value-bind (f ord np l1 l2 aux pv )
		    (new-decode-fate enc-val)
		  (ignore pv aux ord)

		  (when (and f (neq f :unknown))
		    (setq minl1 (min l1 minl1))
		    (when (neq f :impossible)
		      (when ( np 1)
			(if some
			    (with-restart-options (((error "continue")))
						  (ferror "some already"))
			    (return-from position nil))
			(setq some t))
		      (setq minl1 (min l1 minl1)))

		  (when (eq f :dead)
		    (if count (incf total)
			(multiple-value-bind (moving xpos ypos)
			    (decode-move-geometry positions)
			  (unless (or coerce-position coerce-shape)
			    (place-surrounded-in-position game shape xpos ypos (- l1 minl1) idx)
			    (update)
			    (format t "~&Shape ~\\shape\\ in ~S,~S moving ~S  introns ~D libs ~d to ~d~%~A "
				    shape xpos ypos moving idx (- l1 minl1)(- l2 minl1)
				    (cf lval)))
			  (with-restart-options
			    (((error "Continue"))
			     ((error "Skip the rest of this shape")
			      (return-from shape nil))
			     ((error "skip the rest of this position")
			      (return-from position nil))
			     ((error "coerce the rest of this position")
			      (setq coerce-position t))
			     ((error "coerce the rest of this shape")
			      (setq coerce-shape t))
			     ((error "Coerce it to DEAD-WITH-EYE")
			      (setq coerce-one t)))
			    (unless (or coerce-position coerce-shape)
			      (ferror "candidate to change fate")))
			  (when (or coerce-position coerce-shape coerce-one)
			    (loop for libs from (- l1 minl1) to (- l2 minl1) do
			      (unless repair (format t "."))
			      (incf total)
			      (modify-fate :moving moving
					   :shape shape
					   :introns idx
					   :liberties libs
					   :position (list (list xpos ypos))
					   :print-message (not (or coerce-shape
								   coerce-position))
					   :fate :dead-with-eye)))
			  )))))))
	    finally
	    (when (> total 0)
	      (format t "~&~S = ~D" shape total)
	      (incf grand-total total)))

	  )
      shape
      :c
      t progress-note)
    grand-total))

(defun shape-dead-outside-abnormalities
       (&key repair
	(property *fate-property*)
	count
	(progress-note "dead outside abnormalities")
	shape)

  ;look for places where "dead" probably should be "dead with eye"
  (let ((grand-total 0)
	(grand-questionable 0))
    (map-shapes-and-drop-ins
      #'(lambda (&key shape position set)
	  (declare (sys:downward-function))
	  (ignore set position)
	  (loop named shape
		with coerce-shape = repair
		with prop = (get-prop shape property)
		as total = 0
		as questionable = 0
		for (positions . results) in prop
		do
	    (multiple-value-bind (moving xpos ypos ) (decode-move-geometry positions)
	      (when (eq moving :second) 
	    (loop named position
		  with coerce-position
		  as someord = nil and somefate = nil
		  for idx from 0 below (array-length results)
		  as minl1 = 999
		  as val = (aref results idx)
		  as lval = (cond ((listp val) val)
				  ((null val) val)
				  (t (list val)))
		  do
	      (loop named libs
		    for enc-val in lval
		    as coerce-one = (and (not count) (or coerce-shape coerce-position))
		    do 
		(multiple-value-bind (f ord np l1 l2 aux pv )
		    (new-decode-fate enc-val)
		  (ignore pv aux np)

		  (when (memq f '(:dead :dead-with-eye))

		    (setq minl1 (min l1 minl1))
		    (when (fixp ord)
		      (setq someord ord
			    somefate f))

		    (when (and someord (not (fixp ord)))

		      (let* ((nf (fate-of-shape shape xpos ypos :first
						property (logior idx (lsh 1 (1- someord)))))
			     (new-fate (new-decode-fate (fate-for-outside-liberties nf (- l1 minl1 1)))))
			(when (and (neq new-fate :impossible)
				   (neq new-fate :unknown))

			  (let ((idx-test (bit-test idx (lsh 1 (1- someord))))
				(fate-test (not (or (eq f new-fate)
						    (eq new-fate :dead-with-eye)))))
				
			    (when (and (not count)
				       (or (not coerce-one)
					   (and (not repair)
						idx-test
						fate-test)))
			      (place-surrounded-in-position game shape xpos ypos (- l1 minl1) idx)
			      (update)
			      (format t "~&Shape ~\\shape\\ in ~S,~S moving ~S  introns ~D libs ~d to ~d some = ~A this = ~A~%~A "
				      shape xpos ypos moving idx (- l1 minl1)(- l2 minl1)
				      someord ord
				      (cf lval)))

			    (with-restart-options
			      (((error "Continue"))
			       ((error "Skip this one")
				(setq coerce-one nil))
			       ((error "Skip the rest of this shape")
				(return-from shape nil))
			       ((error "skip the rest of this position")
				(return-from position nil))
			       ((error "coerce the rest of this position")
				(setq coerce-position t
				      coerce-one t))
			       ((error "coerce the rest of this shape")
				(setq coerce-shape t
				      coerce-one t))
			       ((error "Coerce it to ~S ~D" new-fate someord)
				(setq coerce-one t))
			       ((error "Coerce it to ~S ~D" f someord)
				(setq new-fate f
				      coerce-one t))
			       )
			      (if (or idx-test fate-test)
				  (progn (incf questionable)
					 (if (or repair count)
					     (setq coerce-one nil)
					     (ferror "bad successor because ~{~A ~}"
						     (cond (idx-test (list "introns" idx (lsh 1 (1- someord))))
							   (fate-test (list "new fate" f new-fate))
							   ))))
				  (incf total)
				  (unless (or coerce-one count)
				    (ferror "candidate to change move"))))

			      (when coerce-one
				(loop for libs from (- l1 minl1) to (- l2 minl1) do
				  (unless repair (format t "."))
				  (modify-fate :moving moving
					       :shape shape
					       :introns idx
					       :liberties libs
					       :position (list (list xpos ypos))
					       :move-at someord
					       :fate new-fate
					       :print-message (not (or coerce-shape
								       coerce-position))
					       ))))
			  ))))))
	    finally
	    (when (> (+ questionable total) 0)
	      (format t "~&~S ~S,~S = ~D + ~D" shape xpos ypos total questionable)
	      (incf grand-questionable  questionable )
	      (incf grand-total total)))

	  ))))
      shape
      :c t progress-note)
    (format t "~&Grand total ~D   + questionable ~D" grand-total grand-questionable)))

(defun shape-outside-abnormalities
       (&key repair
	(property *fate-property*)
	count
	(progress-note "outside abnormalities")
	shape)

  ;look for places where first move is "outside" but can't be.

  (let ((grand-total 0)
	(grand-questionable 0))
    (map-shapes-and-drop-ins
      #'(lambda (&key shape position set)
	  (declare (sys:downward-function))
	  (ignore set position)
	  (loop named shape
		with coerce-shape = repair
		with prop = (get-prop shape property)
		as total = 0
		as questionable = 0
		for (positions . results) in prop
		do
	    (multiple-value-bind (moving xpos ypos ) (decode-move-geometry positions)
	      (when (eq moving :second) 
	    (loop named position
		  with coerce-position with retry 
		  as someord = nil and somefate = nil
		  for idx from 0 below (array-length results)
		  as minl1 = 999
		  as val = (aref results idx)
		  as lval = (cond ((listp val) (first val))
				  ((null val) val)
				  (t val))
		  as coerce-one = (and (not count) (or coerce-shape coerce-position))
		  do
	      (multiple-value-bind (f ord ) (new-decode-fate lval)
		(unless (or (eq f :unknown)(eq f :impossible))
		  (when (and (not (fixp ord))
			     (not (and (eq ord :pass)(memq f '(:alive :alive-with-eye :seki)))))

		    (when (and (not count)
			       (or (not coerce-one)
				   (not repair)))
		      (place-surrounded-in-position game shape xpos ypos 0 idx)
		      (update)
		      (format t "~&Shape ~\\shape\\ in ~S,~S moving ~S  introns ~d  ord = ~A~%~A "
			      shape xpos ypos moving idx 
				      ord
				      (cf val)))

		    (with-restart-options
		      (((error "Continue"))
		       ((error "Skip this one")
			(setq coerce-one nil))
		       ((error "Skip the rest of this shape")
			(return-from shape nil))
		       ((error "skip the rest of this position")
			(return-from position nil))
		       ((error "re-evaluate the rest of this position")
			(setq coerce-position t
			      coerce-one t))
		       ((error "re-evaluate the rest of this shape")
			(setq coerce-shape t
			      coerce-one t))
		       ((error "re-evaluate this one")
			(setq coerce-one t))
		       )
		      (incf total)
		      (when (or retry (not (or coerce-one count)))
			(ferror "candidate to change move")))

		    (when coerce-one
		      (modify-fate :moving :second :shape shape
				   :position `((,xpos ,ypos))
				   :introns idx
				   :print-message (not (or coerce-shape
							   coerce-position))
				   :fate :unknown
				   :liberties 0))
		      )))
	    finally
	    (when (> (+ questionable total) 0)
	      (format t "~&~S ~S,~S = ~D + ~D" shape xpos ypos total questionable)
	      (incf grand-questionable  questionable )
	      (incf grand-total total))))))

	  )
      shape
      :c t progress-note)
    (format t "~&Grand total ~D   + questionable ~D" grand-total grand-questionable)))

(defun shape-unknown-abnormalities
       (&key repair
	(property *fate-property*)
	count
	(progress-note "unknown abnormalities")
	shape)

  ;look for places where fate is unknown, and figure them

  (let ((grand-total 0)
	(grand-questionable 0))
    (map-shapes-and-drop-ins
      #'(lambda (&key shape position set)
	  (declare (sys:downward-function))
	  (ignore set position)
	  (loop named shape
		with coerce-shape = repair
		with prop = (get-prop shape property)
		as total = 0
		as questionable = 0
		for (positions . results) in prop
		do
	    (multiple-value-bind (moving xpos ypos ) (decode-move-geometry positions)

	    (loop named position
		  with coerce-position with retry 
		  as someord = nil and somefate = nil
		  for idx from 0 below (array-length results)
		  as minl1 = 999
		  as val = (aref results idx)
		  as lval = (cond ((listp val) val)
				  ((null val) val)
				  (t (list val)))
		  as coerce-one = (and (not count) (or coerce-shape coerce-position))
		  do
	      (loop for this-val in lval do
	      (multiple-value-bind (f ord ignore l1 l2) (new-decode-fate this-val)
		(unless (eq f :impossible)
		  (setq minl1 (min l1 minl1))
		  (when (eq f :unknown)

		    (when (and (not count)
			       (or (not coerce-one)
				   (not repair)))
		      (place-surrounded-in-position game shape xpos ypos 0 idx)
		      (update)
		      (format t "~&Shape ~\\shape\\ in ~S,~S moving ~S  introns ~d  ord = ~A~%~A "
			      shape xpos ypos moving idx 
			      ord
			      (cf val)))

		    (with-restart-options
		      (((error "Continue"))
		       ((error "Skip this one")
			(setq coerce-one nil))
		       ((error "Skip the rest of this shape")
			(return-from shape nil))
		       ((error "skip the rest of this position")
			(return-from position nil))
		       ((error "re-evaluate the rest of this position")
			(setq coerce-position t
			      coerce-one t))
		       ((error "re-evaluate the rest of this shape")
			(setq coerce-shape t
			      coerce-one t))
		       ((error "re-evaluate this one")
			(setq coerce-one t))
		       )
		      (incf total)
		      (when (or retry (not (or coerce-one count)))
			(ferror "candidate to change move")))

		    (if coerce-one
			(loop for libs from (- l1 minl1) to (- l2 minl1)
			      do
			  (calculate-one-fate shape
					      :property :fate
					      :x-pos xpos
					      :y-pos ypos
					      :moving (list moving)
					      :outside-liberties libs
					      :introns idx
					      :forced t)))

		      ))))
	    finally
	    (when (> (+ questionable total) 0)
	      (format t "~&~S ~S,~S = ~D + ~D" shape xpos ypos total questionable)
	      (incf grand-questionable  questionable )
	      (incf grand-total total)))

	  )))
      shape
      :c t
      progress-note)
    (format t "Grand total ~D   + questionable ~D" grand-total grand-questionable)))

(defun shape-duplicate-abnormalities (&key repair
				      (progress-note "duplicate abnormalities")
				      shape property position introns)
  introns
  (map-shapes-and-drop-ins 
    #'(lambda (&key shape position set
	       &aux (deleted 0) (siz (expt 2 (intron-array-size shape))))
	(declare (sys:downward-function))
	position set
	(map-props
	  #'(lambda (&key property)
	      (declare (sys:downward-function))
	      (loop with propval = (get-prop shape property)
		    and settled = 0
		    for val on propval
		    as ((key . arr)) = val
		    do
		(loop until (zerop key)
		      as bit = (logand key (- key))
		      do (setf key (logxor key bit))
		      unless (bit-test settled bit)
			do
		  ;we found a new bit, look for duplicates
			  (loop for newval on (cdr val)
				as ((newkey . newarr)) = newval
				when (bit-test bit newkey)
				  do
				    (multiple-value-bind (move xpos ypos)
					(decode-move-geometry bit)
						;found a duplicate, see if it is identical
				      (format t "~&Found duplicate for ~A (#o~O) ~A,~A ~A ~A~%"
					      shape bit xpos ypos property move)
				      (loop for i from 0 below siz
					    as errors from 0
					    as fate1 = (aref arr i)
					    as fate2 = (aref newarr i)
					    unless (eql fate1 fate2)
					      do
						(format t "~&~%~\\picture\\ (#o~o) fate is ~A -> ~A~&"
							(picture shape i) i
							(multiple-value-list (new-decode-fate fate1))
							(multiple-value-list (new-decode-fate fate2)))
						(incf errors)
					    finally 
					      (when (and (> errors 0) repair)
						(loop as option = (fquery '(:type :readline
									    :choices
									    ((#\F  "First")
									     (#\S "Second")
									     (#\N "Neither")
									     (#\B  "Break")))
									  "discard? ")
						      do
						  (selector option char-equal
						    (#\F (setf (first (first val))
							       (logxor (first (first val)) bit))
						     (loop-finish))
						    (#\S (setf (first (first newval))
							       (logxor newkey bit))
						     (loop-finish))
						    (#\N (loop-finish))
						    (t (signal-error ("Look at it")))))))
				      ))

			  ;look for NIL's in the array
			  (loop with len = (array-length arr)
				and n-nils = 0
				for i from 0 below len
				as v = (aref arr i)
				when (null v) do (incf n-nils)
				finally
				  (unless (zerop n-nils)
				    (multiple-value-bind (move xpos ypos)
					(decode-move-geometry bit)
						;found a duplicate, see if it is identical
				      (format t "~&Found ~D NILs for ~A ~A,~A ~A ~A~%"
					      n-nils shape xpos ypos property move)
				      )
				    ))
			  (setq settled (logior settled bit))
			  )
		(when (zerop (first (first val)))
		  (setq propval (delete (first val) propval))
		  (incf deleted))
		finally
		  (put-prop shape propval property)))
	  property)
	(unless (zerop deleted)
	  (format t "~%~D deleted for ~A" deleted shape))
	)
    shape position t progress-note
    ))

(defun check-identical-pos (&key shape property (moves '(:first :second)) quiet
			    (difference-matters '(t t t t nil nil nil)))
  (map-shape-spec
    #'(lambda (&key shape property outside-liberties x-pos y-pos introns set)
	(declare (sys:downward-function))
	set outside-liberties
	(loop for move in moves do
	  (multiple-value-bind (fate fa)
	      (fate-of-shape shape x-pos y-pos move property introns)
	    (multiple-value-bind (center-fate center-fa)
		(fate-of-shape shape :center :center move property introns)

	      (multiple-value-bind (nil nil np1 nl1)
		  (new-decode-fate (if (listp fate) (first fate) fate))
		(multiple-value-bind(nil nil npc nlc)
		    (new-decode-fate (if (listp center-fate)
					 (first center-fate)
					 center-fate))
		  (when (and (eql np1 npc)
			     (eql nl1 nlc))
		    (loop with bad = 0 and non-trivial = 0
			  for i from 0 below (array-length fa)
			  as a1 = (aref fa i)
			  as a2 = (aref center-fa i)
			  unless (equal a1 a2)
			    do
			      (incf bad)
			      (let ((cfc (cf (if (nlistp a2) (list a2) a2)))
				    (cf1 (cf (if (nlistp a1) (list a1) a1)))
				    trivial)
				(loop with cfc = cfc and cf1 = cf1
				      with cc and c1
				      while (or cf1 cfc)
				      do
				  (when cf1 (setq c1 (first cf1)))
				  (when cfc (setq cc (first cfc)))

				  (when (and (null cfc)
					     cf1)
				    (setq cc (copylist cc))
				    (setf (fourth cc)(fifth cc)))

				  (when (and (null cf1)
					     cfc)
				    (setq c1 (copylist c1))
				    (setf (fourth c1) (fifth c1)))

				  (setq cf1 (cdr cf1)
					cfc (cdr cfc))
				  (when (and (not (equal cc c1))
					     (not (loop for matters in difference-matters
							as f1 in c1
							as f2 in cc
							always (or (null matters)
								   (eql f1 f2)))))
				    (setq trivial t)))
				(when trivial (incf non-trivial))
				(unless (and quiet (> bad 0))
				  (format t "~&~A~%~A ~A #o~o ~A,~A ~A~& ~a ->~&~A"
					  (if trivial "" "(trivial) ")
					  (picture  shape i) shape i x-pos y-pos move
					  cfc cf1)
				  ))
			  finally
			    (unless (zerop bad)
			      (with-restart-options
				(((error "Continue"))
				 ((error "Use Center value for both")
				  (copy-array-contents center-fa fa)
				  )
				 ((error "Use ~A,~A value for both" x-pos y-pos)
				  (copy-array-contents fa center-fa)
				  ))
				(signal-error ("Total of ~A differences ~A non trivial"
					bad
					non-trivial)))
			      )))
		  ))))))
    :liberties 0
    :introns 0
    :shape shape
    :property property 
    ))

(defun shape-abnormalities (&key repair old-props 
				 shape (property *fate-property*) position introns)
  
  (shape-duplicate-abnormalities
    :repair repair
    :shape shape :property property :position position :introns introns)
  (shape-position-abnormalities :property property :shape shape)
  (when old-props
    (shape-result-abnormalities
      (first old-props)
      :shape shape :property property :position position :introns introns))
  (shape-liberties-abnormalities :repair repair
    :shape shape :property property :position position :introns introns)
  (shape-move-abnormalities
    :repair repair
    :shape shape :property property :position position :introns introns)
  (shape-outcome-abnormalities :repair repair :shape shape :position position :introns introns)
  (shape-fate-abnormalities :repair repair
    :shape shape :property property :position position :introns introns)
  (shape-dead-with-eye-abnormalities :repair repair :shape shape :property property)
  (shape-na-abnormalities :repair repair  :property property :shape shape)
  (shape-outside-abnormalities :repair repair :property property :shape shape)
  (shape-unknown-abnormalities :repair repair :property property :shape shape)
  )

(defun combine-fates ()
    ;this is hazardous, because fixups tend to create empty slots
  (loop for shape in (all-shapes zhash-shape-database) do
    (combine-one-fate shape)))

(defun combine-one-fate (shape &optional (props fate-properties))
  (loop with some for prop in props
	do
    (hash-cons-fate-properties shape prop)	  
    (loop for (nil nil sh) in (get-prop shape :dropped-shapes)
	  when (combine-one-fate sh)
	    do (setq some t))
	when (share-fate-properties shape prop)
	  do (setq some t)
	finally (return  some)))

3||#

0(defmethod (re-init zhash-shape-database) (&optional ignore)
  (clrhash self)
  (setf n-entries 0))

(defmethod (add-shape zhash-shape-database) (new &optional (newval new) must-be-new)
  (let* ((hash (zhash-set new))
	 (old (gethash hash self)))
    (cond (old (if must-be-new (error "shape ~S hash isn't new, old is ~S"
				      new old)))
	  (t (incf n-entries)
	     (setf (gethash hash self) (list newval 0))
	     (let ((is (get-all-isomers new)))
	       (setf (canonical-number new) n-entries)
	       (loop for i in is 
		 as h = (zhash-set i)
		 as old = (gethash h self)
		 do (cond ((eql h hash)(assert (eql (permutation-number i) 0)))
			  ((null old) (setf (gethash h self) (list new (permutation-number i))))
			  (t (error "shape ~S isomer ~S is already present" new i))))
	       )
	     ))))


(defmethod (recognise-shape zhash-shape-database) (new)
  (cond ((> (size new) shape-database-generation)
	 nil)
	(t (let ((v (gethash (zhash-set new) self)))
	     (when v (values (first v )(second v)))))))

(eval-when (load)
  (unless (find-shape "x x x x x x x")
    (build-shapes zhash-shape-database))
  ;(build-zshape-database shape-database zhash-shape-database)
  )


#||

 Intron 120. is incorrectly labeled dead for 2 liberties
 No fates for moving first in right-top
 Number adjacent is incorrectly recorded as 2

||#