package com.skodev.mapping;

import com.skodev.VtigerClient.DoQueryExecuter;
import com.skodev.VtigerClient.VTigerAPIClient;
import com.skodev.exceptions.MappingException;
import com.skodev.exceptions.VtigerInterExc;
import com.vtiger.vtwsclib.WSClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.simple.JSONObject;
import org.milyn.edi.unedifact.d01b.D01BInterchangeFactory;
import org.milyn.edi.unedifact.d01b.ORDERS.Orders;
import org.milyn.edi.unedifact.d01b.ORDERS.SegmentGroup1;
import org.milyn.edi.unedifact.d01b.ORDERS.SegmentGroup2;
import org.milyn.edi.unedifact.d01b.ORDERS.SegmentGroup28;
import org.milyn.edi.unedifact.d01b.common.DTMDateTimePeriod;
import org.milyn.edi.unedifact.d01b.common.MOAMonetaryAmount;
import org.milyn.edi.unedifact.d01b.common.PIAAdditionalProductId;
import org.milyn.edi.unedifact.d01b.common.QTYQuantity;
import org.milyn.edi.unedifact.d01b.common.field.C507DateTimePeriod;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactInterchange41;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactMessage41;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InboundMapper_edishop2edivendor implements InboundMapperInterface {

    private VTigerAPIClient vtapi;
    private static Logger log = LoggerFactory.getLogger(InboundMapper_edishop2edivendor.class.getClass());
    // Create an instance of the EJC generated factory class... cache this and reuse !!!
    private final D01BInterchangeFactory factory;

    public InboundMapper_edishop2edivendor(VTigerAPIClient vtapi, D01BInterchangeFactory factory) {
        this.vtapi = vtapi;
        this.factory = factory;
    }

    @Override
    public void newInbound(UNEdifactInterchange41 interchange) throws MappingException, VtigerInterExc {
        log.info("Inbound controller starts to process new interchange '{}'", interchange.getInterchangeHeader().getControlRef());
        for (UNEdifactMessage41 messageWithControlSegments : interchange.getMessages()) {
            // BT: Message number
            log.info("Start processing message '{}'", messageWithControlSegments.getMessageHeader().getMessageRefNum());

            Object messageObj = messageWithControlSegments.getMessage();
            if (messageObj instanceof Orders) {
                this.inboundORDERS((Orders) messageObj);
            } else {
                throw new MappingException("Inbound mapper for partner " + interchange.getInterchangeHeader().getSender().getId()
                        + " doesn't support " + messageWithControlSegments.getMessageHeader().getMessageIdentifier().getTypeSubFunctionId()
                        + " message type. Processing aborted.");
            }
        }
        log.info("Inbound controller has finished to process interchange '{}'", interchange.getInterchangeHeader().getControlRef());
    }

    //"BT:" comments - explanation of business meaning (Business Term)
    private void inboundORDERS(Orders ordersMsg) throws MappingException, VtigerInterExc {
        //Create structure for execute doCreate request
        log.info("Mapper {} start to process ORDER msg number {} ", this.getClass().toString(), ordersMsg.getBGMBeginningOfMessage().getC106DocumentMessageIdentification().getE1004DocumentIdentifier());
        Map JSON = new HashMap();
        Map inventories = new HashMap();
        ArrayList<Map> products = new ArrayList<>();
        Map global = new HashMap();
        inventories.put("Products", products);
        inventories.put("Global", global);
        JSON.put("inventories", inventories);

        //BT: Order number --maps-- Заказ на закупку
        JSON.put("vtiger_purchaseorder", ordersMsg.getBGMBeginningOfMessage().getC106DocumentMessageIdentification().getE1004DocumentIdentifier());

        for (DTMDateTimePeriod DTM : ordersMsg.getDTMDateTimePeriod()) {
            C507DateTimePeriod date = DTM.getC507DateTimePeriod();
            if (date.getE2005DateOrTimeOrPeriodFunctionCodeQualifier().equals("137")) {
                //BT: this is message date
                date.getE2380DateOrTimeOrPeriodValue();
                //in format 102 = CCYYMMDD or 203 = CCYYMMDDHHMM
                date.getE2379DateOrTimeOrPeriodFormatCode();
            } else if (date.getE2005DateOrTimeOrPeriodFunctionCodeQualifier().equals("2")) {
                //BT: this is delivery date
                date.getE2380DateOrTimeOrPeriodValue();
                //in format 102 = CCYYMMDD or 203 = CCYYMMDDHHMM
                date.getE2379DateOrTimeOrPeriodFormatCode();
            }
        }

        Iterator<SegmentGroup1> isg1 = ordersMsg.getSegmentGroup1().iterator();
        while (isg1.hasNext()) {
            SegmentGroup1 sg1 = isg1.next();
            switch (sg1.getRFFReference().getC506Reference().getE1153ReferenceCodeQualifier()) {
                case "CT":
                    //BT: Contract number
                    sg1.getRFFReference().getC506Reference().getE1154ReferenceIdentifier();
                    break;
                case "PD":
                    //BT: Promotion deal number
                    sg1.getRFFReference().getC506Reference().getE1154ReferenceIdentifier();
                    break;
            }
        }

        // BT: Partys' ids
        JSONObject contrObj = null;
        if (ordersMsg.getSegmentGroup2() == null) {
            throw new MappingException("Missing Segment2 group. Segment2 group is Required in ECR-Rus specs");
        }
        Iterator<SegmentGroup2> isg2 = ordersMsg.getSegmentGroup2().iterator();
        boolean BYFlag = false;
        while (isg2.hasNext()) {
            SegmentGroup2 sg2 = isg2.next();
            switch (sg2.getNADNameAndAddress().getE3035PartyFunctionCodeQualifier()) {
                case "SU":
                    //BT: Supplyer's ID
                    break;
                case "BY":
                    //BT: Buyer's ID
                    BYFlag = true;
                    String contrRef = DoQueryExecuter.getContragentRefByGLN(sg2.getNADNameAndAddress().getC082PartyIdentificationDetails().getE3039PartyIdentifier(), vtapi.auth());
                    JSON.put("account_id", contrRef);
                    //Complete buyer's adress fields
                    contrObj = vtapi.retrieve(contrRef);
                    //Юредический адрес
                    JSON.put("bill_street", contrObj.get("bill_street"));
                    JSON.put("bill_city", contrObj.get("bill_city"));
                    JSON.put("bill_state", contrObj.get("bill_state"));
                    JSON.put("bill_code", contrObj.get("bill_code"));
                    JSON.put("bill_country", contrObj.get("bill_country"));
                    JSON.put("bill_pobox", contrObj.get("bill_pobox"));
                    //Фактический адрес
                    JSON.put("ship_street", contrObj.get("ship_street"));
                    JSON.put("ship_city", contrObj.get("ship_city"));
                    JSON.put("ship_state", contrObj.get("ship_state"));
                    JSON.put("ship_code", contrObj.get("ship_code"));
                    JSON.put("ship_country", contrObj.get("ship_country"));
                    JSON.put("ship_pobox", contrObj.get("ship_pobox"));
                    break;
                case "DP":
                    //BT: Delivery party ID
                    break;
                case "IV":
                    //BT: Invoicee ID
                    break;
                default: //TODO Log nonsupported qualifier found 
            }
        }
        if (!BYFlag) {
            throw new MappingException("Missing Buyers id information into Segment2 group.");
        }

        //BT: Items section
        if (ordersMsg.getSegmentGroup28() == null) {
            throw new MappingException("Missing Segment28 group. Segment28 group is Required in ECR-Rus specs");
        }
        Iterator<SegmentGroup28> isg28 = ordersMsg.getSegmentGroup28().iterator();
        int itemsCntr = 0;
        while (isg28.hasNext()) {
            SegmentGroup28 sg28 = isg28.next();
            JSONObject itemObj;
            Map item = new HashMap();
            //Number of item's line 
            sg28.getLINLineItem().getE1082LineItemIdentifier();
            if (sg28.getLINLineItem().getC212ItemNumberIdentification().getE7143ItemTypeIdentificationCode().equals("SRV")) {
                //BT: GTIN codes in use
                //This is item's GTIN
                String itemRef = DoQueryExecuter.getProductRef(sg28.getLINLineItem().getC212ItemNumberIdentification().getE7140ItemIdentifier(), vtapi.auth());
                itemObj = vtapi.retrieve(itemRef);
                item.put("hdnProductCode", itemObj.get("productcode"));
                item.put("hdnProductId", itemRef.substring(itemRef.lastIndexOf("x") + 1));
                //item.put("productDescription",itemObj.get("description"));
                products.add(item);
            } else {
                throw new MappingException("Only SRV Item Type Identification code suppoted at ECR-Rus specs");
            }

            Iterator<PIAAdditionalProductId> PIAlist = sg28.getPIAAdditionalProductId().iterator();
            while (PIAlist.hasNext()) {
                PIAAdditionalProductId PIA = PIAlist.next();
                switch (PIA.getC2121ItemNumberIdentification().getE7143ItemTypeIdentificationCode()) {
                    case ("IN"):
                        //BT: Supplierss article number
                        PIA.getC2121ItemNumberIdentification().getE7140ItemIdentifier();
                        break;
                    case ("SA"):
                        //BT: Buyer's article number
                        PIA.getC2121ItemNumberIdentification().getE7140ItemIdentifier();
                        break;
                }
            }

            //TODO: IMD
            Iterator<QTYQuantity> QTYlist = sg28.getQTYQuantity().iterator();
            while (QTYlist.hasNext()) {
                QTYQuantity QTY = QTYlist.next();
                switch (QTY.getC186QuantityDetails().getE6063QuantityTypeCodeQualifier()) {
                    case ("21"):
                        //BT: Ordered quantity
                        item.put("qty", QTY.getC186QuantityDetails().getE6060Quantity());
                        break;
                    case ("59"):
                        //BT: Number of consumer units in the traded unit
                        break;
                }
            }

            //BT: group tax count
            global.put("TaxType", "group");
            JSON.put("hdnTaxType", "group");

            //BT: Value of order line
            Iterator<MOAMonetaryAmount> MOAlist = sg28.getMOAMonetaryAmount().iterator();
            while (MOAlist.hasNext()) {
                MOAMonetaryAmount MOA = MOAlist.next();
                switch (MOA.getC516MonetaryAmount().getE5025MonetaryAmountTypeCodeQualifier()) {
                    case ("128"):
                        //BT: Total amount of item line
                        //item.put("productTotal", MOA.getC516MonetaryAmount().getE5004MonetaryAmount());
                        break;
                    case ("203"):
                        //BT: Goods item total - allowances + charge
                        //item.put("totalAfterDiscount", MOA.getC516MonetaryAmount().getE5004MonetaryAmount());
                        //item.put("netPrice", MOA.getC516MonetaryAmount().getE5004MonetaryAmount());
                        break;
                }

                //Скидка
                //НДС
            }

            //BT: Price of order line
            if(sg28.getSegmentGroup32().get(0).getPRIPriceDetails().getC509PriceInformation().getE5125PriceCodeQualifier().equals("AAB")){
                //BT: Item's Gross price
                item.put("listPrice", sg28.getSegmentGroup32().get(0).getPRIPriceDetails().getC509PriceInformation().getE5118PriceAmount());
            }
            //item.put("listPrice", sg28.getSegmentGroup32().get(0).getPRIPriceDetails().getC509PriceInformation().getE5118PriceAmount());

        }
        //BT: Value of all ordered lines
        if (ordersMsg.getMOAMonetaryAmount().size() > 0) {
            if (ordersMsg.getMOAMonetaryAmount().get(0).getC516MonetaryAmount().getE5025MonetaryAmountTypeCodeQualifier().equals("86")) {
                //global.put("grandTotal", ordersMsg.getMOAMonetaryAmount().get(0).getC516MonetaryAmount().getE5004MonetaryAmount());
                //JSON.put("ndnGrandTotal", ordersMsg.getMOAMonetaryAmount().get(0).getC516MonetaryAmount().getE5004MonetaryAmount());
            }

        }
        JSON.put("sostatus", "Created");
        JSON.put("invoicestatus", "AutoCreated");
        JSON.put("subject", contrObj.get("accountname") + "_" + ordersMsg.getBGMBeginningOfMessage().getC106DocumentMessageIdentification().getE1004DocumentIdentifier());
        log.info("Mapping of ORDERS msg {} completed. Trying to send msg to VTiger... ", ordersMsg.getBGMBeginningOfMessage().getC106DocumentMessageIdentification().getE1004DocumentIdentifier());
        vtapi.newORDERS(JSON);
        log.info("ORDERS msg {} added to VTiger CRM successfully ", ordersMsg.getBGMBeginningOfMessage().getC106DocumentMessageIdentification().getE1004DocumentIdentifier());
    }

}
