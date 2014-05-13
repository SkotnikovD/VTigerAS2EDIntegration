package com.skodev.FreeAS2;

import com.skodev.exceptions.AS2Exception;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreeAS2Communicator {

    private Map<String, File> outboundMap;
    private Map<File, String> inboundMap;
    private FileAlterationMonitor fam;
    private static Logger log = LoggerFactory.getLogger(FreeAS2Communicator.class.getName());
    private static double outcomeCntr = 0;

    public FreeAS2Communicator(long checkNewMsgsIntervalMlsec, IncomingMsgsListener inboundListener) throws AS2Exception{
        try {
            outboundMap = new HashMap<>();
            inboundMap = new HashMap<>();
            this.retrieveInOutDirsFromConfig();
            
            fam = new FileAlterationMonitor(checkNewMsgsIntervalMlsec);
            for (File f : inboundMap.keySet()) {
                FileAlterationObserver fao = new FileAlterationObserver(f);
                fao.checkAndNotify();
                fao.addListener(new CreateNewFileOnlyListenerAdapter(inboundListener));
                fam.addObserver(fao);
                log.info("New inbound listener had been started on directory: {}",f);
            }
            fam.start();
        } catch (Exception ex) {
            throw new AS2Exception(ex);
        }
    }

    private void retrieveInOutDirsFromConfig() throws AS2Exception{
        try {
            URL in = getClass().getResource("/AS2Config.xml");
            SAXReader reader = new SAXReader();
            Document document = reader.read(in);
            
            Element root = document.getRootElement();
            List<Element> partnersList = root.element("partners").elements("partner");
            
            for (Element e : partnersList) {
                String GLN = e.element("GLN").getText();
                String inDir = e.element("inbound_dir").getText();
                File inD = new File(inDir);
                inD.mkdir();
                String outDir = e.element("outbound_dir").getText();
                File outD = new File(outDir);
                outD.mkdir();
                if(this.outboundMap.containsValue(outD)){
                    throw new AS2Exception(" Wron ASConfig.xml: directory "+ outD +" had already been registered for other partner.");
                }
                this.inboundMap.put(inD, GLN);
                this.outboundMap.put(GLN, outD);
                log.info("New AS2 party with GLN = {} regisered. Inbound dir: {}, outbound: {}", GLN, inDir, outDir);
            }
        } catch (DocumentException ex) {
            throw new AS2Exception(ex);
        }
    }

    public void sendMsg(String msg, String receiverGLN) throws IOException {
        for (String gln : outboundMap.keySet()) {
            if (gln.equals(receiverGLN)) {
                File outboundDir = outboundMap.get(gln);
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                String msgFileName = timeStamp +"_" + String.valueOf(outcomeCntr)+".edi";
                outcomeCntr++;
                FileUtils.writeStringToFile(new File(outboundDir, msgFileName), msg);
                return;
            }
        }
    }
}
