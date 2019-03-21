
/*  
 * Copyright (c) 2011, Oracle
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of Sun Microsystems nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 *  Note:  In order to comply with the binary form redistribution 
 *         requirement in the above license, the licensee may include 
 *         a URL reference to a copy of the required copyright notice, 
 *         the list of conditions and the disclaimer in a human readable 
 *         file with the binary form of the code that is subject to the
 *         above license.  For example, such file could be put on a 
 *         Blu-ray disc containing the binary form of the code or could 
 *         be put in a JAR file that is broadcast via a digital television 
 *         broadcast medium.  In any event, you must include in any end 
 *         user licenses governing any code that includes the code subject 
 *         to the above license (in source and/or binary form) a disclaimer 
 *         that is at least as protective of Sun as the disclaimers in the 
 *         above license.
 * 
 *         A copy of the required copyright notice, the list of conditions and
 *         the disclaimer will be maintained at 
 *         https://hdcookbook.dev.java.net/misc/license.html .
 *         Thus, licensees may comply with the binary form redistribution
 *         requirement with a text file that contains the following text:
 * 
 *             A copy of the license(s) governing this code is located
 *             at https://hdcookbook.dev.java.net/misc/license.html
 */

package com.hdcookbook.mhpmenu;

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.commands.ActivatePartCommand;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.fontstrip.FontStripText;
import com.hdcookbook.grinxlet.GrinXlet;
//import com.hdcookbook.grin.media.Playlist; //bd-j concept not valid in mhp
//import com.hdcookbook.grin.media.PlayerWrangler;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.Profile;

import java.util.HashMap;
import java.util.Random;

/**
 * This is the director class for the Gun Bunny game.  We chose to put all
 * of the game logic in the director class.  The media control logic that
 * runs the starfield video is located in the xlet class.  By doing this
 * separation, we're able to run the entire game in GrinView on desktop
 * JDK, which makes debugging and experimentation much easier.
 * <p>
 * GrinBunnyDirector is tighly coupled to grinbunny_show.txt.  The director
 * consists almost entirely of public methods that are called from a
 * show using the java_command construct.  This means that they happen within
 * the animator thread when the show is in the command execution phase of
 * the animation loop.  For this reason, these methods
 * can safely operate directly on the GRIN scene graph, rather than by posting
 * commands to modify it.  Of course, the director could also change the
 * scene graph by posting commands; that would be slightly less efficient
 * and would defer the state change until slightly later in the same frame
 * of animation.
 * 
 * @author Bill Foote
 */
public class MHPMenuDirector extends Director {
	
	//checkbox menu member vars
	private int currentChapter=1;
	private boolean check[]={false,false,false,false};
	
	//simple counter member vars
	private boolean clockEnabled = false;
	private int max_digits=4;
	private int digits[] = new int[max_digits];
	private int clock = 0;
	
	//interpolation animation vars
	private boolean isAnimating = false;
	private boolean goRight;
	private int dukeX = 0;
	private int dukeSpeed = 5;

    public MHPMenuDirector() {
    }

    /**
     * {@inheritDoc}
     **/
    public void notifyDestroyed() {
       
    }

    /**
     * Called by a java_command in the show 
     **/
    public void chPrev() {
    	if(currentChapter>=1) {
			this.setAssemblyPart("as.chapter.slide", "pa.chapter.slide."+currentChapter+"to"+(currentChapter-1));
			currentChapter--;
		} else {
			currentChapter=1;
		}
    }

    /**
     * Called by a java_command in the show 
     **/
    public void chNext() {
    	if(currentChapter<=6) {
			this.setAssemblyPart("as.chapter.slide", "pa.chapter.slide."+currentChapter+"to"+(currentChapter+1));
			currentChapter++;
		} else {
			currentChapter=6;
		}
    }

    /**
     * Called by a java_command in the show 
     **/
    public void check(int checkNum) {
    	if(!check[checkNum]){
			check[checkNum]=true;
			this.setAssemblyPart("as.checkbox.checkbox"+(checkNum+1), "pa.checkbox.as.checkbox.checkbox"+(checkNum+1)+".001");
		} else {
			check[checkNum]=false;
			this.setAssemblyPart("as.checkbox.checkbox"+(checkNum+1), "pa.checkbox.as.checkbox.checkbox"+(checkNum+1)+".box");
		}
    }
    
    /**
     * Called by a java_command in the show 
     **/
    public void start() {
		clockEnabled=true;
    }
    
    /**
     * Called by a java_command in the show 
     **/
    public void stop() {
		clockEnabled=false;
    }
    
