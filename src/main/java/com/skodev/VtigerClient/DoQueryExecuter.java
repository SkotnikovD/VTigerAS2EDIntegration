package com.skodev.VtigerClient;

import com.skodev.exceptions.VtigerInterExc;
import com.vtiger.vtwsclib.WSClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class DoQueryExecuter {

    private static final String productRefbyCodeRequest = "SELECT id FROM Products WHERE productcode LIKE '%s'";
    private static final String contragentByGLNRequest = "SELECT id FROM Accounts WHERE siccode LIKE '%s'";
    private static final String getVendorGLNRequest = "SELECT vendorGLN FROM Vendors WHERE id LIKE '%s'";

//@return Contragent reference in format 'module_id'x'contragent_id'
    public static String getContragentRefByGLN(String GLN, WSClient client) throws VtigerInterExc {
        JSONArray queryResult = client.doQuery(String.format(contragentByGLNRequest, GLN));
        if (client.hasError(queryResult)) {
            throw new VtigerInterExc((String) ((JSONObject) client.lastError()).get("message"));
        } else {
            if (queryResult.size() == 0) {
                throw new VtigerInterExc("No contragent found with GLN '" + GLN + "'");
            } else {
                JSONObject res = (JSONObject) queryResult.get(0);
                return ((String) res.get("id"));
            }
        }
    }

    //@return Product reference in format 'module_id'x'product_id'
    public static String getProductRef(String productCode, WSClient client) throws VtigerInterExc {
        JSONArray queryResult = client.doQuery(String.format(productRefbyCodeRequest, productCode));
        if (client.hasError(queryResult)) {
            throw new VtigerInterExc((String) ((JSONObject) client.lastError()).get("message"));
        } else {
            if (queryResult.size() == 0) {
                throw new VtigerInterExc("No product found with code '" + productCode + "'");
            } else {
                JSONObject res = (JSONObject) queryResult.get(0);
                String sb = (String) res.get("id");
                return sb;
                //return (sb.substring(sb.lastIndexOf("x") + 1));
            }
        }
    }
    
    //@return Vendor's GLN
    public static String getVendorGLN(String vendorRef, WSClient client) throws VtigerInterExc {
        JSONArray queryResult = client.doQuery(String.format(getVendorGLNRequest, vendorRef));
        if (client.hasError(queryResult)) {
            throw new VtigerInterExc((String) ((JSONObject) client.lastError()).get("message"));
        } else {
            if (queryResult.size() == 0) {
                throw new VtigerInterExc("No vendor found with ref = '" + vendorRef + "'");
            } else {
                JSONObject res = (JSONObject) queryResult.get(0);
                String sb = (String) res.get("vendorGLN");
                return sb;
            }
        }
    }
}
