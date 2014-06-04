package com.skodev.mapping;

import com.skodev.ConfigReader.ConfigReader;
import com.skodev.VtigerClient.VTigerAPIClient;
import com.skodev.exceptions.MappingException;
import com.skodev.exceptions.VtigerInterExc;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.milyn.edi.unedifact.d01b.D01BInterchangeFactory;
import org.milyn.edi.unedifact.d01b.ORDERS.Orders;
import org.milyn.edi.unedifact.d01b.ORDERS.SegmentGroup2;
import org.milyn.edi.unedifact.d01b.ORDERS.SegmentGroup28;
import org.milyn.edi.unedifact.d01b.ORDERS.SegmentGroup32;
import org.milyn.edi.unedifact.d01b.ORDERS.SegmentGroup7;
import org.milyn.edi.unedifact.d01b.common.BGMBeginningOfMessage;
import org.milyn.edi.unedifact.d01b.common.CUXCurrencies;
import org.milyn.edi.unedifact.d01b.common.DTMDateTimePeriod;
import org.milyn.edi.unedifact.d01b.common.LINLineItem;
import org.milyn.edi.unedifact.d01b.common.NADNameAndAddress;
import org.milyn.edi.unedifact.d01b.common.PRIPriceDetails;
import org.milyn.edi.unedifact.d01b.common.QTYQuantity;
import org.milyn.edi.unedifact.d01b.common.Uns;
import org.milyn.edi.unedifact.d01b.common.field.C002DocumentMessageName;
import org.milyn.edi.unedifact.d01b.common.field.C082PartyIdentificationDetails;
import org.milyn.edi.unedifact.d01b.common.field.C106DocumentMessageIdentification;
import org.milyn.edi.unedifact.d01b.common.field.C186QuantityDetails;
import org.milyn.edi.unedifact.d01b.common.field.C212ItemNumberIdentification;
import org.milyn.edi.unedifact.d01b.common.field.C5041CurrencyDetails;
import org.milyn.edi.unedifact.d01b.common.field.C507DateTimePeriod;
import org.milyn.edi.unedifact.d01b.common.field.C509PriceInformation;
import org.milyn.smooks.edi.unedifact.model.r41.UNB41;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactInterchange41;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactMessage41;
import org.milyn.smooks.edi.unedifact.model.r41.UNH41;
import org.milyn.smooks.edi.unedifact.model.r41.UNT41;
import org.milyn.smooks.edi.unedifact.model.r41.UNZ41;
import org.milyn.smooks.edi.unedifact.model.r41.types.DateTime;
import org.milyn.smooks.edi.unedifact.model.r41.types.MessageIdentifier;
import org.milyn.smooks.edi.unedifact.model.r41.types.Party;
import org.milyn.smooks.edi.unedifact.model.r41.types.SyntaxIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboundMapper_edivendor2edishop implements OutboundMapperInterface {

    private final VTigerAPIClient vtapi;
    private static final Logger log = LoggerFactory.getLogger(OutboundMapper_edivendor2edishop.class);
    private final D01BInterchangeFactory factory;

    public OutboundMapper_edivendor2edishop(VTigerAPIClient vtapi, D01BInterchangeFactory factory) {
        this.vtapi = vtapi;
        this.factory = factory;
    }

    //"BT:" comments - explanation of business meaning (Business Term)
    @Override
    public File newPurchaseOrder(String id) throws MappingException, VtigerInterExc {
        log.info("Mapper {} start to process purchase order with id {} ", this.getClass().toString(), id);
        //creating blank interchange and message objects
        UNEdifactInterchange41 i = new UNEdifactInterchange41();
        //Set interchange's message list
        List<UNEdifactMessage41> msgList = new LinkedList<>();
        i.setMessages(msgList);
        //Add nem message to message list
        UNEdifactMessage41 orderMsg = new UNEdifactMessage41();
        msgList.add(orderMsg);

        //Retrieving purchase order object from CRM
        JSONObject POObj = vtapi.retrieve(id);
        JSONObject supplier = vtapi.retrieve((String) POObj.get("vendor_id"));
        JSONObject inventorise = (JSONObject) POObj.get("inventories");
        JSONObject global = (JSONObject) inventorise.get("Global");
        JSONArray products = (JSONArray) inventorise.get("Products");
        
        String vendorGLN = (String) supplier.get("description");

        //UNB seg filling
        UNB41 hdr = new UNB41();
        i.setInterchangeHeader(hdr);
        hdr.setSyntaxIdentifier(new SyntaxIdentifier());
        hdr.getSyntaxIdentifier().setCodedCharacterEncoding("UNOA");
        hdr.getSyntaxIdentifier().setServiceCodeListDirVersion("2");

        hdr.setSender(new Party());
        hdr.getSender().setId(ConfigReader.getMyGLN());
        hdr.getSender().setCodeQualifier("14");
        hdr.setRecipient(new Party());
        hdr.getRecipient().setId(vendorGLN);
        hdr.getRecipient().setCodeQualifier("14");

        hdr.setDate(new DateTime());
        Date curDate = new Date();
        SimpleDateFormat date = new SimpleDateFormat("yyMMdd");
        SimpleDateFormat time = new SimpleDateFormat("HHmm");
        hdr.getDate().setDate(date.format(curDate));
        hdr.getDate().setTime(time.format(curDate));
        SimpleDateFormat ref = new SimpleDateFormat("yyMMddHHmmssSS");
        hdr.setControlRef(ref.format(curDate));

        int segCntr = 0;

        //Set message header
        segCntr++;
        int msgRefNum = 1;
        UNH41 mh = new UNH41();
        mh.setMessageRefNum(String.valueOf(msgRefNum));
        MessageIdentifier mi = new MessageIdentifier();
        mi.setId("ORDERS");
        mi.setVersionNum("D");
        mi.setReleaseNum("01B");
        mi.setControllingAgencyCode("UN");
        mi.setAssociationAssignedCode("EAN010");
        mh.setMessageIdentifier(mi);
        orderMsg.setMessageHeader(mh);

        Orders ordr = new Orders();
        orderMsg.setMessage(ordr);

        //BGM
        segCntr++;
        BGMBeginningOfMessage BGM = new BGMBeginningOfMessage();
        BGM.setC002DocumentMessageName(new C002DocumentMessageName());
        BGM.getC002DocumentMessageName().setE1001DocumentNameCode("220");
        BGM.setC106DocumentMessageIdentification(new C106DocumentMessageIdentification());
        BGM.getC106DocumentMessageIdentification().setE1004DocumentIdentifier((String) POObj.get("purchaseorder_no"));
        BGM.setE1225MessageFunctionCode("9");
        ordr.setBGMBeginningOfMessage(BGM);

        //DTM message date
        segCntr++;
        DTMDateTimePeriod DTM = new DTMDateTimePeriod();
        DTM.setC507DateTimePeriod(new C507DateTimePeriod());
        DTM.getC507DateTimePeriod().setE2005DateOrTimeOrPeriodFunctionCodeQualifier("137");
        SimpleDateFormat dtmForm = new SimpleDateFormat("YYYYMMdd");
        DTM.getC507DateTimePeriod().setE2380DateOrTimeOrPeriodValue(dtmForm.format(curDate));
        DTM.getC507DateTimePeriod().setE2379DateOrTimeOrPeriodFormatCode("CCYYMMDD");
        List<DTMDateTimePeriod> dtpl = new LinkedList<>();
        dtpl.add(DTM);
        ordr.setDTMDateTimePeriod(dtpl);

        //NAD SU
        segCntr++;
        LinkedList<SegmentGroup2> SG2l = new LinkedList<>();
        SegmentGroup2 sg2 = new SegmentGroup2();
        NADNameAndAddress nad = new NADNameAndAddress();
        nad.setE3035PartyFunctionCodeQualifier("SU");
        nad.setC082PartyIdentificationDetails(new C082PartyIdentificationDetails());
        nad.getC082PartyIdentificationDetails().setE3039PartyIdentifier(vendorGLN);
        nad.getC082PartyIdentificationDetails().setE3055CodeListResponsibleAgencyCode("9");
        sg2.setNADNameAndAddress(nad);
        SG2l.add(sg2);

        //NAD BY
        segCntr++;
        SegmentGroup2 sg2_2 = new SegmentGroup2();
        NADNameAndAddress nad2 = new NADNameAndAddress();
        nad2.setE3035PartyFunctionCodeQualifier("BY");
        nad2.setC082PartyIdentificationDetails(new C082PartyIdentificationDetails());
        nad2.getC082PartyIdentificationDetails().setE3039PartyIdentifier(ConfigReader.getMyGLN());
        nad2.getC082PartyIdentificationDetails().setE3055CodeListResponsibleAgencyCode("9");
        sg2_2.setNADNameAndAddress(nad2);
        SG2l.add(sg2_2);
        ordr.setSegmentGroup2(SG2l);

        //CUX
        segCntr++;
        List<SegmentGroup7> sg7l = new LinkedList<>();
        SegmentGroup7 sg7 = new SegmentGroup7();
        CUXCurrencies cux = new CUXCurrencies();
        cux.setC5041CurrencyDetails(new C5041CurrencyDetails());
        cux.getC5041CurrencyDetails().setE6347CurrencyUsageCodeQualifier("2");
        cux.getC5041CurrencyDetails().setE6345CurrencyIdentificationCode("RUB");
        cux.getC5041CurrencyDetails().setE6343CurrencyTypeCodeQualifier("9");
        sg7.setCUXCurrencies(cux);
        sg7l.add(sg7);
        ordr.setSegmentGroup7(sg7l);

        //SG28
        LinkedList<SegmentGroup28> sg28l = new LinkedList<>();
        int itemCntr = 0;
        for (Object product : products) {
            itemCntr++;
            JSONObject prod = (JSONObject) product;
            SegmentGroup28 sg28 = new SegmentGroup28();
            sg28l.add(sg28);
            //LIN
            segCntr++;
            LINLineItem LIN = new LINLineItem();
            LIN.setE1082LineItemIdentifier(String.valueOf(itemCntr));
            LIN.setC212ItemNumberIdentification(new C212ItemNumberIdentification());
            LIN.getC212ItemNumberIdentification().setE7140ItemIdentifier((String)prod.get("productName"));
            LIN.getC212ItemNumberIdentification().setE7143ItemTypeIdentificationCode("SRV");
            
            //QTY
            segCntr++;
            QTYQuantity QTY = new QTYQuantity();
            QTY.setC186QuantityDetails(new C186QuantityDetails());
            QTY.getC186QuantityDetails().setE6063QuantityTypeCodeQualifier("21");
            QTY.getC186QuantityDetails().setE6060Quantity((String)prod.get("qty"));
            LinkedList<QTYQuantity> QTYl = new LinkedList<>();
            QTYl.add(QTY);
            
            //MOA
            segCntr++;
            List<SegmentGroup32> sg32l = new LinkedList<>();
            SegmentGroup32 sg32 = new SegmentGroup32();
            PRIPriceDetails PRI = new PRIPriceDetails();
            PRI.setC509PriceInformation(new C509PriceInformation());
            PRI.getC509PriceInformation().setE5125PriceCodeQualifier("AAB");
            PRI.getC509PriceInformation().setE5118PriceAmount(new BigDecimal((String)prod.get("listPrice")));
            sg32.setPRIPriceDetails(PRI);
            sg32l.add(sg32);
            sg28.setSegmentGroup32(sg32l);
            sg28.setQTYQuantity(QTYl);
            sg28.setLINLineItem(LIN);
            
            //UNS
            segCntr++;
            Uns UNS = new Uns();
            UNS.setE0081("S");
            ordr.setUNSSectionControl(UNS);
            
            //MOA
        }
        ordr.setSegmentGroup28(sg28l);
        
        //UNT
        segCntr++;
        orderMsg.setMessageTrailer(new UNT41());
        orderMsg.getMessageTrailer().setSegmentCount(segCntr);
        orderMsg.getMessageTrailer().setMessageRefNum(String.valueOf(msgRefNum));
        
        //UNZ
        i.setInterchangeTrailer(new UNZ41());
        i.getInterchangeTrailer().setControlCount(1);
        i.getInterchangeTrailer().setControlRef(ref.format(curDate));
        
        log.info("Mapper {} has finished to map purchase order with id {}.", this.getClass().toString(), id);
        try {
            File f = File.createTempFile("PO_"+vendorGLN+"_", ".edi");
            System.out.println(f.getPath());
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(f),"UTF-8")) {
                factory.toUNEdifact(i, writer);
            }
            log.info("Mapper {} successfully write purchase order with id {} to the temp file", this.getClass().toString(), id);
            return f;
        } catch (IOException ex) {
            throw new MappingException("Error while writing mapped result to file.", ex);
        }
        
    }
}
