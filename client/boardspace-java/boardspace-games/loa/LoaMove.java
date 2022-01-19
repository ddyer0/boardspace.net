package loa;

public interface LoaMove
{
    public int fromX();

    public int fromY();

    public int toX();

    public int toY();

    public int N();

    public boolean captures();

    public String moveString();
}
