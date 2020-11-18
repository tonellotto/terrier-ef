package it.cnr.isti.hpclab.ef.util;

import java.text.DecimalFormat;


import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public final class SynchronizedProgressBar 
{
	private ProgressBar pb = null;
	
	private static SynchronizedProgressBar instance = null;
	
	public static SynchronizedProgressBar create(final String name, final int numSteps)
	{
		instance = new SynchronizedProgressBar(name, numSteps);
		return instance;		
	}
	
	public static SynchronizedProgressBar getInstance()
	{
		return instance;			
	}

	private SynchronizedProgressBar(final String name, final int numSteps)
	{
        this.pb = new ProgressBarBuilder()
                .setInitialMax(numSteps)
                .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                .setTaskName(name)
                .setUpdateIntervalMillis(1000)
                .showSpeed(new DecimalFormat("#.###"))
                .build();
	}
		
	@SuppressWarnings("deprecation")
	public synchronized void stop()
	{
		this.pb.stop();
	}
	
	public synchronized void step()
	{
		this.pb.step();
	}
}
