package com.skodev.VtigerController;

import com.skodev.FreeAS2.IncomingMsgsListener;
import com.skodev.VtigerClient.VTigerAPIClient;
import com.skodev.exceptions.MappingException;
import com.skodev.exceptions.VtigerInterExc;
import com.skodev.mapping.InboundMapperInterface;
import com.skodev.mapping.InboundMapper_edishop2edivendor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.milyn.edi.unedifact.d01b.D01BInterchangeFactory;
import org.milyn.smooks.edi.unedifact.model.UNEdifactInterchange;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactInterchange41;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class InboundController implements IncomingMsgsListener {

    private VTigerAPIClient VTClient;
    private D01BInterchangeFactory factory;
    private Map<String, InboundMapperInterface> inboundMappers;
    private static final Logger log = LoggerFactory.getLogger(InboundController.class);

    public InboundController(VTigerAPIClient VTClient) {
        try {
            factory = D01BInterchangeFactory.getInstance();
            this.VTClient = VTClient;
            inboundMappers = new HashMap();
            this.registerNewMapper("2000000000000", new InboundMapper_edishop2edivendor(VTClient, factory));
        } catch (IOException | SAXException ex) {
            throw new RuntimeException("Can't retrieve Smooks factory for EDI processing.", ex);
        }
    }

    @Override
    public void newInbound(File msg) {
        InputStream msgStream;
        UNEdifactInterchange interchange = null;
        try {
            msgStream = new FileInputStream(msg);
            interchange = factory.fromUNEdifact(msgStream);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        // Need to test which interchange syntax version.  Supports v4.1 at the moment...
        if (interchange instanceof UNEdifactInterchange41) {
            UNEdifactInterchange41 interchange41 = (UNEdifactInterchange41) interchange;
            String senderId = interchange41.getInterchangeHeader().getSender().getId();
            for (Map.Entry<String, InboundMapperInterface> entry : inboundMappers.entrySet()) {
                if (entry.getKey().equals(senderId)) {
                    try {
                        entry.getValue().newInbound(interchange41);
                    } catch (MappingException | VtigerInterExc ex) {
                        log.error("Error into mapping process occured.", ex);
                    }
                    return;
                }
            }
            log.error("No appropriate input mapper for sender {} was found", senderId);
        } else {
            log.error("Incoming msg had non-supported syntax version. Processing failed");
        }
    }

    public final void registerNewMapper(String senderGLN, InboundMapperInterface mapper) {
        this.inboundMappers.put(senderGLN, mapper);
    }
}
