package lib;
/**
 * mouse state encodes the state of the mouse up, down, dragging, pinching and so on
 * @author Ddyer
 *
 */
public enum MouseState {
/** last mouse action was enter */
LAST_IS_ENTER,
/** last mouse action was mouse exit */
LAST_IS_EXIT,
/** last mouse action was mouse down */
LAST_IS_DOWN, 
/** last mouse action was mouse drag */

LAST_IS_DRAG, 
/** last mouse action was mouse up */

LAST_IS_UP, 
/** last mouse action was mouse move */

LAST_IS_MOVE,

/** last mouse action is a pinch gesture */
LAST_IS_PINCH,
/** mouse wheel action */
LAST_IS_WHEEL,

/** last is idle, nothing happening! */
LAST_IS_IDLE
};
