package com.skodev.VtigerController;

import com.skodev.FreeAS2.FreeAS2Communicator;
import com.skodev.VtigerClient.DoQueryExecuter;
import com.skodev.mapping.OutboundMapperInterface;
import com.skodev.VtigerClient.VTigerAPIClient;
import com.skodev.exceptions.VtigerInterExc;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;

public class OutboundController {
    private final VTigerAPIClient vtigerAPI;
    private final Map<String, OutboundMapperInterface> mappersMap;
    private final FreeAS2Communicator as2module;
    
    public OutboundController (VTigerAPIClient vtigerAPI, FreeAS2Communicator communicator) {
        this.vtigerAPI = vtigerAPI;
        this.as2module = communicator;
        mappersMap = new HashMap<>();
    }
    
    public void newOrder(String orderId) throws VtigerInterExc{
        JSONObject res = vtigerAPI.retrieve(orderId);
        String receiverGLN = DoQueryExecuter.getVendorGLN((String)res.get("vendor_id"), vtigerAPI.auth());
        //Choosing appropriate mapper for this exchange
        switch (receiverGLN){
            case "1000000000000":{
                
                break;
            }
        }
    }
    
    public void registerMapper(String recieverGLN, OutboundMapperInterface mapper){
        mappersMap.put(recieverGLN, mapper);
    }
}
