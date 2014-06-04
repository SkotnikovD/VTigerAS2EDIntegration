package com.skodev.vtigerapiuser;

import com.skodev.ConfigReader.ConfigReader;
import com.skodev.FreeAS2.FreeAS2Communicator;
import com.skodev.REST.ContainerImpl;
import com.skodev.REST.HttpServer;
import com.skodev.VtigerClient.DoQueryExecuter;
import com.skodev.VtigerClient.VTigerAPIClient;
import com.skodev.VtigerController.InboundController;
import com.skodev.VtigerController.OutboundController;
import com.skodev.exceptions.AS2Exception;
import com.skodev.exceptions.MappingException;
import com.skodev.exceptions.VtigerInterExc;
import com.skodev.mapping.OutboundMapper_edivendor2edishop;
import com.vtiger.vtwsclib.WSClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.milyn.edi.unedifact.d01b.D01BInterchangeFactory;
import org.milyn.edi.unedifact.d01b.ORDERS.Orders;
import org.milyn.edi.unedifact.d01b.ORDERS.SegmentGroup28;
import org.milyn.edi.unedifact.d01b.common.IMDItemDescription;
import org.milyn.smooks.edi.unedifact.model.UNEdifactInterchange;
import org.milyn.smooks.edi.unedifact.model.r41.UNB41;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactInterchange41;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactMessage41;
import org.milyn.smooks.edi.unedifact.model.r41.UNH41;
import org.milyn.smooks.edi.unedifact.model.r41.UNT41;
import org.milyn.smooks.edi.unedifact.model.r41.UNZ41;
import org.milyn.smooks.edi.unedifact.model.r41.types.Party;
import org.simpleframework.http.core.Container;
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
    
    public static void testInbound(){
        VTigerAPIClient vtcl = new VTigerAPIClient("http://vm1.salesplatform.ru/edivendor", "admin", "QNCadBuxbKKq03E");
        InboundController  ic=  new InboundController(vtcl);
        ic.newInbound(new File("D:\\Files\\11sem\\Magistr\\Projects\\VTigerAS2EDIntegration\\ORDERSexmpl.edi"));
    }
    
    public static void testEDIOut() throws IOException, SAXException{
        D01BInterchangeFactory f = D01BInterchangeFactory.getInstance();
        UNEdifactInterchange41 inter = new UNEdifactInterchange41();
        UNEdifactMessage41 msg = new UNEdifactMessage41();
        LinkedList<UNEdifactMessage41> msgList = new LinkedList<UNEdifactMessage41>();
        msgList.add(msg);
        inter.setMessages(msgList);
        inter.setInterchangeHeader(new UNB41());
        inter.getInterchangeHeader().setRecipient(new Party());
        inter.getInterchangeHeader().getRecipient().setId("12345676");
        inter.setInterchangeTrailer(new UNZ41());
        msg.setMessageHeader(new UNH41());
        msg.getMessageHeader().setMessageRefNum("777777");
        msg.setMessageTrailer(new UNT41());
        msg.setMessage(new Orders());
        inter.getInterchangeTrailer().setControlCount(2);
        
        
        
        //inter.setInterchangeHeader(new UNB41().setControlRef("123456789"));
        
        File out = new File("D:\\Files\\11sem\\Magistr\\Projects\\VTigerAS2EDIntegration\\testOut.edi");
        out.createNewFile();
        FileWriter fw = new FileWriter(out);
        inter.write(fw);
        //f.toUNEdifact(inter, fw);
    }
    
    public static void testOutbound() throws AS2Exception, IOException, SAXException, VtigerInterExc, MappingException{
        VTigerAPIClient vtcl = new VTigerAPIClient("http://vm1.salesplatform.ru/edishop", "admin", "QNCadBuxbKKq03E");
        InboundController  ic =  new InboundController(vtcl);
        FreeAS2Communicator AS2Com = new FreeAS2Communicator(2000, ic);
        OutboundController oc = new OutboundController(vtcl, AS2Com);
        oc.registerMapper("1000000000000", new OutboundMapper_edivendor2edishop(vtcl, D01BInterchangeFactory.getInstance()));
       // Container c = new ContainerImpl(oc);
       // HttpServer serv = new HttpServer(c);
        //serv.start(13007);
        oc.newOrder("14x12");
    }
    
    public static void main(String[] args) throws IOException, SAXException, VtigerInterExc, Exception {
     //   VTigerAPIClient vtcl = new VTigerAPIClient("http://vm1.salesplatform.ru/edishop", "admin", "QNCadBuxbKKq03E");
      //  System.out.println(vtcl.retrieve("14x12"));
//System.out.println(vtcl.retrieve("11x4"));
//        InboundController  ic=  new InboundController(vtcl);
//        FreeAS2Communicator AS2Com = new FreeAS2Communicator(2000, ic);
//        
//        OutboundController oc = new OutboundController(vtcl, AS2Com);
       // testInbound();
        //testEDIOut();
        
      testOutbound();
        //VTigerAPIClient vtcl = new VTigerAPIClient("http://vm1.salesplatform.ru/edishop", "admin", "QNCadBuxbKKq03E");
        //System.out.println(DoQueryExecuter.getVendorGLN("11x4", vtcl.auth()));
        //doDescribe("Vendors");
//        
   // FileUtils.deleteDirectory(new File("D:\\Files\\11sem\\Magistr\\Projects\\VTigerAS2EDIntegration\\FreeAS2Inter\\EdiShop\\In\\processed"));
    }

}
