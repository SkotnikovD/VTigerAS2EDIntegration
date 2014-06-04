package com.skodev.VtigerClient;

import com.skodev.exceptions.VtigerInterExc;
import com.vtiger.vtwsclib.WSClient;
import java.util.Map;
import org.json.simple.JSONObject;
import org.slf4j.LoggerFactory;

public class VTigerAPIClient implements InternalSystemAPI {

    private final WSClient client;
    private final String APIKey;
    private final String login;
    private static final Object lock = new Object();

    public VTigerAPIClient(String urlToCRM, String login, String APIKey) {
        this.client = new WSClient(urlToCRM);
        this.APIKey = APIKey;
        this.login = login;
    }

    public WSClient auth() throws VtigerInterExc {
        synchronized (lock) {
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
    }

    public JSONObject retrieve(String entityId) throws VtigerInterExc {
        synchronized (lock) {
            this.auth();
            JSONObject resObj = client.doRetrieve(entityId);
            if (client.hasError(resObj)) {
                throw new VtigerInterExc("Error while retrieve entity with id = " + entityId + ". Reason: " + client.lastError().toString());
            } else {
                return resObj;
            }
        }
    }

    public synchronized JSONObject create(String moduleName, Map object) throws VtigerInterExc {
        synchronized (lock) {
            this.auth();
            JSONObject createResult = client.doCreate(moduleName, object);
            if (client.hasError(createResult)) {
                throw new VtigerInterExc("Error while creating entity into " + moduleName + " module. Reason: " + client.lastError().toString());
            } else {
                return createResult;
            }
        }
    }

    @Override
    public void newORDERS(Map obj) throws VtigerInterExc {
        this.create("SalesOrder", obj);
    }

}
