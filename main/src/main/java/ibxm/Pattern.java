
package ibxm;

public class Pattern {
	public int numRows;
	public byte[] data;

	public Pattern( int numChannels, int numRows ) {
		this.numRows = numRows;
		data = new byte[ numChannels * numRows * 5 ];
	}

	public Note getNote( int index, Note note ) {
		int offset = index * 5;
		note.key = data[ offset ] & 0xFF;
		note.instrument = data[ offset + 1 ] & 0xFF;
		note.volume = data[ offset + 2 ] & 0xFF;
		note.effect = data[ offset + 3 ] & 0xFF;
		note.param = data[ offset + 4 ] & 0xFF;
		return note;
	}

	public void toStringBuffer( StringBuffer out ) {
		Note note = new Note();
		char[] chars = new char[ 10 ];
		int numChannels = data.length / ( numRows * 5 );
		for( int row = 0; row < numRows; row++ ) {
			for( int channel = 0; channel < numChannels; channel++ ) {
				getNote( numChannels * row + channel, note );
				note.toChars( chars );
				out.append( chars );
				out.append( ' ' );
			}
			out.append( '\n' );
		}
	}

	public String toString() {
		int numChannels = data.length / ( numRows * 5 );
		StringBuffer stringBuffer = new StringBuffer( numRows * numChannels * 11 + numRows );
		toStringBuffer( stringBuffer );
		return stringBuffer.toString();
	}
}
