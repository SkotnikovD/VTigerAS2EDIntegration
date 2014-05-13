package com.skodev.vtigerapiuser;

import com.skodev.FreeAS2.FreeAS2Communicator;
import com.skodev.FreeAS2.IncomingMsgsListener;
import com.skodev.REST.ContainerImpl;
import com.skodev.REST.TestContainer;
import com.skodev.exceptions.VtigerInterExc;
import com.vtiger.vtwsclib.WSClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.milyn.edi.unedifact.d01b.D01BInterchangeFactory;
import org.milyn.edi.unedifact.d01b.ORDERS.Orders;
import org.milyn.edi.unedifact.d01b.ORDERS.SegmentGroup28;
import org.milyn.edi.unedifact.d01b.common.IMDItemDescription;
import org.milyn.smooks.edi.unedifact.model.UNEdifactInterchange;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactInterchange41;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactMessage41;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.SocketConnection;
import org.xml.sax.SAXException;

public class Main {

    public static void map() throws IOException, SAXException {
        // Create an instance of the EJC generated factory class... cache this and reuse !!!
        D01BInterchangeFactory factory = D01BInterchangeFactory.getInstance();

        // Deserialize the UN/EDIFACT interchange stream to Java...
        InputStream stream = new FileInputStream("ORDERSexmpl.edi");

        /*------------------------------------------
         Read the interchange to Java Objects...
         -------------------------------------------*/
        UNEdifactInterchange interchange;
        try {
            interchange = factory.fromUNEdifact(stream);

            // Need to test which interchange syntax version.  Supports v4.1 at the moment...
            if (interchange instanceof UNEdifactInterchange41) {
                UNEdifactInterchange41 interchange41 = (UNEdifactInterchange41) interchange;

                System.out.println("\nJava Object Values:");
                System.out.println("\tInterchange Sender ID: " + interchange41.getInterchangeHeader().getSender().getId());

                for (UNEdifactMessage41 messageWithControlSegments : interchange41.getMessages()) {
                    // Process the messages...

                    System.out.println("\tMessage Name: " + messageWithControlSegments.getMessageHeader().getMessageIdentifier().getId());

                    Object messageObj = messageWithControlSegments.getMessage();
                    if (messageObj instanceof Orders) {
                        Orders order = (Orders) messageObj;
                        for (SegmentGroup28 sg28 : order.getSegmentGroup28()) {

                            Iterator it = sg28.getIMDItemDescription().iterator();
                            while (it.hasNext()) {
                                System.out.println("Item: " + ((IMDItemDescription) it.next()).getC273ItemDescription().getE70081ItemDescription());
                            }
                        }
                        System.out.println("\tParty Name: " + order.getSegmentGroup2().get(0).getNADNameAndAddress().getC080PartyName().getE30361PartyName());
                    }
                }
            }
        } finally {
            stream.close();
        }
    }

    public static void doListType() {
        WSClient client = new WSClient("http://vm1.salesplatform.ru/edivendor");
        boolean result = client.doLogin("admin", "QNCadBuxbKKq03E");

        if (result == false) {
            System.out.println("Login failed!");
            System.out.println(client.lastError());
        } else {
            Map types = client.doListTypes();
            Iterator iterator = types.keySet().iterator();
            while (iterator.hasNext()) {
                Object key = iterator.next();
                Map moduleInfo = (Map) types.get(key);
                System.out.println("Module name: " + moduleInfo.get("name") + "key:" + key + "Module info: " + moduleInfo);
            }
        }
    }

    public static void doDescribe(String modName) throws IOException {
        WSClient client = new WSClient("http://vm1.salesplatform.ru/edishop");
        boolean result = client.doLogin("admin", "QNCadBuxbKKq03E");

        if (result == false) {
            System.out.println("Login failed!");
            System.out.println(client.lastError());
        } else {
            JSONObject describeResult = client.doDescribe(modName);
            if (client.hasError(describeResult)) {
                System.out.println("Describe failed!"
                        + client.lastError());
            } else {
                System.out.println(describeResult);
            }
        }
    }

    public static void doQuery() {
        WSClient client = new WSClient("http://vm1.salesplatform.ru/edivendor");
        boolean result = client.doLogin("admin", "QNCadBuxbKKq03E");
        if (result == false) {
            System.out.println("Login failed!");
            System.out.println(client.lastError());
        } else {
            JSONArray queryResult = client.doQuery("SELECT id FROM Products WHERE productcode LIKE 'GTIN0000000001'");
            if (client.hasError(queryResult)) {
                System.out.println("Query failed!" + client.lastError());
            } else {
                System.out.println(queryResult);
                System.out.println("# Result Rows " + queryResult.size());
                System.out.println("# " + client.getResultColumns(queryResult));
                Iterator resultIterator = queryResult.iterator();
                while (resultIterator.hasNext()) {
                    JSONObject row = (JSONObject) resultIterator.next();
                    Iterator rowIterator = row.keySet().iterator();
                    System.out.println("---");
                    while (rowIterator.hasNext()) {
                        Object key = rowIterator.next();
                        Object val = row.get(key);
                        System.out.println("put(\"" + key + " \", \"" + val + "\");");
                    }
                }
            }
        }
    }

    

