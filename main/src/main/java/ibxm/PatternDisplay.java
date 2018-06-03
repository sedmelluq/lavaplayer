
package ibxm;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Image;

public class PatternDisplay extends Canvas {
	public static final int CHANNEL_WIDTH = 88;

	private int pan, channels = 0;
	private Image charset, image;

	private boolean[] muted;
	private short[][] buffer;

	private int[] fxclr = new int[] {
		/* 0 1 2 3 4 5 6 7 8 9 : ; < = > ? */
		   1,1,1,1,1,7,7,5,5,4,0,0,0,0,0,0,
		/* @ A B C D E F G H I J K L M N O */
		   0,5,6,5,6,0,6,5,5,0,0,0,4,0,0,0,
		/* P Q R S T U V W X Y Z [ \ ] ^ _ */
		   5,0,4,0,5,0,0,0,1,0,0,0,0,0,0,0,
		/* ` a b c d e f g h i j k l m n o */
		   0,6,6,6,5,1,1,1,1,5,1,7,7,0,0,4,
		/* p q r s t u v w x y z { | } ~ */
		   0,4,5,0,6,1,5,0,0,0,0,0,0,0,0
	};

	private int[] exclr = new int[] {
		/* 0 1 2 3 4 5 6 7 8 9 : ; < = > ? @ A B C D E F */
		   0,1,1,1,1,1,6,5,0,4,0,0,0,0,0,0,0,5,5,4,4,4,4
	};

	private int[] sxclr = new int[] {
		/* 0 1 2 3 4 5 6 7 8 9 : ; < = > ? @ A B C D E F */
		   5,4,1,1,5,0,0,0,5,0,0,0,0,0,0,0,0,5,4,4,4,4,4
	};

	private int[] vcclr = new int[] {
		/* 0 1 2 3 4 5 6 7 8 9 : ; < = > ? @ A B C D E F */
		   5,5,5,5,5,0,5,5,5,5,0,0,0,0,0,0,0,1,1,5,5,5,1
	};

	private synchronized void drawBuffer( int x0, int y0, int x1, int y1 ) {
		int cols = getBufferWidth();
		int rows = getBufferHeight();
		if( charset == null ) {
			initCharset();
		}
		if( image == null ) {
			image = createImage( cols * 8, rows * 16 );
		}
		//System.out.println(x0 + "," + x1 + "," +y0 + "," +y1);
		if( x0 < cols && y0 < rows ) {
			if( x0 < 0 ) x0 = 0;
			if( y0 < 0 ) y0 = 0;
			if( x1 > cols ) x1 = cols;
			if( y1 > rows ) y1 = rows;
			Graphics gfx = image.getGraphics();
			if( buffer == null ) {
				gfx.setColor( java.awt.Color.BLACK );
				gfx.fillRect( x0, y0, x1 * 8, y1 * 16 );
			} else {
				for( int y = y0; y < y1; y++ ) {
					for( int x = x0; x < x1; x++ ) {
						int chr = buffer[ y ][ x ];
						gfx.setClip( x * 8, y * 16, 8, 16 );
						gfx.drawImage( charset, ( x - ( chr & 0xFF ) + 32 ) * 8,
							( y - ( chr >> 8 ) ) * 16, this );
					}
				}
			}
			gfx.dispose();
		}
	}

	public void paint( Graphics g ) {
		int cw = getWidth();
		int ch = getHeight();
		int bw = getBufferWidth() * 8;
		int bh = getBufferHeight() * 16;
		drawBuffer( pan / 8, 0, ( pan + cw ) / 8, ch / 16 );
		g.drawImage( image, -pan, 0, this );
		g.setColor( java.awt.Color.BLACK );
		if( cw > ( bw - pan ) ) {
			g.fillRect( bw - pan, 0, cw - bw + pan, bh );
		}
		if( ch > bh ) {
			g.fillRect( 0, bh, cw, ch - bh );
		}
	}

	public void update( Graphics g ) {
		paint( g );
	}

	@Override
	public java.awt.Dimension getPreferredSize() {
		return new java.awt.Dimension( getBufferWidth() * 8, getBufferHeight() * 16 );
	}

	private int getBufferWidth() {
		return channels * 11 + 4;
	}

	private int getBufferHeight() {
		return 16;
	}

	public void setPan( int x ) {
		pan = x;
		repaint();
	}

	public int getChannel( int x ) {
		x = x - 32 + pan;
		int channel = x / CHANNEL_WIDTH;
		if( x < 0 || channel >= channels ) {
			channel = -1;
		}
		return channel;
	}

	public void setMuted( int channel, boolean mute ) {
		if( channel < 0 ) {
			for( int idx = 0; idx < channels; idx++ ) {
				muted[ idx ] = mute;
			}
		} else if( channel < channels ) {
			muted[ channel ] = mute;
		}
	}

	public boolean isMuted( int channel ) {
		return muted[ channel ];
	}

