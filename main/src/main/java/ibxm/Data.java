
package ibxm;

/* A data array dynamically loaded from an InputStream. */
public class Data {
	private int bufLen;
	private byte[] buffer;
	private java.io.InputStream stream;

	public Data( java.io.InputStream inputStream ) throws java.io.IOException {
		bufLen = 1 << 16;
		buffer = new byte[ bufLen ];
		stream = inputStream;
		readFully( stream, buffer, 0, bufLen );
	}

	public Data( byte[] data ) {
		bufLen = data.length;
		buffer = data;
	}

	public byte sByte( int offset ) throws java.io.IOException {
		load( offset, 1 );
		return buffer[ offset ];
	}

	public int uByte( int offset ) throws java.io.IOException {
		load( offset, 1 );
		return buffer[ offset ] & 0xFF;
	}

	public int ubeShort( int offset ) throws java.io.IOException {
		load( offset, 2 );
		return ( ( buffer[ offset ] & 0xFF ) << 8 ) | ( buffer[ offset + 1 ] & 0xFF );
	}

	public int uleShort( int offset ) throws java.io.IOException {
		load( offset, 2 );
		return ( buffer[ offset ] & 0xFF ) | ( ( buffer[ offset + 1 ] & 0xFF ) << 8 );
	}

	public int uleInt( int offset ) throws java.io.IOException {
		load( offset, 4 );
		int value = buffer[ offset ] & 0xFF;
		value = value | ( ( buffer[ offset + 1 ] & 0xFF ) << 8 );
		value = value | ( ( buffer[ offset + 2 ] & 0xFF ) << 16 );
		value = value | ( ( buffer[ offset + 3 ] & 0x7F ) << 24 );
		return value;
	}

	public String strLatin1( int offset, int length ) throws java.io.IOException {
		load( offset, length );
		char[] str = new char[ length ];
		for( int idx = 0; idx < length; idx++ ) {
			int chr = buffer[ offset + idx ] & 0xFF;
			str[ idx ] = chr < 32 ? 32 : ( char ) chr;
		}
		return new String( str );
	}

	public String strCp850( int offset, int length ) throws java.io.IOException {
		load( offset, length );
		try {
			char[] str = new String( buffer, offset, length, "Cp850" ).toCharArray();
			for( int idx = 0; idx < str.length; idx++ ) {
				str[ idx ] = str[ idx ] < 32 ? 32 : str[ idx ];
			}
			return new String( str );
		} catch( java.io.UnsupportedEncodingException e ) {
			return strLatin1( offset, length );
		}
	}

	public short[] samS8( int offset, int length ) throws java.io.IOException {
		load( offset, length );
		short[] sampleData = new short[ length ];
		for( int idx = 0; idx < length; idx++ ) {
			sampleData[ idx ] = ( short ) ( buffer[ offset + idx ] << 8 );
		}
		return sampleData;
	}

	public short[] samS8D( int offset, int length ) throws java.io.IOException {
		load( offset, length );
		short[] sampleData = new short[ length ];
		int sam = 0;
		for( int idx = 0; idx < length; idx++ ) {
			sam += buffer[ offset + idx ];
			sampleData[ idx ] = ( short ) ( sam << 8 );
		}
		return sampleData;
	}

	public short[] samU8( int offset, int length ) throws java.io.IOException {
		load( offset, length );
		short[] sampleData = new short[ length ];
		for( int idx = 0; idx < length; idx++ ) {
			sampleData[ idx ] = ( short ) ( ( ( buffer[ offset + idx ] & 0xFF ) - 128 ) << 8 );
		}
		return sampleData;
	}

	public short[] samS16( int offset, int samples ) throws java.io.IOException {
		load( offset, samples * 2 );
		short[] sampleData = new short[ samples ];
		for( int idx = 0; idx < samples; idx++ ) {
			sampleData[ idx ] = ( short ) ( ( buffer[ offset + idx * 2 ] & 0xFF ) | ( buffer[ offset + idx * 2 + 1 ] << 8 ) );
		}
		return sampleData;
	}

	public short[] samS16D( int offset, int samples ) throws java.io.IOException {
		load( offset, samples * 2 );
		short[] sampleData = new short[ samples ];
		int sam = 0;
		for( int idx = 0; idx < samples; idx++ ) {
			sam += ( buffer[ offset + idx * 2 ] & 0xFF ) | ( buffer[ offset + idx * 2 + 1 ] << 8 );
			sampleData[ idx ] = ( short ) sam;
		}
		return sampleData;
	}

	public short[] samU16( int offset, int samples ) throws java.io.IOException {
		load( offset, samples * 2 );
		short[] sampleData = new short[ samples ];
		for( int idx = 0; idx < samples; idx++ ) {
			int sam = ( buffer[ offset + idx * 2 ] & 0xFF ) | ( ( buffer[ offset + idx * 2 + 1 ] & 0xFF ) << 8 );
			sampleData[ idx ] = ( short ) ( sam  - 32768 );
		}
		return sampleData;
	}

	private void load( int offset, int length ) throws java.io.IOException {
		while( offset + length > bufLen ) {
			int newBufLen = bufLen << 1;
			byte[] newBuf = new byte[ newBufLen ];
			System.arraycopy( buffer, 0, newBuf, 0, bufLen );
			if( stream != null ) {
				readFully( stream, newBuf, bufLen, newBufLen - bufLen );
			}
			bufLen = newBufLen;
			buffer = newBuf;
		}
	}

	private static void readFully( java.io.InputStream inputStream, byte[] buffer, int offset, int length ) throws java.io.IOException {
		int read = 1, end = offset + length;
		while( read > 0 ) {
			read = inputStream.read( buffer, offset, end - offset );
			offset += read;
		}
	}
}
