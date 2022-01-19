package lib;

import java.awt.Component;

public interface ImageConsumer
{
	// this is what we need from the consumer, in addition to being a Component
	void setLowMemory(String string);	
	Component getMediaComponent();
}