	public synchronized void display( ibxm.Module module, int pat, int row ) {
		ibxm.Pattern pattern = module.patterns[ pat ];
		if( buffer == null || module.numChannels != channels ) {
			channels = module.numChannels;
			muted = new boolean[ channels ];
			buffer = new short[ getBufferHeight() ][ getBufferWidth() ];
			if( image != null ) {
				image.flush();
				image = null;
			}
		}
		drawInt( pat, 0, 0, 3, 3 );
		drawChar( ' ', 0, 3, 0 );
		for( int c = 0; c < channels; c++ ) {
			if( muted[ c ] ) {
				drawString( " Muted  ", 0, c * 11 + 4, 3 );
				drawInt( c, 0, c * 11 + 12, 3, 2 );
			} else {
				drawString( "Channel ", 0, c * 11 + 4, 0 );
				drawInt( c, 0, c * 11 + 12, 0, 2 );
			}
			drawString( " ", 0, c * 11 + 14, 0 );
		}
		ibxm.Note note = new ibxm.Note();
		char[] chars = new char[ 10 ];
		for( int y = 1; y < 16; y++ ) {
			int r = row - 8 + y;
			if( r >= 0 && r < pattern.numRows ) {
				int bcol = ( y == 8 ) ? 8 : 0;
				drawInt( r, y, 0, bcol, 3 );
				drawChar( ' ', y, 3, bcol );
				for( int c = 0; c < channels; c++ ) {
					int x = 4 + c * 11;
					pattern.getNote( r * module.numChannels + c, note ).toChars( chars );
					if( muted[ c ] ) {
						for( int idx = 0; idx < 10; idx++ ) {
							drawChar( chars[ idx ], y, x + idx, bcol );
						}
					} else {
						int clr = chars[ 0 ] == '-' ? bcol : bcol + 2;
						for( int idx = 0; idx < 3; idx++ ) {
							drawChar( chars[ idx ], y, x + idx, clr );
						}
						for( int idx = 3; idx < 5; idx++ ) {
							clr = chars[ idx ] == '-' ? bcol : bcol + 3;
							drawChar( chars[ idx ], y, x + idx, clr );
						}
						clr = bcol;
						if( chars[ 5 ] >= '0' && chars[ 5 ] <= 'F' ) {
							clr = bcol + vcclr[ chars[ 5 ] - '0' ];
						}
						drawChar( chars[ 5 ], y, x + 5, clr );
						drawChar( chars[ 6 ], y, x + 6, clr );
						if( chars[ 7 ] == 'E' ) {
							clr = bcol;
							if( chars[ 8 ] >= '0' && chars[ 8 ] <= 'F' ) {
								clr = clr + exclr[ chars[ 8 ] - '0' ];
							}
						} else if( chars[ 7 ] == 's' ) {
							clr = bcol;
							if( chars[ 8 ] >= '0' && chars[ 8 ] <= 'F' ) {
								clr = clr + sxclr[ chars[ 8 ] - '0' ];
							}
						} else {
							clr = bcol;
							if( chars[ 7 ] >= '0' && chars[ 7 ] <= '~' ) {
								clr = clr + fxclr[ chars[ 7 ] - '0' ];
							}
						}
						for( int idx = 7; idx < 10; idx++ ) {
							drawChar( chars[ idx ], y, x + idx, clr );
						}
					}
					drawChar( ' ', y, x + 10, 0 );
				}
			} else {
				drawString( "    ", y, 0, 0 );
				for( int c = 0; c < channels; c++ ) {
					drawString( "           ", y, 4 + c * 11, 0 );
				}
			}
		}
		repaint();
	}

	private void drawInt( int val, int row, int col, int clr, int len ) {
		while( len > 0 ) {
			len = len - 1;
			drawChar( '0' + val % 10, row, col + len, clr );
			val = val / 10;
		}
	}

	private void drawString( String str, int row, int col, int clr ) {
		for( int idx = 0, len = str.length(); idx < len; idx++ ) {
			drawChar( str.charAt( idx ), row, col + idx, clr );
		}
	}

	private void drawChar( int chr, int row, int col, int clr ) {
		buffer[ row ][ col ] = ( short ) ( ( clr << 8 ) | chr );
	}

	private void initCharset() {
		int[] pal = new int[] {
		/*  Blue    Green   Cyan    Red       Magenta   Yellow    White */
			0x00C0, 0x8000, 0x8080, 0x800000, 0x800080, 0x806000, 0x808080, 0x608000,
			0x60FF, 0xFF00, 0xFFFF, 0xFF0000, 0xFF00FF, 0xFFC000, 0xFFFFFF, 0xC0FF00 };
		java.net.URL png = PatternDisplay.class.getResource( "topaz8.png" );
		Image mask = java.awt.Toolkit.getDefaultToolkit().getImage( png );
		charset = createImage( 8 * 96, 16 * pal.length );
		Graphics g = charset.getGraphics();
		for( int r = 0; r < pal.length; r++ ) {
			g.setColor( java.awt.Color.BLACK );
			g.setClip( 0, r * 16, 8, 16 );
			g.fillRect( 0, r * 16, 8, 16 );
			java.awt.Color clr = new java.awt.Color( pal[ r ] );
			for( int c = 1; c < 96; c++ ) {
				g.setClip( c * 8, r * 16, 8, 16 );
				int x = c - ( ( c - 1 ) & 0x1F );
				int y = r - ( ( c - 1 ) >> 5 );
				while( !g.drawImage( mask, x * 8, y * 16, clr, null ) ) {
					try{ Thread.sleep( 10 ); } catch( InterruptedException e ) {}
				}
			}
		}
		g.dispose();
	}
}
