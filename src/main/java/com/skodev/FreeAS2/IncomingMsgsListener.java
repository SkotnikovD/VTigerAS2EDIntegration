package com.skodev.FreeAS2;

import java.io.File;

public interface IncomingMsgsListener {
    public void newInbound(File msg);

    @Override
    public boolean equals(Object obj);

    @Override
    public int hashCode();
    
}