    public static void doCreate() {
        WSClient client = new WSClient("http://vm1.salesplatform.ru/edivendor");
        boolean result = client.doLogin("admin", "QNCadBuxbKKq03E");
        if (result == false) {
            System.out.println("Login failed!");
            System.out.println(client.lastError());
        } else {
            Map valuesmap = new HashMap();
            valuesmap.put("assigned_user_id", "19x1");
            valuesmap.put("ship_street", "Адрес доставки");
            valuesmap.put("account_id", "3x2");
            valuesmap.put("subject", "Заказ-Тест инф. о продуктах");
            valuesmap.put("sostatus", "Created");
            valuesmap.put("bill_street", "Ю/а улица");
            valuesmap.put("invoicestatus", "AutoCreated");
            valuesmap.put("carrier", "Внутренняя служба перевозки");
            valuesmap.put("hdnDiscountAmount", "3");
            Map inventories = new HashMap();
            valuesmap.put("inventories", inventories);
            ArrayList Products = new ArrayList();
            Map Global = new HashMap();
            inventories.put("Products", Products);
            inventories.put("Global", Global);
            Map product0 = new HashMap();
            Products.add(product0);
            product0.put("hdnProductId", "3");
            product0.put("qty", "3");
            product0.put("listPrice", "35");
            product0.put("discount_percentage", "5");
            product0.put("discount_type", "percentage");
            product0.put("tax1_percentage", "18");
            //Global.put("grandTotal", "7777");
            //Global.put("discount_percentage_final", "3");
            Global.put("shipping_handling_charge", "200");
            Global.put("shtax1_sh_percent", "4");
            Global.put("adjustmentType", "+");
            Global.put("adjustment", "234");
            JSONObject createResult = client.doCreate(
                    "SalesOrder", valuesmap);
            if (client.hasError(createResult)) {
                System.out.println("Create failed!"
                        + client.lastError());
            } else {
                System.out.println(createResult);
            }
        }
    }
    
    public static void testAS2Monitor () throws IOException, Exception{
        class ListenerImpl implements IncomingMsgsListener{

            @Override
            public void newInbound(File msg) {
                System.out.println("New inbound file:" + msg.getPath());
            }
            
        }
        FreeAS2Communicator fc = new FreeAS2Communicator(2000);
        ListenerImpl li = new ListenerImpl();
        fc.registerInboundListener(new File("D:\\Files\\11sem\\Magistr\\Projects\\VtigerAPIUser\\FreeAS2Inter\\from_supplier\\"), 
                new File("D:\\Files\\11sem\\Magistr\\Projects\\VtigerAPIUser\\FreeAS2Inter\\to_supplier\\"), li);
        //fc.sendMsg("Привет мир!", li, "newMsgName");
        
    }

    public static void main(String[] args) throws IOException, SAXException, VtigerInterExc, Exception {
        
//          WSClient client = new WSClient("http://vm1.salesplatform.ru/edivendor");
//          JSONArray queryResult = client.doQuery("SELECT id FROM Products WHERE productcode LIKE 'GTIN0000000001'");
//          System.out.println(queryResult);
//          System.out.println(client.hasError(queryResult));
//          System.out.println(client.lastError());
        
//        HttpServer serv = new HttpServer();
//        serv.addHandlerForResourse(new VtigerRESTHandler(), "VtigerEdiShop/newORDER");
//        serv.start(13007);
        
        
//        Container container = new ContainerImpl();
//        Server server = new ContainerServer(container);
//        SocketConnection connection = new SocketConnection(server);
//        SocketAddress address = new InetSocketAddress(13007);
//        connection.connect(address);
        
        new FreeAS2Communicator(2000).registerInOutDirs();
        
//DoQueryExecuter e = new DoQueryExecuter(Main.auth());
        //System.out.println(e.getProductRef("GTIN0000000001"));
        //Main.map();
       // Main.doDescribe("Vendors");
//Main.doQuery();
        //SalesPlatformAdapter sa = new SalesPlatformAdapter("http://vm1.salesplatform.ru/edivendor", "admin", "QNCadBuxbKKq03E");
        //sa.inboundInterchange(null);
        //Main.doRetrieve();
      //  Main.doCreate();
//Main.doListType();
    }

}
