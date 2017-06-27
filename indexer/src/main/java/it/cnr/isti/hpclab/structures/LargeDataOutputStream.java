package it.cnr.isti.hpclab.structures;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LargeDataOutputStream extends OutputStream implements DataOutput 
{
    /**
     * The number of bytes written to the data output stream so far.
     * If this counter overflows, it will be wrapped to Long.MAX_VALUE.
     */
    protected long written;
    
    protected DataOutputStream dos;

    public LargeDataOutputStream(OutputStream out) 
    {
    	written = 0;
    	dos = new DataOutputStream(out);
    }

    /**
     * Increases the written counter by the specified value
     * until it reaches Long.MAX_VALUE.
     */
    private void incCount(int value) 
    {
        long temp = written + value;
        if (temp < 0) {
            temp = Long.MAX_VALUE;
        }
        written = temp;
    }

    
    @Override
    public synchronized void write(int b) throws IOException 
    {
        dos.write(b);
        incCount(Byte.BYTES);
    }

    @Override
    public synchronized void write(byte b[], int off, int len) throws IOException
    {
        dos.write(b, off, len);
        incCount(len);
    }

    @Override
    public void flush() throws IOException 
    {
        dos.flush();
    }

    @Override
    public final void writeBoolean(boolean v) throws IOException 
    {
        dos.write(v ? 1 : 0);
        incCount(Byte.BYTES);
    }

    @Override
    public final void writeByte(int v) throws IOException 
    {
        dos.write(v);
        incCount(Byte.BYTES);
    }

    @Override
    public final void writeShort(int v) throws IOException 
    {
    	dos.writeShort(v);
        incCount(Short.BYTES);
    }

    @Override
    public final void writeChar(int v) throws IOException 
    {        
    	dos.writeChar(v);
        incCount(Character.BYTES);
    }

    @Override
    public final void writeInt(int v) throws IOException 
    {
        dos.writeInt(v);
        incCount(Integer.BYTES);
    }

    @Override
    public final void writeLong(long v) throws IOException 
    {        
    	dos.writeLong(v);
        incCount(Long.BYTES);
    }

    @Override
    public final void writeFloat(float v) throws IOException 
    {
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    public final void writeDouble(double v) throws IOException 
    {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    public final void writeBytes(String s) throws IOException 
    {
        dos.writeBytes(s);
        incCount(s.length());
    }

    @Override
    public final void writeChars(String s) throws IOException 
    {
        dos.writeChars(s);
        incCount(s.length() * Character.BYTES);
    }

    @Override
    public final void writeUTF(String str) throws IOException 
    {    
    	int strlen = str.length();
        int utflen = 0;
        int c = 0;
        
        /* use charAt instead of copying String to char array */
        for (int i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }
    	
    	dos.writeUTF(str);
    	
    	incCount(utflen + Short.BYTES);
    }

    public final long size() 
    {
        return written;
    }

	@Override
	public void close() throws IOException 
	{
		dos.close();
	}

	@Override
	public void write(byte[] b) throws IOException 
	{
		write(b, 0, b.length);
	}
}