package bridge;


public interface FocusListener  {

    /**
     * Invoked when a component gains the keyboard focus.
     */
    public void focusGained(FocusEvent e);

    /**
     * Invoked when a component loses the keyboard focus.
     */
    public void focusLost(FocusEvent e);
}
