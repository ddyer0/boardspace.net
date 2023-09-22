/* copyright notice */package loa;

class Evaluator implements Move_Mapper
{
    Player_Info player;

    Evaluator(Player_Info pl)
    {
        player = pl;
    }

    public void Move_Map(Line_Info info, Move_Spec move)
    {
        player.Evaluate_One_Move(move);
    }
}
