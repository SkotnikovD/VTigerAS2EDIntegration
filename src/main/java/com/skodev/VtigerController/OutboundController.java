package com.skodev.VtigerController;

import com.skodev.FreeAS2.FreeAS2Communicator;
import com.skodev.VtigerClient.DoQueryExecuter;
import com.skodev.VtigerClient.VTigerAPIClient;
import com.skodev.exceptions.AS2Exception;
import com.skodev.exceptions.MappingException;
import com.skodev.exceptions.VtigerInterExc;
import com.skodev.mapping.OutboundMapperInterface;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboundController {

    private final VTigerAPIClient vtigerAPI;
    private final Map<String, OutboundMapperInterface> mappersMap;
    private final FreeAS2Communicator as2module;
    private static final Logger log = LoggerFactory.getLogger(OutboundController.class);

    public OutboundController(VTigerAPIClient vtigerAPI, FreeAS2Communicator communicator) {
        this.vtigerAPI = vtigerAPI;
        this.as2module = communicator;
        mappersMap = new HashMap<>();
    }

    public void newOrder(String orderId) throws VtigerInterExc, MappingException, AS2Exception {
        JSONObject res = vtigerAPI.retrieve(orderId);
        String venId = (String) res.get("vendor_id");
        JSONObject resVen = vtigerAPI.retrieve(venId);
        String receiverGLN = (String) resVen.get("description");
        System.out.println("Vendor gln " + receiverGLN);
        //String receiverGLN = DoQueryExecuter.getVendorGLN(venId, vtigerAPI.auth());
        //Choosing appropriate mapper for this exchange
        OutboundMapperInterface mapper = mappersMap.get(receiverGLN);
        if (mapper == null) {
            log.error("No inbound mapper has been found for sender with GLN {}.", receiverGLN);
        } else {
            File f = mapper.newPurchaseOrder(orderId);
            as2module.sendMsg(f, receiverGLN);
        }
    }

    public void registerMapper(String recieverGLN, OutboundMapperInterface mapper) {
        mappersMap.put(recieverGLN, mapper);
    }
}
