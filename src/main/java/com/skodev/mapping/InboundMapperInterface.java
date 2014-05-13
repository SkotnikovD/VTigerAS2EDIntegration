package com.skodev.mapping;

import com.skodev.exceptions.MappingException;
import java.io.InputStream;

public interface InboundMapperInterface {
    public void newInbound(InputStream stream) throws MappingException;
}
