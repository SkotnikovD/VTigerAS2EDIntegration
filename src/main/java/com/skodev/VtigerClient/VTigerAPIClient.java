package com.skodev.VtigerClient;

import com.skodev.exceptions.VtigerInterExc;
import com.vtiger.vtwsclib.WSClient;
import java.util.Map;
import org.json.simple.JSONObject;
import org.slf4j.LoggerFactory;

public class VTigerAPIClient {

    private final WSClient client;
    private final String APIKey;
    private final String login;

    private DoQueryExecuter queryExecuter;

    public VTigerAPIClient(String urlToCRM, String login, String APIKey) {
        this.client = new WSClient(urlToCRM);
        this.APIKey = APIKey;
        this.login = login;
    }

    public WSClient auth() throws VtigerInterExc {
        boolean result = false;
        int i = 0;
        while (result == false && i < 3) {
            result = client.doLogin(login, APIKey);
            i++;
        }
        if (result == false) {
            LoggerFactory.getLogger(this.getClass()).warn("Authorise for {} has failed. Reason: {}", login, client.lastError());
            throw new VtigerInterExc("Authorisation has failed!");
        }
        return this.client;
    }

    public JSONObject retrieve(String entityId) throws VtigerInterExc {
        this.auth();
        JSONObject resObj = client.doRetrieve(entityId);
        if (client.hasError(resObj)){
            throw new VtigerInterExc("Error while retrieve entity with id = "+ entityId +". Reason: "+ client.lastError().toString());
        } else{
            return resObj;
        }
    }
    
    public JSONObject create (String moduleName, Map object) throws VtigerInterExc{
        this.auth();
        JSONObject createResult = client.doCreate("SalesOrder", object);
        if (client.hasError(createResult)){
            throw new VtigerInterExc("Error while creating entity into "+ moduleName +" module. Reason: "+ client.lastError().toString());
        } else{
            return createResult;
        }
    }

}
