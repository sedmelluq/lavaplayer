
package ibxm;

public class Note {
	public int key, instrument, volume, effect, param;

	private static final String b36ToString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String keyToString = "A-A#B-C-C#D-D#E-F-F#G-G#";

	public String toString() {
		return new String( toChars( new char[ 10 ] ) );
	}

	public char[] toChars( char[] chars ) {
		keyToChars( key, chars );
		chars[ 3 ] = ( instrument > 0xF && instrument < 0xFF ) ? b36ToString.charAt( ( instrument >> 4 ) & 0xF ) : '-';
		chars[ 4 ] = ( instrument > 0x0 && instrument < 0xFF ) ? b36ToString.charAt( instrument & 0xF ) : '-';
		chars[ 5 ] = ( volume > 0xF && volume < 0xFF ) ? b36ToString.charAt( ( volume >> 4 ) & 0xF ) : '-';
		chars[ 6 ] = ( volume > 0x0 && volume < 0xFF ) ? b36ToString.charAt( volume & 0xF ) : '-';
		if( ( effect > 0 || param > 0 ) && effect < 36 ) {
			chars[ 7 ] = b36ToString.charAt( effect );
		} else if( effect > 0x80 && effect < 0x9F ) {
			chars[ 7 ] = ( char ) ( 96 + ( effect & 0x1F ) );
		} else {
			chars[ 7 ] = '-';
		}
		chars[ 8 ] = ( effect > 0 || param > 0 ) ? b36ToString.charAt( ( param >> 4 ) & 0xF ) : '-';
		chars[ 9 ] = ( effect > 0 || param > 0 ) ? b36ToString.charAt( param & 0xF ) : '-';
		return chars;
	}

	private static void keyToChars( int key, char[] out ) {
		out[ 0 ] = ( key > 0 && key < 118 ) ? keyToString.charAt( ( ( key + 2 ) % 12 ) * 2 ) : '-';
		out[ 1 ] = ( key > 0 && key < 118 ) ? keyToString.charAt( ( ( key + 2 ) % 12 ) * 2 + 1 ) : '-';
		out[ 2 ] = ( key > 0 && key < 118 ) ? ( char ) ( '0' + ( key + 2 ) / 12 ) : '-';
	}
}
