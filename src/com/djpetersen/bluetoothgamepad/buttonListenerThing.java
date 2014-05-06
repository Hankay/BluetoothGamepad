package com.djpetersen.bluetoothgamepad;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * In order to avoid a lot of repetition in the main activity,
 * we use this file to set similar listeners for all of the
 * NES buttons.
 * @author derek
 *
 */
public class buttonListenerThing implements OnTouchListener {
	private NesButton mapped_button;
	private GamepadState target_state;
	public buttonListenerThing(NesButton s, GamepadState g) {
		this.mapped_button = s;
		this.target_state = g;
	}
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			target_state.addState(mapped_button);
			return true;
		}
		else if(event.getAction() == MotionEvent.ACTION_UP) {
			target_state.rmState(mapped_button);
			return true;
		}
		return false;
	}
	
}
