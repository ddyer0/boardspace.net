package bugs;

import com.codename1.ui.Font;

import bridge.SystemFont;
import bugs.data.Profile;
import lib.G;
import lib.Graphics;
import lib.Image;
import lib.InternationalStrings;
import lib.Random;
import lib.TextContainer;

public class DietGoal implements Goal,BugsConstants
{	static int dietGoalCounter = 0;
	public int uid;
	public BugsChip chip;
	public int multiplier;
	public String text;
	public String longText;
	enum TestType { herbivore, carnivore, negavore, parasite, scavenger, flying }
	public TestType test;
	public String getCommonName() { return text; }
	public DietGoal(TestType tt,BugsChip ch,int mu,String msg,String longmsg)
	{
		dietGoalCounter++;
		test = tt;
		uid = dietGoalCounter;
		chip = ch;
		multiplier = mu;
		text = msg;
		longText = longmsg;
	}
	public int getUid() {
		return uid;
	}

	public void drawExtendedChip(Graphics gc, Font font, int xp, int yp, int w, int h,boolean x2)
	{
		String sd = G.getTranslations().get(longText,x2 ? multiplier*2 : multiplier);
		int margin = h/20;
		TextContainer textContainer = new TextContainer(BugsId.Description);
		if(sd!=null)
		{	
				textContainer.setBounds(xp+margin,yp+margin,w-margin*2,h-margin*2);
				textContainer.setText(sd);
				textContainer.setFont(SystemFont.getFont(font,w/10));
				textContainer.selectFontSize();
				textContainer.flagExtensionLines = false;
				textContainer.frameAndFill=false;
				textContainer.setVisible(true);
				textContainer.redrawBoard(gc,null);
		}
	}
	public String getHelpText() {
		return text;
	}


	public Image getIllustrationImage() {
		return chip.getImage();
	}

	public double pointValue(BugsBoard parentBoard,BugCard bug) {
		boolean match = false;
		Profile profile = bug.getProfile();
		switch(test)
		{
		case carnivore:	match = profile.isPredator(); break;
		case scavenger: match = profile.isScavenger(); break;
		case negavore: match = profile.isNegavore(); break;
		case parasite: match= profile.isParasite(); break;
		case flying: match = profile.isFlying(); break;
		case herbivore: 
			if(parentBoard.revision<101) 
				{ match = profile.isPrey(); 
				}
				else 
				{ 	match = profile.isHerbivore(); 
				}
			break;	// catch all
		default: throw G.Error("Not expecting %s",test);
		}
		return match ? multiplier : 0;
	}
	
	public boolean matches(BugsBoard b,BugCard bug,boolean wild)
	{	
		return pointValue(b,bug)>0;
	}
	static DietGoal FlyingGoal = new DietGoal(TestType.flying,BugsChip.Wings,2,"Flying Bugs",
			"Each bug that can fly scores #1{ points, point, points}");
	
	static DietGoal[] DietGoals = 
		{
		new DietGoal(TestType.herbivore,BugsChip.Vegetarian,1,"Herbivores",
				"Each herbivore bug scores #1{ points, point, points}"),		
		new DietGoal(TestType.carnivore,BugsChip.Predator,2,"Predators",
				"Each predator bug scores #1{ points, point, points}"),	
		new DietGoal(TestType.parasite,BugsChip.Parasite,2,"Parasites",
				"Each parasite bug scores #1{ points, point, points}"),	
		new DietGoal(TestType.negavore,BugsChip.Negavore,2,"Non-Eating Bugs",
				"Each non-eating bug scores #1{ points, point, points}"),	
		new DietGoal(TestType.scavenger,BugsChip.Scavenger,2,"Scavenger",
				"Each scavenger bug scores #1{ points, point, points}"),	
		FlyingGoal
		
		};
	public String legend(boolean twice)
	{
		int p = (multiplier*(twice ? 2 : 1));
		return ""+p+" per";
		
	}
	public static Goal randomGoal(Random r,BugsBoard b,BugsChip bugs[]) {
		
		// pick a rand card to be targeted - this assures that the goalMarket
		// are in reasonable proportion to the actual population
		BugCard chip = (BugCard)bugs[r.nextInt(bugs.length)];
		if(r.nextDouble()<FLYING_GOAL_PERCENTAGE && FlyingGoal.matches(b,chip,false))
		{
			return FlyingGoal;
		}
		for(DietGoal goal : DietGoals)
		{
			if(goal.matches(b,chip,false)) { return goal; }
		}
		throw G.Error("no goal matches "+chip);
	}
	public static void putStrings()
	{	for(DietGoal goal : DietGoals)
		{
		InternationalStrings.put(goal.text);
		InternationalStrings.put(goal.longText);
		}
	}
}
