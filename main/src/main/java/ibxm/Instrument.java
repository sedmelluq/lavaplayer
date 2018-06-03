
package ibxm;

public class Instrument {
	public String name = "";
	public int numSamples = 1;
	public int vibratoType = 0, vibratoSweep = 0, vibratoDepth = 0, vibratoRate = 0;
	public int volumeFadeOut = 0;
	public Envelope volumeEnvelope = new Envelope();
	public Envelope panningEnvelope = new Envelope();
	public int[] keyToSample = new int[ 97 ];
	public Sample[] samples = new Sample[] { new Sample() };

	public void toStringBuffer( StringBuffer out ) {
		out.append( "Name: " + name + '\n' );
		if( numSamples > 0 ) {
			out.append( "Num Samples: " + numSamples + '\n' );
			out.append( "Vibrato Type: " + vibratoType + '\n' );
			out.append( "Vibrato Sweep: " + vibratoSweep + '\n' );
			out.append( "Vibrato Depth: " + vibratoDepth + '\n' );
			out.append( "Vibrato Rate: " + vibratoRate + '\n' );
			out.append( "Volume Fade Out: " + volumeFadeOut + '\n' );
			out.append( "Volume Envelope:\n" );
			volumeEnvelope.toStringBuffer( out );
			out.append( "Panning Envelope:\n" );
			panningEnvelope.toStringBuffer( out );
			for( int samIdx = 0; samIdx < numSamples; samIdx++ ) {
				out.append( "Sample " + samIdx + ":\n" );
				samples[ samIdx ].toStringBuffer( out );
			}
			out.append( "Key To Sample: " );
			for( int keyIdx = 1; keyIdx < 97; keyIdx++ )
				out.append( keyToSample[ keyIdx ] + ", " );
			out.append( '\n' );
		}
	}
}
