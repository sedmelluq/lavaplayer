
package ibxm;

public class Sample {
	public static final int
		FP_SHIFT = 15,
		FP_ONE = 1 << FP_SHIFT,
		FP_MASK = FP_ONE - 1;

	public static final int C2_PAL = 8287, C2_NTSC = 8363;

	public String name = "";
	public int volume = 0, panning = -1, relNote = 0, fineTune = 0;
	private int loopStart = 0, loopLength = 0;
	private short[] sampleData;
	
	/* Constants for the fixed-point sinc interpolator. */
	private static final int LOG2_FILTER_TAPS = 4; /* 16 taps. */
	private static final int FILTER_TAPS = 1 << LOG2_FILTER_TAPS;
	private static final int DELAY = FILTER_TAPS / 2;
	private static final int LOG2_TABLE_ACCURACY = 4;
	private static final int TABLE_ACCURACY = 1 << LOG2_TABLE_ACCURACY;
	private static final int TABLE_INTERP_SHIFT = FP_SHIFT - LOG2_TABLE_ACCURACY;
	private static final int TABLE_INTERP_ONE = 1 << TABLE_INTERP_SHIFT;
	private static final int TABLE_INTERP_MASK = TABLE_INTERP_ONE - 1;
	private static final int LOG2_NUM_TABLES = LOG2_FILTER_TAPS - 1;
	private static final int NUM_TABLES = 1 << LOG2_NUM_TABLES;
	private static final short[][] SINC_TABLES = calculateSincTables();

	private static short[][] calculateSincTables() {
		short[][] sincTables = new short[ NUM_TABLES ][];
		for( int tableIdx = 0; tableIdx < NUM_TABLES; tableIdx++ ) {
			sincTables[ tableIdx ] = calculateSincTable( 1.0 / ( tableIdx + 1 ) );
		}
		return sincTables;
	}

	private static short[] calculateSincTable( double lowpass ) {
		short[] sincTable = new short[ ( TABLE_ACCURACY + 1 ) * FILTER_TAPS ];
		double windDT = -2.0 * Math.PI / FILTER_TAPS;
		double sincDT = -Math.PI;
		int tableIdx = 0;
		for( int tableY = 0; tableY <= TABLE_ACCURACY; tableY++ ) {
			double fracT = tableY / ( double ) TABLE_ACCURACY;
			double sincT = Math.PI * ( FILTER_TAPS / 2 - 1 + fracT );
			double windT = Math.PI + sincT * 2.0 / FILTER_TAPS;
			for( int tableX = 0; tableX < FILTER_TAPS; tableX++ ) {
				double sincY = lowpass;
				if( sincT != 0 ) {
					sincY = Math.sin( lowpass * sincT ) / sincT;
				}
				/* Blackman-Harris window function.*/
				double windY = 0.35875;
				windY -= 0.48829 * Math.cos( windT );
				windY += 0.14128 * Math.cos( windT * 2 );
				windY -= 0.01168 * Math.cos( windT * 3 );
				sincTable[ tableIdx++ ] = ( short ) Math.round( sincY * windY * 32767 );
				sincT += sincDT;
				windT += windDT;
			}
		}
		return sincTable;
	}

	public void setSampleData( short[] sampleData, int loopStart, int loopLength, boolean pingPong ) {
		int sampleLength = sampleData.length;
		// Fix loop if necessary.
		if( loopStart < 0 || loopStart > sampleLength )
			loopStart = sampleLength;
		if( loopLength < 0 || ( loopStart + loopLength ) > sampleLength )
			loopLength = sampleLength - loopStart;
		sampleLength = loopStart + loopLength;
		// Compensate for sinc-interpolator delay.
		loopStart += DELAY;
		// Allocate new sample.
		int newSampleLength = DELAY + sampleLength + ( pingPong ? loopLength : 0 ) + FILTER_TAPS;
		short[] newSampleData = new short[ newSampleLength ];
		System.arraycopy( sampleData, 0, newSampleData, DELAY, sampleLength );
		sampleData = newSampleData;
		if( pingPong ) {
			// Calculate reversed loop.
			int loopEnd = loopStart + loopLength;
			for( int idx = 0; idx < loopLength; idx++ )
				sampleData[ loopEnd + idx ] = sampleData[ loopEnd - idx - 1 ];
			loopLength *= 2;
		}
		// Extend loop for sinc interpolator.
		for( int idx = loopStart + loopLength, end = idx + FILTER_TAPS; idx < end; idx++ )
			sampleData[ idx ] = sampleData[ idx - loopLength ];
		this.sampleData = sampleData;
		this.loopStart = loopStart;
		this.loopLength = loopLength;
	}