    /**
     * Called by a java_command in the show 
     **/
    public void count() {
    	if(clockEnabled){
			if(clock>9999)clock=0;
			clock++;
			//zero clock
			this.setAssemblyPart("as.counter.num_0", "pa.counter.as.counter.num_0.0");
			this.setAssemblyPart("as.counter.num_00", "pa.counter.as.counter.num_00.0");
			this.setAssemblyPart("as.counter.num_000", "pa.counter.as.counter.num_000.0");
			this.setAssemblyPart("as.counter.num_0000", "pa.counter.as.counter.num_0000.0");
			//set clock
			setClock();
		}
    }

    /**
	 * set the clock assembly
	 * this algorythm could be much better.
	 */
	public void setClock() {
		try {
			// set digits into an array.
			for (int i = 0; i < max_digits; i++) {
				if (i == 0) {
					digits[i] = clock % 10;
				} else {
					int value = clock;
					for (int j = i; j == 0; j--) {
						value = value - digits[j];
					}
					int tenPow = (int) Math.pow(10, i);
					digits[i] = (value / tenPow) % 10;
				}
			}

			// apply digits array to UI.
			String tempConcat = "0";
			for (int i = 0; i < digits.length; i++) {
				this.setAssemblyPart("as.counter.num_"+tempConcat, "pa.counter.as.counter.num_"+tempConcat+"."+digits[i]);				
				tempConcat = tempConcat + "0";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    /**
     * Called by a java_command in the show before every frame
     **/
    public void interpolateHeartbeat() {
    	if(isAnimating) {
			if(goRight && dukeX<400) {
				dukeX+=dukeSpeed;
				this.setTranslation("tn.duke", dukeX, 0);
			} else if(dukeX>0) {
				//go left
				dukeX-=dukeSpeed;
				this.setTranslation("tn.duke", dukeX, 0);
			}
		}
    }
    
    /**
     * Called by a java_command in the show 
     **/
    public void startAnimation() {
    	isAnimating=true;
    }
    
    /**
     * Called by a java_command in the show 
     **/
    public void stopAnimation() {
    	isAnimating=false;
    }

    /**
     * Called by a java_command in the show 
     **/
    public void setGoRight() {
    	goRight=true;
    }
    
    /**
     * Called by a java_command in the show 
     **/
    public void setGoLeft() {
    	goRight=false;
    }
    
	/**
	 * Set the active part of the specified assembly to the given part name.
	 * 
	 * @param assemblyName	Name of the Grin assembly
	 * @param partName		Name of the part in the assembly
	 */
	protected void setAssemblyPart(String assemblyName, String partName) {

		Show show = getShow();
		if (assemblyName != null && partName != null && show != null) {
			Feature f = show.getFeature(assemblyName);
			if (f != null && f instanceof Assembly) {
				Assembly a = (Assembly) f;
				f = a.findPart(partName);
				if (f != null) {
					ActivatePartCommand cmd = new ActivatePartCommand(show, a, f);
					if (cmd != null) {
						show.runCommand(cmd);
					}
				} else {
		            Debug.println("Part not found in show: " + partName);
				}
			} else {
	            Debug.println("Assembly not found in show: " + assemblyName);
			}
		}
	}
	
	/**
	 * Moves the specified translator feature to the values given.  Note, the number of coordinate 
	 * entries in the given arrays match those of the original translator, 
	 * otherwise this call is ignored.
	 * 
	 * @param translation	Name of Translation feature to modify	
	 * @param x				New x-coordinate relative to which to shift translation
	 * @param y				New y-coordinate relative to which to shift translation
	 */
	protected void setTranslation(String translation, int x, int y) {
		Show show = getShow();
		if (translation != null && show != null) {
			Feature f = show.getFeature(translation);
			if (f != null && f instanceof InterpolatedModel) {
				SetTranslationCommand cmd = new SetTranslationCommand(show, (InterpolatedModel) f, x, y);
				if (cmd != null) {
					show.runCommand(cmd);
				} else {
					Debug.println("Failed to instantiate SetTranslationCommand.");
				}
			} else {
				Debug.println("Translation not found in show: " + translation);
			}
		}
	}
	
// SetTranslationCommand Inner Class ================================================
	
	private static class SetTranslationCommand
		extends Command
	{
		private InterpolatedModel translation;
		private int x;
		private int y;
		
		public SetTranslationCommand(Show show, InterpolatedModel translation, int x, int y) {
			super(show);
			this.translation = translation;
			this.x = x;
			this.y = y;
		}
		
		public void execute() {
			if (this.translation != null) {
				// Apply adjustment factor
				this.translation.setField(Translator.X_FIELD, this.x);
				this.translation.setField(Translator.Y_FIELD, this.y);
			}
		}
	}

}
