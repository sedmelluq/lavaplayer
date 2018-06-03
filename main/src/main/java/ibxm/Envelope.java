
package ibxm;

public class Envelope {
	public boolean enabled = false, sustain = false, looped = false;
	public int sustainTick = 0, loopStartTick = 0, loopEndTick = 0;
	public int numPoints = 1;
	public int[] pointsTick = new int[ 1 ];
	public int[] pointsAmpl = new int[ 1 ];
	
	public int nextTick( int tick, boolean keyOn ) {
		tick++;
		if( looped && tick >= loopEndTick ) tick = loopStartTick;
		if( sustain && keyOn && tick >= sustainTick ) tick = sustainTick;
		return tick;
	}
	
	public int calculateAmpl( int tick ) {
		int ampl = pointsAmpl[ numPoints - 1 ];
		if( tick < pointsTick[ numPoints - 1 ] ) {
			int point = 0;
			for( int idx = 1; idx < numPoints; idx++ )
				if( pointsTick[ idx ] <= tick ) point = idx;
			int dt = pointsTick[ point + 1 ] - pointsTick[ point ];
			int da = pointsAmpl[ point + 1 ] - pointsAmpl[ point ];
			ampl = pointsAmpl[ point ];
			ampl += ( ( da << 24 ) / dt ) * ( tick - pointsTick[ point ] ) >> 24;
		}
		return ampl;
	}
	
	public void toStringBuffer( StringBuffer out ) {
		out.append( "Enabled: " + enabled + '\n' );
		out.append( "Sustain: " + sustain + '\n' );
		out.append( "Looped: " + looped + '\n' );
		out.append( "Sustain Tick: " + sustainTick + '\n' );
		out.append( "Loop Start Tick: " + loopStartTick + '\n' );
		out.append( "Loop End Tick: " + loopEndTick + '\n' );
		out.append( "Num Points: " + numPoints + '\n' );
		out.append( "Points: " );
		for( int point = 0; point < numPoints; point++ ) {
			out.append( "(" + pointsTick[ point ] + ", " + pointsAmpl[ point ] + "), " );
		}
		out.append( '\n' );
	}
}
