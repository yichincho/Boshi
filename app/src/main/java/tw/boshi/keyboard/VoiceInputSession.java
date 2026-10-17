package tw.boshi.keyboard;

/** A single dictation transaction. Late/duplicate callbacks never insert text. */
final class VoiceInputSession {
 private enum State { IDLE, LISTENING, PROCESSING }
 private State state=State.IDLE;
 private int generation;
 int start(){if(active())return -1;state=State.LISTENING;return ++generation;}
 boolean active(){return state!=State.IDLE;}
 boolean listening(){return state==State.LISTENING;}
 boolean processing(){return state==State.PROCESSING;}
 boolean current(int token){return active()&&token==generation;}
 boolean stop(int token){if(!current(token)||!listening())return false;state=State.PROCESSING;return true;}
 boolean finish(int token){if(!current(token))return false;state=State.IDLE;return true;}
 void cancel(){generation++;state=State.IDLE;}
}