	public void resampleNearest( int sampleIdx, int sampleFrac, int step,
			int leftGain, int rightGain, int[] mixBuffer, int offset, int length ) {
		int loopLen = loopLength;
		int loopEnd = loopStart + loopLen;
		sampleIdx += DELAY;
		if( sampleIdx >= loopEnd )
			sampleIdx = normaliseSampleIdx( sampleIdx );
		short[] data = sampleData;
		int outIdx = offset << 1;
		int outEnd = ( offset + length ) << 1;
		while( outIdx < outEnd ) {
			if( sampleIdx >= loopEnd ) {
				if( loopLen < 2 ) break;
				while( sampleIdx >= loopEnd ) sampleIdx -= loopLen;
			}
			int y = data[ sampleIdx ];
			mixBuffer[ outIdx++ ] += y * leftGain >> FP_SHIFT;
			mixBuffer[ outIdx++ ] += y * rightGain >> FP_SHIFT;
			sampleFrac += step;
			sampleIdx += sampleFrac >> FP_SHIFT;
			sampleFrac &= FP_MASK;
		}
	}
	
	public void resampleLinear( int sampleIdx, int sampleFrac, int step,
			int leftGain, int rightGain, int[] mixBuffer, int offset, int length ) {
		int loopLen = loopLength;
		int loopEnd = loopStart + loopLen;
		sampleIdx += DELAY;
		if( sampleIdx >= loopEnd )
			sampleIdx = normaliseSampleIdx( sampleIdx );
		short[] data = sampleData;
		int outIdx = offset << 1;
		int outEnd = ( offset + length ) << 1;
		while( outIdx < outEnd ) {
			if( sampleIdx >= loopEnd ) {
				if( loopLen < 2 ) break;
				while( sampleIdx >= loopEnd ) sampleIdx -= loopLen;
			}
			int c = data[ sampleIdx ];
			int m = data[ sampleIdx + 1 ] - c;
			int y = ( m * sampleFrac >> FP_SHIFT ) + c;
			mixBuffer[ outIdx++ ] += y * leftGain >> FP_SHIFT;
			mixBuffer[ outIdx++ ] += y * rightGain >> FP_SHIFT;
			sampleFrac += step;
			sampleIdx += sampleFrac >> FP_SHIFT;
			sampleFrac &= FP_MASK;
		}
	}

	public void resampleSinc( int sampleIdx, int sampleFrac, int step,
			int leftGain, int rightGain, int[] mixBuffer, int offset, int length ) {
		int tableIdx = 0;
		if( step > FP_ONE ) {
			// Increase lowpass filter to avoid aliasing.
			tableIdx = ( step >> FP_SHIFT ) - 1;
			if( tableIdx >= NUM_TABLES ) {
				tableIdx = NUM_TABLES - 1;
			}
		}
		short[] sincTable = SINC_TABLES[ tableIdx ];
		int loopLen = loopLength;
		int loopEnd = loopStart + loopLen;
		if( sampleIdx >= loopEnd )
			sampleIdx = normaliseSampleIdx( sampleIdx );
		short[] data = sampleData;
		int outIdx = offset << 1;
		int outEnd = ( offset + length ) << 1;
		while( outIdx < outEnd ) {
			if( sampleIdx >= loopEnd ) {
				if( loopLen < 2 ) break;
				while( sampleIdx >= loopEnd ) sampleIdx -= loopLen;
			}
			int tableIdx1 = ( sampleFrac >> TABLE_INTERP_SHIFT ) << LOG2_FILTER_TAPS;
			int tableIdx2 = tableIdx1 + FILTER_TAPS;
			int a1 = 0, a2 = 0;
			for( int tap = 0; tap < FILTER_TAPS; tap++ ) {
				a1 += sincTable[ tableIdx1 + tap ] * data[ sampleIdx + tap ];
				a2 += sincTable[ tableIdx2 + tap ] * data[ sampleIdx + tap ];
			}
			a1 >>= FP_SHIFT;
			a2 >>= FP_SHIFT;
			int y = a1 + ( ( a2 - a1 ) * ( sampleFrac & TABLE_INTERP_MASK ) >> TABLE_INTERP_SHIFT );
			mixBuffer[ outIdx++ ] += y * leftGain >> FP_SHIFT;
			mixBuffer[ outIdx++ ] += y * rightGain >> FP_SHIFT;
			sampleFrac += step;
			sampleIdx += sampleFrac >> FP_SHIFT;
			sampleFrac &= FP_MASK;
		}
	}

	public int normaliseSampleIdx( int sampleIdx ) {
		int loopOffset = sampleIdx - loopStart;
		if( loopOffset > 0 ) {
			sampleIdx = loopStart;
			if( loopLength > 1 ) sampleIdx += loopOffset % loopLength;
		}
		return sampleIdx;
	}
	
	public boolean looped() {
		return loopLength > 1;
	}

	public void toStringBuffer( StringBuffer out ) {
		out.append( "Name: " + name + '\n' );
		out.append( "Volume: " + volume + '\n' );
		out.append( "Panning: " + panning + '\n' );
		out.append( "Relative Note: " + relNote + '\n' );
		out.append( "Fine Tune: " + fineTune + '\n' );
		out.append( "Loop Start: " + loopStart + '\n' );
		out.append( "Loop Length: " + loopLength + '\n' );
		/*
		out.append( "Sample Data: " );
		for( int idx = 0; idx < sampleData.length; idx++ )
			out.append( sampleData[ idx ] + ", " );
		out.append( '\n' );
		*/
	}
}
