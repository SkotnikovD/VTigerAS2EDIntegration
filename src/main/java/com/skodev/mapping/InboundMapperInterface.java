package com.skodev.mapping;

import com.skodev.exceptions.MappingException;
import com.skodev.exceptions.VtigerInterExc;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactInterchange41;

public interface InboundMapperInterface {
    public void newInbound(UNEdifactInterchange41 interchange) throws MappingException, VtigerInterExc;
}
