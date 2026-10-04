package dev.aeternum;

/** Monotonic presentation clock. Small corrections change speed, never move time backward. */
final class PlaybackClock {
    private String track=""; private boolean initialized,playing; private double base,duration,rate=1; private long at;
    synchronized boolean initialized(){return initialized;}
    synchronized void reset(){initialized=false;}
    synchronized double position(long now){return Math.clamp(base+(playing?Math.max(0,now-at)/1e9*rate:0),0,duration>0?duration:Double.MAX_VALUE);}
    synchronized void update(String next,double reported,double length,boolean active,long now){
        double predicted=initialized?position(now):reported,delta=reported-predicted;
        boolean seek=!initialized||!track.equals(next)||Math.abs(delta)>1.5;
        base=seek?reported:predicted;
        // Pauses freeze the displayed position. Resuming and small quantized updates slew gently.
        rate=seek?1:Math.clamp(1+delta*.25,.8,1.2);
        track=next;duration=length;playing=active;at=now;initialized=true;
    }
}
