package mcc_2016_g05_p2.niksula.hut.fi.rpc;

import java.io.IOException;
import java.io.InputStream;


/* package */ class ChunkedDecoderInputStream extends InputStream
{
    private final InputStream m_is;
    private int m_chunkRemaining;
    private boolean m_firstChunk;
    private boolean m_ended;

    /* package */ ChunkedDecoderInputStream (InputStream is)
    {
        m_is = is;
        m_chunkRemaining = 0;
        m_firstChunk = true;
        m_ended = false;
    }

    @Override
    public int read() throws IOException
    {
        byte buf[] = new byte[1];
        int numRead = read(buf);
        if (numRead == -1)
            return -1;
        return buf[0];
    }

    @Override
    public int read (byte buf[]) throws IOException
    {
        return read(buf, 0, buf.length);
    }

    @Override
    public int read (byte buf[], int offset, int length) throws IOException
    {
        if (length == 0)
            return 0;
        if (m_ended)
            return -1;

        if (m_chunkRemaining > 0)
        {
            int maxRead = Math.min(length, m_chunkRemaining);
            int numRead = m_is.read(buf, offset, maxRead);
            if (numRead < 0)
                return killme();
            m_chunkRemaining -= numRead;
            return numRead;
        }

        if (!m_firstChunk)
        {
            if (m_is.read() != '\r')
                return killme();
            if (m_is.read() != '\n')
                return killme();
        }
        m_firstChunk = false;

        // Read chunk len
        StringBuilder lengthFieldBuf = new StringBuilder();
        for (;;)
        {
            int letter = m_is.read();
            if (letter == -1)
                return killme();
            if (letter == ';' || letter == '\r')
                break;
            boolean isNumber = (letter >= '0' && letter <= '9');
            boolean isHexLetter = (letter >= 'A' && letter <= 'F') || (letter >= 'a' && letter <= 'f');
            if (!isNumber && !isHexLetter)
                return killme();
            lengthFieldBuf.append(Character.toLowerCase((char)letter));
        }
        for (;;)
        {
            int letter = m_is.read();
            if (letter == -1)
                return killme();
            if (letter == '\n')
                break;
        }

        try
        {
            m_chunkRemaining = Integer.parseInt(lengthFieldBuf.toString(), 16);
        }
        catch (NumberFormatException ex)
        {
            return killme();
        }

        if (m_chunkRemaining <= 0)
            return killme();
        return read(buf, offset, length);
    }

    private int killme()
    {
        m_ended = true;
        return -1;
    }
}
