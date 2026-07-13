package graphicstest;

import java.util.Enumeration;

import bridge.BSClip;
import bridge.Color;
import bridge.BSClip.AudioFormat;
import graphicstest.GraphicsViewer.TestAble;
import lib.Graphics;
import lib.SoundManager;

class soundclips implements TestAble
{	// games known to have custom clips:  Arimaa BlackDeath Cannon Euphoria Gobblet Honey Imagine Mutton
	//    pendulum, quinamid sprint viticulture warp6 yspahan
	boolean inited = false;
	public void init(){
		//SoundManager.preloadSounds(ArimaaViewer.soundNames);
		//SoundManager.preloadSounds(BlackDeathViewer.soundNames);
		//SoundManager.preloadSounds(CannonViewer.soundNames);
		//SoundManager.preloadSounds(EuphoriaViewer.soundNames);
		//SoundManager.preloadSounds(EuphoriaViewer.DIE_ROLL);
		//SoundManager.preloadSounds(GobGameViewer.soundNames);
		//SoundManager.preloadSounds(HoneyViewer.soundNames);
		//SoundManager.preloadSounds(ImagineViewer.soundNames);
		//SoundManager.preloadSounds(MuttonGameViewer.Sounds);
		//SoundManager.preloadSounds(PendulumViewer.soundNames);
		//SoundManager.preloadSounds(QuinamidViewer.rotates);
		//SoundManager.preloadSounds(QuinamidViewer.shifts);
		//SoundManager.preloadSounds(SprintViewer.soundNames);
		//SoundManager.preloadSounds(ViticultureViewer.soundNames);
		//SoundManager.preloadSounds(Warp6Viewer.soundNames);
		//SoundManager.preloadSounds(YspahanViewer.Sounds);
		inited = true;
	}
	Enumeration<String>keys = null;
	String clip = null;
	int mystep = 0;
	public void runTest(Graphics gc,int x,int y,int w,int h) {
		if(!inited) { init(); }
		if(keys==null || !keys.hasMoreElements()) 
			{ keys = SoundManager.getInstance().soundClips.keys();
			}
		if(mystep!=GraphicsViewer.step) { clip=null; mystep=GraphicsViewer.step; }
		if(clip==null) { clip = keys.nextElement(); }
		gc.setColor(Color.black);
		gc.fillRect(0,0,w,h);
		gc.setColor(Color.white);
		gc.Text(""+clip,w/10,h/10);
		BSClip sound = SoundManager.loadASoundClip(clip);
		AudioFormat format = sound.getFormat();
		gc.Text(""+format,w/10,h/10*2);
		if(SoundManager.soundIdle()) 
		{  	gc.Text("playing "+clip,w/10,3*h/10);
			SoundManager.playASoundClip(clip,1000); 
		}

		
	}
}
