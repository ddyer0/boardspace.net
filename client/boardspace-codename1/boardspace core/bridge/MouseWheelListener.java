package bridge;


import java.util.EventListener;

public interface MouseWheelListener extends EventListener {

    /**
     * Invoked when the mouse wheel is rotated.
     * @see MouseWheelEvent
     */
    public void mouseWheelMoved(MouseWheelEvent e);
}
