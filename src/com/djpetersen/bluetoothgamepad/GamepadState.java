package com.djpetersen.bluetoothgamepad;

/**
 * This code store a 'state' of the gamepad. It keeps a tally of the buttons
 * pressed stored as ones and zeros. Because we only have 8 buttons, we can 
 * use a single byte.
 * 
 * @author derek
 *
 */
public class GamepadState {

	/**
	 * The stored 'state'
	 */
	private byte MYSTATE; /* not the app for ISU */

	public GamepadState() {
		MYSTATE = 0;
	}

	/**
	 * Toggles a state change based on a NesButton parameter
	 * @param s 
	 */
	public synchronized void addState(NesButton s) {
		switch (s) {
		case A:
			MYSTATE |= 8;
			break;
		case B:
			MYSTATE |= 4;
			break;
		case DOWN:
			MYSTATE |= 64;
			break;
		case LEFT:
			MYSTATE |= 32;
			break;
		case RIGHT:
			MYSTATE |= 16;
			break;
		case SELECT:
			MYSTATE |= 1;
			break;
		case START:
			MYSTATE |= 2;
			break;
		case UP:
			MYSTATE |= 0b10000000;
			break;
		default:
			/* NOP */
			break;

		}
	}
	
	/**
	 * Unsets a flag on the state
	 * @param s
	 */
	public synchronized void rmState(NesButton s) {
		switch (s) {
		case A:
			MYSTATE &= ~8;
			break;
		case B:
			MYSTATE &= ~4;
			break;
		case DOWN:
			MYSTATE &= ~64;
			break;
		case LEFT:
			MYSTATE &= ~32;
			break;
		case RIGHT:
			MYSTATE &= ~16;
			break;
		case SELECT:
			MYSTATE &= ~1;
			break;
		case START:
			MYSTATE &= ~2;
			break;
		case UP:
			MYSTATE &= 0b01111111;
			break;
		default:
			/* NOP */
			break;

		}
	}
	
	/**
	 * retrieves the state
	 * @return
	 */
	public synchronized byte getState() {
		return MYSTATE;
	}
}
